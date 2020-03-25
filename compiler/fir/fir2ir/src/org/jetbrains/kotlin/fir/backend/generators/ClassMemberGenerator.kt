/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

internal class ClassMemberGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope,
    fakeOverrideMode: FakeOverrideMode
) : Fir2IrComponents by components {

    private val fakeOverrideGenerator = FakeOverrideGenerator(session, declarationStorage, conversionScope, fakeOverrideMode)

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun <T : IrDeclaration> applyParentFromStackTo(declaration: T): T = conversionScope.applyParentFromStackTo(declaration)

    fun convertClassContent(irClass: IrClass, klass: FirClass<*>) {
        declarationStorage.enterScope(irClass.descriptor)
        conversionScope.withClass(irClass) {
            val primaryConstructor = klass.getPrimaryConstructorIfAny()
            val irPrimaryConstructor = primaryConstructor?.let { declarationStorage.getCachedIrConstructor(it)!! }
            if (irPrimaryConstructor != null) {
                with(declarationStorage) {
                    enterScope(irPrimaryConstructor.descriptor)
                    irPrimaryConstructor.valueParameters.forEach { symbolTable.introduceValueParameter(it) }
                    irPrimaryConstructor.putParametersInScope(primaryConstructor)
                    convertFunctionContent(irPrimaryConstructor, primaryConstructor)
                }
            }
            val processedCallableNames = mutableListOf<Name>()
            klass.declarations.forEach {
                if (it !is FirTypeAlias && (it !is FirConstructor || !it.isPrimary)) {
                    it.accept(visitor, null)
                    when (it) {
                        is FirSimpleFunction -> processedCallableNames += it.name
                        is FirProperty -> processedCallableNames += it.name
                    }
                }
            }
            with(fakeOverrideGenerator) { irClass.addFakeOverrides(klass, processedCallableNames) }
            annotations = klass.annotations.mapNotNull {
                it.accept(visitor, null) as? IrConstructorCall
            }
            if (irPrimaryConstructor != null) {
                declarationStorage.leaveScope(irPrimaryConstructor.descriptor)
            }
        }
        declarationStorage.leaveScope(irClass.descriptor)
    }

    fun <T : IrFunction> convertFunctionContent(irFunction: T, firFunction: FirFunction<*>?): T {
        val descriptor = irFunction.descriptor
        conversionScope.withParent(irFunction) {
            if (firFunction != null) {
                if (irFunction !is IrConstructor || !irFunction.isPrimary) {
                    // Scope for primary constructor should be entered before class declaration processing
                    with(declarationStorage) {
                        enterScope(descriptor)
                        irFunction.valueParameters.forEach { symbolTable.introduceValueParameter(it) }
                        irFunction.putParametersInScope(firFunction)
                    }
                }
                for ((valueParameter, firValueParameter) in valueParameters.zip(firFunction.valueParameters)) {
                    valueParameter.setDefaultValue(firValueParameter)
                }
            }
            if (firFunction is FirConstructor && irFunction is IrConstructor && !parentAsClass.isAnnotationClass) {
                val body = IrBlockBodyImpl(startOffset, endOffset)
                val delegatedConstructor = firFunction.delegatedConstructor
                if (delegatedConstructor != null) {
                    val irDelegatingConstructorCall = delegatedConstructor.toIrDelegatingConstructorCall()
                    body.statements += irDelegatingConstructorCall
                }
                if (delegatedConstructor?.isThis == false) {
                    val instanceInitializerCall = IrInstanceInitializerCallImpl(
                        startOffset, endOffset, (parent as IrClass).symbol, irFunction.constructedClassType
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
            } else if (irFunction !is IrConstructor) {
                if (irFunction.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) {
                    val kind = Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES.getValue(irFunction.name)
                    irFunction.body = IrSyntheticBodyImpl(startOffset, endOffset, kind)
                } else {
                    irFunction.body = firFunction?.body?.let { visitor.convertToIrBlockBody(it) }
                }
            }
            if (irFunction !is IrConstructor || !irFunction.isPrimary) {
                // Scope for primary constructor should be left after class declaration
                declarationStorage.leaveScope(descriptor)
            }
        }
        return irFunction
    }

    fun convertPropertyContent(irProperty: IrProperty, property: FirProperty): IrProperty {
        val descriptor = irProperty.descriptor
        val initializer = property.initializer
        val delegate = property.delegate
        val irParent = irProperty.parent
        val propertyType = property.returnTypeRef.toIrType()
        // TODO: this checks are very preliminary, FIR resolve should determine backing field presence itself
        // TODO (2): backing field should be created inside declaration storage
        if (property.modality != Modality.ABSTRACT && (irParent !is IrClass || !irParent.isInterface)) {
            if (initializer != null || property.getter is FirDefaultPropertyGetter ||
                property.isVar && property.setter is FirDefaultPropertySetter
            ) {
                irProperty.backingField = irProperty.createBackingField(
                    property, IrDeclarationOrigin.PROPERTY_BACKING_FIELD, descriptor,
                    Visibilities.PRIVATE, property.name, property.isVal, initializer, propertyType
                )
            } else if (delegate != null) {
                irProperty.backingField = irProperty.createBackingField(
                    property, IrDeclarationOrigin.PROPERTY_DELEGATE, descriptor,
                    Visibilities.PRIVATE, Name.identifier("${property.name}\$delegate"), true, delegate
                )
            }
        }
        irProperty.getter?.setPropertyAccessorContent(
            property.getter, irProperty, propertyType, property.getter is FirDefaultPropertyGetter
        )
        if (property.isVar) {
            irProperty.setter?.setPropertyAccessorContent(
                property.setter, irProperty, propertyType, property.setter is FirDefaultPropertySetter
            )
        }
        irProperty.annotations = property.annotations.mapNotNull {
            it.accept(visitor, null) as? IrConstructorCall
        }
        return irProperty
    }

    private fun IrProperty.createBackingField(
        property: FirProperty,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        visibility: Visibility,
        name: Name,
        isFinal: Boolean,
        firInitializerExpression: FirExpression?,
        type: IrType? = null
    ): IrField {
        val inferredType = type ?: firInitializerExpression!!.typeRef.toIrType()
        val irField = symbolTable.declareField(
            startOffset, endOffset, origin, descriptor, inferredType
        ) { symbol ->
            IrFieldImpl(
                startOffset, endOffset, origin, symbol,
                name, inferredType,
                visibility, isFinal = isFinal, isExternal = false,
                isStatic = property.isStatic || parent !is IrClass,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
            )
        }
        return conversionScope.withParent(applyParentFromStackTo(irField)) {
            declarationStorage.enterScope(descriptor)
            if (firInitializerExpression != null) {
                val initializerExpression = visitor.convertToIrExpression(firInitializerExpression)
                initializer = IrExpressionBodyImpl(initializerExpression)
            }
            correspondingPropertySymbol = this@createBackingField.symbol
            declarationStorage.leaveScope(descriptor)
        }
    }

    private fun IrFunction.setPropertyAccessorContent(
        propertyAccessor: FirPropertyAccessor?,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        isDefault: Boolean
    ) {
        conversionScope.withFunction(this) {
            applyParentFromStackTo(this)
            convertFunctionContent(this, propertyAccessor)
            if (isDefault) {
                conversionScope.withParent(this) {
                    declarationStorage.enterScope(descriptor)
                    val backingField = correspondingProperty.backingField
                    val fieldSymbol = symbolTable.referenceField(correspondingProperty.descriptor)
                    val declaration = this
                    if (backingField != null) {
                        body = IrBlockBodyImpl(
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
                    declarationStorage.leaveScope(descriptor)
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

    private fun FirDelegatedConstructorCall.toIrDelegatingConstructorCall(): IrExpressionBase {
        val constructedIrType = constructedTypeRef.toIrType()
        val constructorSymbol = (this.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirConstructorSymbol
            ?: return convertWithOffsets { startOffset, endOffset ->
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, constructedIrType, "Cannot find delegated constructor call"
                )
            }
        return convertWithOffsets { startOffset, endOffset ->
            val irConstructorSymbol = declarationStorage.getIrFunctionSymbol(constructorSymbol) as IrConstructorSymbol
            if (constructorSymbol.fir.isFromEnumClass || constructorSymbol.fir.returnTypeRef.isEnum) {
                IrEnumConstructorCallImpl(
                    startOffset, endOffset,
                    constructedIrType,
                    irConstructorSymbol
                ).apply {
                    val typeArguments = (constructedTypeRef as? FirResolvedTypeRef)?.type?.typeArguments
                    if (typeArguments?.isNotEmpty() == true) {
                        val irType = (typeArguments.first() as ConeKotlinTypeProjection).type.toIrType()
                        putTypeArgument(0, irType)
                    }
                }
            } else {
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset,
                    constructedIrType,
                    irConstructorSymbol
                )
            }.apply {
                for ((index, argument) in arguments.withIndex()) {
                    val argumentExpression = visitor.convertToIrExpression(argument)
                    putValueArgument(index, argumentExpression)
                }
            }
        }
    }

    private fun IrValueParameter.setDefaultValue(firValueParameter: FirValueParameter) {
        val firDefaultValue = firValueParameter.defaultValue
        if (firDefaultValue != null) {
            this.defaultValue = IrExpressionBodyImpl(visitor.convertToIrExpression(firDefaultValue))
        }
    }
}