/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.utils.unwrapCallRepresentative
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.comparators.FirCallableDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.DataClassResolver

internal class ClassMemberGenerator(
    private val c: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by c {
    private fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T = conversionScope.applyParentFromStackTo(declaration)

    fun convertClassContent(irClass: IrClass, klass: FirClass): Unit = conversionScope.withContainingFirClass(klass) {
        declarationStorage.enterScope(irClass.symbol)
        conversionScope.withClass(irClass) {
            val allDeclarations = buildList {
                addAll(klass.declarations)
                if (klass is FirRegularClass && session.extensionService.declarationGenerators.isNotEmpty()) {
                    addAll(klass.generatedMembers(session).sortedWith(FirCallableDeclarationComparator))
                    addAll(klass.generatedNestedClassifiers(session).sortedWith(FirMemberDeclarationComparator))
                }
            }

            val primaryConstructor = allDeclarations.firstOrNull { it is FirConstructor && it.isPrimary } as FirConstructor?

            @OptIn(UnsafeDuringIrConstructionAPI::class)
            val irPrimaryConstructor = primaryConstructor?.let { declarationStorage.getCachedIrConstructorSymbol(it)!!.owner }
            if (irPrimaryConstructor != null) {
                with(declarationStorage) {
                    enterScope(irPrimaryConstructor.symbol)
                    irPrimaryConstructor.putParametersInScope(primaryConstructor)
                    convertFunctionContent(irPrimaryConstructor, primaryConstructor, containingClass = klass)
                }
            }

            allDeclarations.forEach { declaration ->
                when (declaration) {
                    is FirTypeAlias -> {}
                    is FirConstructor if declaration.isPrimary -> {}
                    is FirRegularClass if declaration.visibility == Visibilities.Local -> {
                        val irNestedClass = classifierStorage.getIrClass(declaration)
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
                declarationStorage.leaveScope(irPrimaryConstructor.symbol)
            }
        }
        declarationStorage.leaveScope(irClass.symbol)
    }

    fun <T : IrFunction> convertFunctionContent(irFunction: T, firFunction: FirFunction?, containingClass: FirClass?): T {
        conversionScope.withParent(irFunction) {
            if (firFunction != null) {
                if (irFunction !is IrConstructor || !irFunction.isPrimary) {
                    // Scope for primary constructor should be entered before class declaration processing
                    with(declarationStorage) {
                        enterScope(irFunction.symbol)
                        irFunction.putParametersInScope(firFunction)
                    }
                }
                val irParameters = parameters.filter { it.kind == IrParameterKind.Regular }
                val annotationMode = containingClass?.classKind == ClassKind.ANNOTATION_CLASS && irFunction is IrConstructor
                for ((valueParameter, firValueParameter) in irParameters.zip(firFunction.valueParameters)) {
                    visitor.withAnnotationMode(enableAnnotationMode = annotationMode) {
                        valueParameter.setDefaultValue(firValueParameter)
                    }
                }
                annotationGenerator.generate(irFunction, firFunction)
            }
            if (firFunction is FirConstructor && irFunction is IrConstructor && !firFunction.isExpect && !irFunction.isExternal) {
                if (!configuration.skipBodies) {
                    val body = factory.createBlockBody(startOffset, endOffset)
                    val delegatedConstructor = firFunction.delegatedConstructor
                    val irClass = parent as IrClass
                    if (delegatedConstructor != null) {
                        val irDelegatingConstructorCall = conversionScope.forDelegatingConstructorCall(irFunction, irClass) {
                            delegatedConstructor.toIrDelegatingConstructorCall()
                        }
                        body.statements += irDelegatingConstructorCall
                    }

                    // TODO(KT-72994) remove when context receivers are removed
                    if (containingClass is FirRegularClass && containingClass.contextParameters.isNotEmpty()) {
                        val contextReceiverFields =
                            c.classifierStorage.getFieldsWithContextReceiversForClass(irClass, containingClass)

                        val thisParameter =
                            conversionScope.dispatchReceiverParameter(irClass) ?: error("No found this parameter for $irClass")

                        val irContextParameters = parameters.filter { it.kind == IrParameterKind.Context }

                        for (index in containingClass.contextParameters.indices) {
                            require(contextReceiverFields.size > index) {
                                "Not defined context receiver #${index} for $irClass. " +
                                        "Context receivers found: $contextReceiverFields"
                            }
                            val irValueParameter = irContextParameters[index]
                            body.statements.add(
                                IrSetFieldImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    contextReceiverFields[index].symbol,
                                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParameter.type, thisParameter.symbol),
                                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValueParameter.type, irValueParameter.symbol),
                                    c.builtins.unitType,
                                )
                            )
                        }
                    }

                    if (delegatedConstructor?.isThis == false) {
                        val instanceInitializerCall = IrInstanceInitializerCallImpl(
                            startOffset, endOffset, irClass.symbol, builtins.unitType
                        )
                        body.statements += instanceInitializerCall
                    }

                    val regularBody = firFunction.body?.let { visitor.convertToIrBlockBody(it) }
                    if (regularBody != null) {
                        body.statements += regularBody.statements
                    }
                    // Constructor of `Any` is a special case because
                    // constructors of other classes have at least a delegation call to a super constructor
                    if (body.statements.isNotEmpty() || containingClass?.classId == StandardClassIds.Any) {
                        irFunction.body = body
                    }
                }
            } else if (irFunction !is IrConstructor && !irFunction.isExpect) {
                when {
                    // Create fake bodies for Enum.values/Enum.valueOf
                    irFunction.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> {
                        val kind = Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES.getValue(irFunction.name)
                        irFunction.body = IrSyntheticBodyImpl(startOffset, endOffset, kind)
                    }
                    irFunction.parent is IrClass && irFunction.parentAsClass.isData -> {
                        require(irFunction is IrSimpleFunction)
                        when {
                            DataClassResolver.isComponentLike(irFunction.name) -> when (firFunction?.body) {
                                null -> dataClassMembersGenerator.registerCopyOrComponentFunction(irFunction)
                                else -> irFunction.body = convertBody(firFunction)
                            }
                            DataClassResolver.isCopy(irFunction.name) -> when (firFunction?.body) {
                                null -> dataClassMembersGenerator.registerCopyOrComponentFunction(irFunction)
                                else -> irFunction.body = convertBody(firFunction)
                            }
                            else -> irFunction.body = convertBody(firFunction)
                        }
                    }
                    else -> {
                        irFunction.body = convertBody(firFunction)
                    }
                }
            }
            if (firFunction != null && (irFunction !is IrConstructor || !irFunction.isPrimary)) {
                // Scope for primary constructor should be left after class declaration
                declarationStorage.leaveScope(irFunction.symbol)
            }
        }
        return irFunction
    }

    private fun IrFunction.convertBody(firFunction: FirFunction?): IrBlockBody? {
        val firBody = firFunction?.body
        return when {
            firBody == null -> null
            configuration.skipBodies -> factory.createBlockBody(startOffset, endOffset).also { body ->
                val expression =
                    IrErrorExpressionImpl(startOffset, endOffset, builtins.nothingType, SKIP_BODIES_ERROR_DESCRIPTION)
                body.statements.add(
                    IrReturnImpl(startOffset, endOffset, builtins.nothingType, symbol, expression)
                )
            }
            else -> visitor.convertToIrBlockBody(firBody)
        }
    }

    fun convertPropertyContent(irProperty: IrProperty, property: FirProperty): IrProperty {
        val initializer = property.backingField?.initializer ?: property.initializer
        val delegate = property.delegate
        val propertyType = property.returnTypeRef.toIrType(c)
        irProperty.initializeBackingField(property, initializerExpression = initializer ?: delegate)
        val needGenerateDefaultGetter = property.getter is FirDefaultPropertyGetter ||
                (property.getter == null && irProperty.parent is IrScript && property.destructuringDeclarationContainerVariable != null)

        irProperty.getter?.setPropertyAccessorContent(
            property.getter, irProperty, propertyType, isDefault = needGenerateDefaultGetter
        )
        // Create fake body for Enum.entries
        if (irProperty.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) {
            val kind = Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES.getValue(irProperty.name)
            irProperty.getter!!.body = IrSyntheticBodyImpl(irProperty.startOffset, irProperty.endOffset, kind)
        }

        if (property.isVar) {
            irProperty.setter?.setPropertyAccessorContent(
                property.setter, irProperty, propertyType, property.setter is FirDefaultPropertySetter
            )
        }
        annotationGenerator.generate(irProperty, property)
        return irProperty
    }

    fun convertFieldContent(irField: IrField, field: FirField): IrField {
        conversionScope.withParent(irField) {
            declarationStorage.enterScope(irField.symbol)
            val initializerExpression = field.initializer
            if (irField.initializer == null && initializerExpression != null && !configuration.skipBodies) {
                irField.initializer = IrFactoryImpl.createExpressionBody(visitor.convertToIrExpression(initializerExpression))
            }
            declarationStorage.leaveScope(irField.symbol)
        }
        return irField
    }

    private fun IrProperty.initializeBackingField(
        property: FirProperty,
        initializerExpression: FirExpression?
    ) {
        val irField = backingField ?: return
        val isAnnotationParameter = (irField.parent as? IrClass)?.kind == ClassKind.ANNOTATION_CLASS
        if (!configuration.skipBodies || isAnnotationParameter || property.isConst) {
            conversionScope.withParent(irField) {
                declarationStorage.enterScope(this@initializeBackingField.symbol)
                // NB: initializer can be already converted
                if (initializer == null && initializerExpression != null) {
                    initializer = IrFactoryImpl.createExpressionBody(
                        run {
                            val irExpression = visitor.convertToIrExpression(initializerExpression, isDelegate = property.delegate != null)
                            if (property.delegate == null) {
                                with(visitor.implicitCastInserter) {
                                    irExpression.insertSpecialCast(
                                        initializerExpression,
                                        initializerExpression.resolvedType,
                                        property.returnTypeRef.coneType
                                    )
                                }
                            } else {
                                irExpression
                            }
                        }
                    )
                }
                declarationStorage.leaveScope(this@initializeBackingField.symbol)
            }
        }
        property.backingField?.let { annotationGenerator.generate(irField, it) }
    }

    private fun IrSimpleFunction.setPropertyAccessorContent(
        propertyAccessor: FirPropertyAccessor?,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        isDefault: Boolean
    ) {
        conversionScope.withFunction(this) {
            applyParentFromStackTo(this)
            convertFunctionContent(this, propertyAccessor, containingClass = null)
            if (isDefault) {
                conversionScope.withParent(this) {
                    declarationStorage.enterScope(this.symbol)
                    val backingField = correspondingProperty.backingField
                    val fieldSymbol = backingField?.symbol
                    val declaration = this
                    if (fieldSymbol != null && !configuration.skipBodies) {
                        body = factory.createBlockBody(
                            startOffset, endOffset,
                            listOf(
                                if (isSetter) {
                                    IrSetFieldImpl(startOffset, endOffset, fieldSymbol, builtins.unitType).apply {
                                        setReceiver(declaration)
                                        value = IrGetValueImpl(
                                            startOffset,
                                            endOffset,
                                            propertyType,
                                            parameters.first { it.kind == IrParameterKind.Regular }.symbol
                                        )
                                    }
                                } else {
                                    IrReturnImpl(
                                        startOffset, endOffset, builtins.nothingType, symbol,
                                        IrGetFieldImpl(startOffset, endOffset, fieldSymbol, propertyType).setReceiver(declaration)
                                    )
                                }
                            )
                        )
                    }
                    declarationStorage.leaveScope(this.symbol)
                }
            }
        }
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
        val constructedIrType = constructedTypeRef.toIrType(c)
        val referencedSymbol = calleeReference.toResolvedConstructorSymbol()
            ?: return convertWithOffsets { startOffset, endOffset ->
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, constructedIrType, "Cannot find delegated constructor call"
                )
            }

        // Unwrap substitution overrides from both derived class and a superclass
        val constructorSymbol = referencedSymbol
            .unwrapCallRepresentative(c, referencedSymbol.containingClassLookupTag())
            .unwrapCallRepresentative(c, referencedSymbol.resolvedReturnType.classLikeLookupTagIfAny)

        check(constructorSymbol is FirConstructorSymbol)

        return convertWithOffsets { startOffset, endOffset ->
            val irConstructorSymbol = declarationStorage.getIrFunctionSymbol(constructorSymbol) as IrConstructorSymbol
            val typeArguments = constructedTypeRef.coneType.fullyExpandedType(session).typeArguments
            val constructor = constructorSymbol.fir
            /*
             * We should generate enum constructor call only if it is used to create new enum entry (so it's a super constructor call)
             * If it is this constructor call that we are facing secondary constructor of enum, and should generate
             *   regular delegating constructor call
             *
             * enum class Some(val x: Int) {
             *   A(); // <---- super call, IrEnumConstructorCall
             *
             *   constructor() : this(10) // <---- this call, IrDelegatingConstructorCall
             * }
             */
            @OptIn(UnexpandedTypeCheck::class)
            if ((constructor.isFromEnumClass || constructor.returnTypeRef.isEnum) && this.isSuper) {
                IrEnumConstructorCallImplWithShape(
                    startOffset, endOffset,
                    constructedIrType,
                    irConstructorSymbol,
                    typeArgumentsCount = constructor.typeParameters.size,
                    valueArgumentsCount = constructor.valueParameters.size,
                    contextParameterCount = constructor.contextParameters.size,
                    hasDispatchReceiver = constructor.dispatchReceiverType != null,
                    hasExtensionReceiver = constructor.isExtension,
                )
            } else {
                IrDelegatingConstructorCallImplWithShape(
                    startOffset, endOffset,
                    builtins.unitType,
                    irConstructorSymbol,
                    typeArgumentsCount = constructor.typeParameters.size,
                    valueArgumentsCount = constructor.valueParameters.size + constructor.contextParameters.size,
                    contextParameterCount = constructor.contextParameters.size,
                    hasDispatchReceiver = constructor.dispatchReceiverType != null,
                    hasExtensionReceiver = constructor.isExtension,
                )
            }.let {
                if (constructor.typeParameters.isNotEmpty()) {
                    if (typeArguments.isNotEmpty()) {
                        for ((index, typeArgument) in typeArguments.withIndex()) {
                            if (index >= constructor.typeParameters.size) break
                            val irType = (typeArgument as ConeKotlinTypeProjection).type.toIrType(c)
                            it.typeArguments[index] = irType
                        }
                    }
                }
                with(callGenerator) {
                    declarationStorage.enterScope(irConstructorSymbol)
                    val result = it.applyReceiversAndArguments(this@toIrDelegatingConstructorCall, constructorSymbol, null)
                    declarationStorage.leaveScope(irConstructorSymbol)
                    result
                }
            }
        }
    }

    private fun IrValueParameter.setDefaultValue(firValueParameter: FirValueParameter) {
        if (session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation) &&
            session.moduleData.isCommon &&
            ((conversionScope.parent() as? IrConstructor)?.parent as? IrClass)?.classId == StandardClassIds.Enum
        ) {
            return // TODO: Remove when KT-67381 is implemented
        }

        val firDefaultValue = firValueParameter.defaultValue
        if (firDefaultValue != null) {
            this.defaultValue = when {
                configuration.skipBodies && parent.isDataClassCopy ->
                    // Replicate K1 behavior, which removes default values of data class copy parameters in skipBodies (kapt) mode.
                    null
                configuration.skipBodies && !parent.isAnnotationConstructor ->
                    factory.createExpressionBody(
                        IrErrorExpressionImpl(startOffset, endOffset, builtins.nothingType, SKIP_BODIES_ERROR_DESCRIPTION)
                    )
                else ->
                    factory.createExpressionBody(
                        visitor.convertToIrExpression(firDefaultValue)
                    )
            }
        }
    }

    private val IrElement.isAnnotationConstructor: Boolean
        get() = this is IrConstructor && parentAsClass.isAnnotationClass

    private val IrElement.isDataClassCopy: Boolean
        get() = this is IrSimpleFunction && DataClassResolver.isCopy(name) && parent.let { it is IrClass && it.isData }
}
