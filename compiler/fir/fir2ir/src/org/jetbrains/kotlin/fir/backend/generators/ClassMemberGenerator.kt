/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isFromEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.DataClassResolver

internal class ClassMemberGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T = conversionScope.applyParentFromStackTo(declaration)

    fun convertClassContent(irClass: IrClass, klass: FirClass) {
        declarationStorage.enterScope(irClass)
        conversionScope.withClass(irClass) {
            val allDeclarations = buildList {
                addAll(klass.declarations)
                if (session.extensionService.declarationGenerators.isNotEmpty()) {
                    addAll(klass.generatedMembers(session))
                    addAll(klass.generatedNestedClassifiers(session))
                }
            }

            val primaryConstructor = allDeclarations.firstOrNull { it is FirConstructor && it.isPrimary } as FirConstructor?
            val irPrimaryConstructor = primaryConstructor?.let { declarationStorage.getCachedIrConstructor(it)!! }
            if (irPrimaryConstructor != null) {
                with(declarationStorage) {
                    enterScope(irPrimaryConstructor)
                    irPrimaryConstructor.putParametersInScope(primaryConstructor)
                    convertFunctionContent(irPrimaryConstructor, primaryConstructor, containingClass = klass)
                }
            }

            allDeclarations.forEach { declaration ->
                when {
                    declaration is FirTypeAlias -> {
                    }
                    declaration is FirConstructor && declaration.isPrimary -> {
                    }
                    declaration is FirRegularClass && declaration.visibility == Visibilities.Local -> {
                        val irNestedClass = classifierStorage.getCachedIrClass(declaration)!!
                        irNestedClass.parent = irClass
                        conversionScope.withParent(irNestedClass) {
                            convertClassContent(irNestedClass, declaration)
                        }
                    }
                    else -> declaration.accept(visitor, null)
                }
            }
            annotationGenerator.generate(irClass, klass)
            if (irPrimaryConstructor != null) {
                declarationStorage.leaveScope(irPrimaryConstructor)
            }
        }
        declarationStorage.leaveScope(irClass)
    }

    fun <T : IrFunction> convertFunctionContent(irFunction: T, firFunction: FirFunction?, containingClass: FirClass?): T {
        conversionScope.withParent(irFunction) {
            if (firFunction != null) {
                if (irFunction !is IrConstructor || !irFunction.isPrimary) {
                    // Scope for primary constructor should be entered before class declaration processing
                    with(declarationStorage) {
                        enterScope(irFunction)
                        irFunction.putParametersInScope(firFunction)
                    }
                }
                val irParameters = valueParameters.drop(firFunction.contextReceivers.size)
                val annotationMode = containingClass?.classKind == ClassKind.ANNOTATION_CLASS && irFunction is IrConstructor
                for ((valueParameter, firValueParameter) in irParameters.zip(firFunction.valueParameters)) {
                    valueParameter.setDefaultValue(firValueParameter, annotationMode)
                    annotationGenerator.generate(valueParameter, firValueParameter)
                }
                annotationGenerator.generate(irFunction, firFunction)
            }
            if (firFunction is FirConstructor && irFunction is IrConstructor && !firFunction.isExpect) {
                val body = factory.createBlockBody(startOffset, endOffset)
                val delegatedConstructor = firFunction.delegatedConstructor
                val irClass = parent as IrClass
                if (delegatedConstructor != null) {
                    val irDelegatingConstructorCall = conversionScope.forDelegatingConstructorCall(irFunction, irClass) {
                        delegatedConstructor.toIrDelegatingConstructorCall()
                    }
                    body.statements += irDelegatingConstructorCall
                }

                if (containingClass is FirRegularClass && containingClass.contextReceivers.isNotEmpty()) {
                    val contextReceiverFields =
                        components.classifierStorage.getFieldsWithContextReceiversForClass(irClass)
                            ?: error("Not found context receiver fields")

                    val thisParameter =
                        conversionScope.dispatchReceiverParameter(irClass) ?: error("No found this parameter for $irClass")

                    for (index in containingClass.contextReceivers.indices) {
                        val irValueParameter = valueParameters[index]
                        body.statements.add(
                            IrSetFieldImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                contextReceiverFields[index].symbol,
                                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParameter.type, thisParameter.symbol),
                                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValueParameter.type, irValueParameter.symbol),
                                components.irBuiltIns.unitType,
                            )
                        )
                    }
                }

                if (delegatedConstructor?.isThis == false) {
                    val instanceInitializerCall = IrInstanceInitializerCallImpl(
                        startOffset, endOffset, irClass.symbol, irFunction.constructedClassType
                    )
                    body.statements += instanceInitializerCall
                }

                val regularBody = firFunction.body?.let { visitor.convertToIrBlockBody(it) }
                if (regularBody != null) {
                    body.statements += regularBody.statements
                }
                if (body.statements.isNotEmpty()) {
                    irFunction.body = body
                }
            } else if (irFunction !is IrConstructor && !irFunction.isExpect) {
                when {
                    // Create fake bodies for Enum.values/Enum.valueOf
                    irFunction.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> {
                        val name = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.name ?: irFunction.name
                        val kind = Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES.getValue(name)
                        irFunction.body = IrSyntheticBodyImpl(startOffset, endOffset, kind)
                    }
                    irFunction.parent is IrClass && irFunction.parentAsClass.isData -> {
                        val lookupTag = firFunction?.symbol?.dispatchReceiverClassLookupTagOrNull()
                        when {
                            DataClassResolver.isComponentLike(irFunction.name) ->
                                firFunction?.body?.let { irFunction.body = visitor.convertToIrBlockBody(it) }
                                    ?: DataClassMembersGenerator(components).generateDataClassComponentBody(irFunction, lookupTag!!)
                            DataClassResolver.isCopy(irFunction.name) ->
                                firFunction?.body?.let { irFunction.body = visitor.convertToIrBlockBody(it) }
                                    ?: DataClassMembersGenerator(components).generateDataClassCopyBody(irFunction, lookupTag!!)
                            else ->
                                irFunction.body = firFunction?.body?.let { visitor.convertToIrBlockBody(it) }
                        }
                    }
                    else -> {
                        irFunction.body = firFunction?.body?.let { visitor.convertToIrBlockBody(it) }
                    }
                }
            }
            if (irFunction !is IrConstructor || !irFunction.isPrimary) {
                // Scope for primary constructor should be left after class declaration
                declarationStorage.leaveScope(irFunction)
            }
            if (irFunction is IrSimpleFunction && firFunction is FirSimpleFunction && containingClass != null) {
                irFunction.overriddenSymbols = firFunction.generateOverriddenFunctionSymbols(containingClass)
            }
        }
        return irFunction
    }

    fun convertPropertyContent(irProperty: IrProperty, property: FirProperty, containingClass: FirClass?): IrProperty {
        val initializer = property.backingField?.initializer ?: property.initializer
        val delegate = property.delegate
        val propertyType = property.returnTypeRef.toIrType()
        irProperty.initializeBackingField(property, initializerExpression = initializer ?: delegate)
        if (containingClass != null) {
            irProperty.overriddenSymbols = property.generateOverriddenPropertySymbols(containingClass)
        }
        irProperty.getter?.setPropertyAccessorContent(
            property, property.getter, irProperty, propertyType,
            property.getter is FirDefaultPropertyGetter,
            isGetter = true,
            containingClass = containingClass
        )
        // Create fake body for Enum.entries
        if (irProperty.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) {
            val kind = Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES.getValue(irProperty.name)
            irProperty.getter!!.body = IrSyntheticBodyImpl(irProperty.startOffset, irProperty.endOffset, kind)
        }

        if (property.isVar) {
            irProperty.setter?.setPropertyAccessorContent(
                property, property.setter, irProperty, propertyType,
                property.setter is FirDefaultPropertySetter,
                isGetter = false,
                containingClass = containingClass
            )
        }
        annotationGenerator.generate(irProperty, property)
        return irProperty
    }

    fun convertFieldContent(irField: IrField, field: FirField): IrField {
        conversionScope.withParent(irField) {
            declarationStorage.enterScope(irField)
            val initializerExpression = field.initializer
            if (irField.initializer == null && initializerExpression != null) {
                irField.initializer = irFactory.createExpressionBody(visitor.convertToIrExpression(initializerExpression))
            }
            declarationStorage.leaveScope(irField)
        }
        return irField
    }

    private fun IrProperty.initializeBackingField(
        property: FirProperty,
        initializerExpression: FirExpression?
    ) {
        val irField = backingField ?: return
        conversionScope.withParent(irField) {
            declarationStorage.enterScope(this@initializeBackingField)
            // NB: initializer can be already converted
            if (initializer == null && initializerExpression != null) {
                initializer = irFactory.createExpressionBody(
                    run {
                        val irExpression = visitor.convertToIrExpression(initializerExpression, isDelegate = property.delegate != null)
                        if (property.delegate == null) {
                            with(visitor.implicitCastInserter) {
                                irExpression.cast(initializerExpression, initializerExpression.typeRef, property.returnTypeRef)
                            }
                        } else {
                            irExpression
                        }
                    }
                )
            }
            declarationStorage.leaveScope(this@initializeBackingField)
        }
        annotationGenerator.generate(irField, property)
    }

    private fun IrSimpleFunction.setPropertyAccessorContent(
        property: FirProperty,
        propertyAccessor: FirPropertyAccessor?,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        isDefault: Boolean,
        isGetter: Boolean,
        containingClass: FirClass?
    ) {
        conversionScope.withFunction(this) {
            applyParentFromStackTo(this)
            convertFunctionContent(this, propertyAccessor, containingClass = null)
            if (isDefault) {
                conversionScope.withParent(this) {
                    declarationStorage.enterScope(this)
                    val backingField = correspondingProperty.backingField
                    val fieldSymbol = backingField?.symbol
                    val declaration = this
                    if (fieldSymbol != null) {
                        body = factory.createBlockBody(
                            startOffset, endOffset,
                            listOf(
                                if (isSetter) {
                                    IrSetFieldImpl(startOffset, endOffset, fieldSymbol, irBuiltIns.unitType).apply {
                                        setReceiver(declaration)
                                        value = IrGetValueImpl(startOffset, endOffset, propertyType, valueParameters.first().symbol)
                                    }
                                } else {
                                    IrReturnImpl(
                                        startOffset, endOffset, irBuiltIns.nothingType, symbol,
                                        IrGetFieldImpl(startOffset, endOffset, fieldSymbol, propertyType).setReceiver(declaration)
                                    )
                                }
                            )
                        )
                    }
                    declarationStorage.leaveScope(this)
                }
            }
            if (containingClass != null) {
                this.overriddenSymbols = property.generateOverriddenAccessorSymbols(containingClass, isGetter)
            }

        }
        annotationGenerator.generate(this, property)
    }

    private fun IrFieldAccessExpression.setReceiver(declaration: IrDeclaration): IrFieldAccessExpression {
        if (declaration is IrFunction) {
            val dispatchReceiver = declaration.dispatchReceiverParameter
            if (dispatchReceiver != null) {
                receiver = IrGetValueImpl(startOffset, endOffset, dispatchReceiver.symbol)
            }
        }
        return this
    }

    internal fun FirDelegatedConstructorCall.toIrDelegatingConstructorCall(): IrExpression {
        val constructedIrType = constructedTypeRef.toIrType()
        val referencedSymbol = calleeReference.toResolvedConstructorSymbol()
            ?: return convertWithOffsets { startOffset, endOffset ->
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, constructedIrType, "Cannot find delegated constructor call"
                )
            }

        // Unwrap substitution overrides from both derived class and a super class
        val constructorSymbol = referencedSymbol
            .unwrapCallRepresentative(referencedSymbol.containingClassLookupTag())
            .unwrapCallRepresentative((referencedSymbol.resolvedReturnType as? ConeClassLikeType)?.lookupTag)

        check(constructorSymbol is FirConstructorSymbol)

        val firDispatchReceiver = dispatchReceiver
        return convertWithOffsets { startOffset, endOffset ->
            val irConstructorSymbol = declarationStorage.getIrFunctionSymbol(constructorSymbol) as IrConstructorSymbol
            val typeArguments = constructedTypeRef.coneType.fullyExpandedType(session).typeArguments
            val constructor = constructorSymbol.fir
            if (constructor.isFromEnumClass || constructor.returnTypeRef.isEnum) {
                IrEnumConstructorCallImpl(
                    startOffset, endOffset,
                    constructedIrType,
                    irConstructorSymbol,
                    typeArgumentsCount = constructor.typeParameters.size,
                    valueArgumentsCount = constructor.valueParameters.size
                )
            } else {
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset,
                    irBuiltIns.unitType,
                    irConstructorSymbol,
                    typeArgumentsCount = constructor.typeParameters.size,
                    valueArgumentsCount = irConstructorSymbol.owner.valueParameters.size
                )
            }.let {
                if (constructor.typeParameters.isNotEmpty()) {
                    if (typeArguments.isNotEmpty()) {
                        for ((index, typeArgument) in typeArguments.withIndex()) {
                            if (index >= constructor.typeParameters.size) break
                            val irType = (typeArgument as ConeKotlinTypeProjection).type.toIrType()
                            it.putTypeArgument(index, irType)
                        }
                    }
                }
                if (firDispatchReceiver !is FirNoReceiverExpression) {
                    it.dispatchReceiver = visitor.convertToIrExpression(firDispatchReceiver)
                }
                with(callGenerator) {
                    declarationStorage.enterScope(irConstructorSymbol.owner)
                    val result = it.applyCallArguments(this@toIrDelegatingConstructorCall, annotationMode = false)
                    declarationStorage.leaveScope(irConstructorSymbol.owner)
                    result
                }
            }
        }
    }

    private fun IrValueParameter.setDefaultValue(firValueParameter: FirValueParameter, annotationMode: Boolean) {
        val firDefaultValue = firValueParameter.defaultValue
        if (firDefaultValue != null) {
            this.defaultValue = factory.createExpressionBody(visitor.convertToIrExpression(firDefaultValue, annotationMode))
        }
    }
}
