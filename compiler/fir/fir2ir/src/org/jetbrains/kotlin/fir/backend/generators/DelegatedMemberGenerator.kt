/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.isOverriding
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * A generator for delegated members from implementation by delegation.
 *
 * It assumes a synthetic field with the super-interface type has been created for the delegate expression. It looks for delegatable
 * methods and properties in the super-interface, and creates corresponding members in the subclass.
 * TODO: generic super interface types and generic delegated members.
 */
internal class DelegatedMemberGenerator(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    // Generate delegated members for [subClass]. The synthetic field [irField] has the super interface type.
    fun generate(irField: IrField, subClass: IrClass) {
        val superClass = (irField.type as IrSimpleTypeImpl).classOrNull?.owner ?: return
        val superClassId = superClass.classId ?: return
        superClass.declarations.filter {
            it.isDelegatable() && !subClass.declarations.any { decl -> isOverriding(irBuiltIns, decl, it) }
        }.forEach {
            if (it is IrSimpleFunction) {
                val firFunction = declarationStorage.findOverriddenFirFunction(it, superClassId) ?: return
                val function = generateDelegatedFunction(subClass, irField, it, firFunction)
                subClass.addMember(function)
            } else if (it is IrProperty) {
                val firProperty = declarationStorage.findOverriddenFirProperty(it, superClassId) ?: return
                generateDelegatedProperty(subClass, irField, it, firProperty)
            }
        }
    }

    private fun IrDeclaration.isDelegatable(): Boolean {
        return isOverridable()
                && !isFakeOverride
                && !(this is IrSimpleFunction && hasDefaultImplementation())
    }

    private fun IrDeclaration.isOverridable(): Boolean {
        return when (this) {
            is IrSimpleFunction -> this.isOverridable
            is IrProperty -> visibility != Visibilities.PRIVATE && modality != Modality.FINAL && (parent as? IrClass)?.isFinalClass != true
            else -> false
        }
    }

    private fun IrSimpleFunction.hasDefaultImplementation(): Boolean {
        var realFunction: IrSimpleFunction? = this
        while (realFunction != null && realFunction.isFakeOverride) {
            realFunction = realFunction.overriddenSymbols.firstOrNull()?.owner
        }
        return realFunction != null
                && (realFunction.modality != Modality.ABSTRACT && realFunction.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                || realFunction.annotations.hasAnnotation(FqName("kotlin.jvm.JvmDefault")))
    }

    private fun generateDelegatedFunction(
        subClass: IrClass,
        irField: IrField,
        superFunction: IrSimpleFunction,
        firSuperFunction: FirFunction<*>
    ): IrSimpleFunction {
        val startOffset = irField.startOffset
        val endOffset = irField.endOffset
        val descriptor = WrappedSimpleFunctionDescriptor()
        val origin = IrDeclarationOrigin.DELEGATED_MEMBER
        val modality = if (superFunction.modality == Modality.ABSTRACT) Modality.OPEN else superFunction.modality
        val delegateFunction = symbolTable.declareSimpleFunction(descriptor) { symbol ->
            IrFunctionImpl(
                startOffset,
                endOffset,
                origin,
                symbol,
                superFunction.name,
                superFunction.visibility,
                modality,
                superFunction.returnType,
                superFunction.isInline,
                superFunction.isExternal,
                superFunction.isTailrec,
                superFunction.isSuspend,
                superFunction.isOperator,
                superFunction.isExpect
            ).apply {
                descriptor.bind(this)
                declarationStorage.enterScope(this)
                this.parent = subClass
                overriddenSymbols = listOf(superFunction.symbol)
                dispatchReceiverParameter = declareThisReceiverParameter(symbolTable, subClass.defaultType, origin)
                // TODO: type parameters from superFunctions and type substitution when super interface types are generic
                superFunction.valueParameters.forEach { valueParameter ->
                    val parameterDescriptor = WrappedValueParameterDescriptor()
                    valueParameters += symbolTable.declareValueParameter(
                        startOffset, endOffset, origin, parameterDescriptor, valueParameter.type
                    ) { symbol ->
                        IrValueParameterImpl(
                            startOffset, endOffset, origin, symbol,
                            valueParameter.name, valueParameter.index, valueParameter.type,
                            null, valueParameter.isCrossinline, valueParameter.isNoinline
                        ).also {
                            parameterDescriptor.bind(it)
                            it.parent = this
                        }
                    }
                }

                val visibility = when (firSuperFunction) {
                    is FirSimpleFunction -> firSuperFunction.status.visibility
                    is FirPropertyAccessor -> firSuperFunction.status.visibility
                    else -> Visibilities.PUBLIC
                }
                metadata = FirMetadataSource.Function(
                    buildSimpleFunction {
                        this.origin = FirDeclarationOrigin.Synthetic
                        this.name = superFunction.name
                        this.symbol = FirNamedFunctionSymbol(getCallableId(subClass, superFunction.name))
                        this.status = FirDeclarationStatusImpl(visibility, modality)
                        this.session = components.session
                        this.returnTypeRef = firSuperFunction.returnTypeRef
                        firSuperFunction.valueParameters.map { superParameter ->
                            this.valueParameters.add(
                                buildValueParameterCopy(superParameter) {
                                    this.origin = FirDeclarationOrigin.Synthetic
                                    this.session = components.session
                                    this.symbol = FirVariableSymbol(superParameter.name)
                                }
                            )
                        }
                    }
                )

                declarationStorage.leaveScope(this)
            }
        }

        val body = IrBlockBodyImpl(startOffset, endOffset)
        val irCall = IrCallImpl(
            startOffset,
            endOffset,
            superFunction.returnType,
            superFunction.symbol,
            superFunction.typeParameters.size,
            superFunction.valueParameters.size
        ).apply {
            dispatchReceiver =
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irField.symbol,
                    irField.type,
                    IrGetValueImpl(
                        startOffset, endOffset,
                        delegateFunction.dispatchReceiverParameter?.type!!,
                        delegateFunction.dispatchReceiverParameter?.symbol!!
                    )
                )
            extensionReceiver =
                delegateFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.type, extensionReceiver.symbol)
                }
            delegateFunction.valueParameters.forEach {
                putValueArgument(it.index, IrGetValueImpl(startOffset, endOffset, it.type, it.symbol))
            }
        }
        if (superFunction.returnType.isUnit() || superFunction.returnType.isNothing()) {
            body.statements.add(irCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, delegateFunction.symbol, irCall)
            body.statements.add(irReturn)
        }
        delegateFunction.body = body
        return delegateFunction
    }

    private fun generateDelegatedProperty(
        subClass: IrClass,
        irField: IrField,
        superProperty: IrProperty,
        firSuperProperty: FirProperty
    ) {
        val startOffset = irField.startOffset
        val endOffset = irField.endOffset
        val descriptor = WrappedPropertyDescriptor()
        val modality = if (superProperty.modality == Modality.ABSTRACT) Modality.OPEN else superProperty.modality
        symbolTable.declareProperty(
            startOffset, endOffset,
            IrDeclarationOrigin.DELEGATED_MEMBER, descriptor, superProperty.isDelegated
        ) { symbol ->
            IrPropertyImpl(
                startOffset, endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, symbol,
                superProperty.name, superProperty.visibility,
                modality,
                isVar = superProperty.isVar,
                isConst = superProperty.isConst,
                isLateinit = superProperty.isLateinit,
                isDelegated = superProperty.isDelegated,
                isExternal = false,
                isExpect = superProperty.isExpect,
                isFakeOverride = false
            ).apply {
                descriptor.bind(this)
                this.parent = subClass
                getter = generateDelegatedFunction(subClass, irField, superProperty.getter!!, firSuperProperty.getter!!).apply {
                    this.correspondingPropertySymbol = symbol
                }
                if (superProperty.isVar) {
                    setter = generateDelegatedFunction(subClass, irField, superProperty.setter!!, firSuperProperty.setter!!).apply {
                        this.correspondingPropertySymbol = symbol
                    }
                }
                this.metadata = FirMetadataSource.Property(
                    buildProperty {
                        this.name = superProperty.name
                        this.origin = FirDeclarationOrigin.Synthetic
                        this.session = components.session
                        this.status = FirDeclarationStatusImpl(firSuperProperty.status.visibility, modality)
                        this.isLocal = firSuperProperty.isLocal
                        this.returnTypeRef = firSuperProperty.returnTypeRef
                        this.symbol = FirPropertySymbol(getCallableId(subClass, superProperty.name))
                        this.isVar = firSuperProperty.isVar
                    }
                )
                subClass.addMember(this)
            }
        }
    }

    private fun getCallableId(irClass: IrClass, name: Name): CallableId {
        val classId = irClass.classId
        return if (classId != null) CallableId(classId, name) else CallableId(name)
    }
}
