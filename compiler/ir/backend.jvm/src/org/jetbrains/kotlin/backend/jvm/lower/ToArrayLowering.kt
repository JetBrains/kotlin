/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.DFS

internal val toArrayPhase = makeIrFilePhase(
    ::ToArrayLowering,
    name = "ToArray",
    description = "Handle toArray functions"
)

private class ToArrayLowering(private val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (irClass.isJvmInterface || !irClass.isCollectionSubClass()) return

        val irBuiltIns = context.irBuiltIns
        val symbols = context.ir.symbols

        val toArrayName = Name.identifier("toArray")
        val genericToArray = irClass.declarations.filterIsInstance<IrSimpleFunction>().find { it.isGenericToArray(context) }
        val nonGenericToArray = irClass.declarations.filterIsInstance<IrSimpleFunction>().find { it.isNonGenericToArray() }
        val isDirectCollectionSubClass = irClass.isDirectCollectionSubClass()
        if (genericToArray == null) {
            if (isDirectCollectionSubClass) {
                val typeParameterDescriptor = WrappedTypeParameterDescriptor()
                val typeParameter = IrTypeParameterImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.TO_ARRAY,
                    IrTypeParameterSymbolImpl(typeParameterDescriptor),
                    Name.identifier("T"),
                    index = 0,
                    variance = Variance.INVARIANT,
                    isReified = false
                ).apply {
                    typeParameterDescriptor.bind(this)
                    superTypes.add(irBuiltIns.anyNType)
                }

                val substitutedArrayType = irBuiltIns.arrayClass.typeWith(typeParameter.defaultType)
                val functionDescriptor = WrappedSimpleFunctionDescriptor()
                val irFunction = IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.TO_ARRAY,
                    IrSimpleFunctionSymbolImpl(functionDescriptor),
                    toArrayName,
                    Visibilities.PUBLIC,
                    Modality.OPEN,
                    returnType = substitutedArrayType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false
                )
                functionDescriptor.bind(irFunction)
                irFunction.parent = irClass

                typeParameter.parent = irFunction
                irFunction.typeParameters.add(typeParameter)

                val dispatchReceiverParameterDescriptor = WrappedValueParameterDescriptor()
                irFunction.dispatchReceiverParameter = IrValueParameterImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.TO_ARRAY,
                    IrValueParameterSymbolImpl(dispatchReceiverParameterDescriptor),
                    Name.special("<this>"),
                    index = -1,
                    type = irClass.defaultType,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false
                ).apply {
                    parent = irFunction
                }
                val valueParameterDescriptor = WrappedValueParameterDescriptor()
                irFunction.valueParameters.add(
                    IrValueParameterImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        JvmLoweredDeclarationOrigin.TO_ARRAY,
                        IrValueParameterSymbolImpl(valueParameterDescriptor),
                        Name.identifier("array"),
                        index = 0,
                        varargElementType = null,
                        type = substitutedArrayType,
                        isCrossinline = false,
                        isNoinline = false
                    ).apply {
                        valueParameterDescriptor.bind(this)
                        parent = irFunction
                    }
                )

                irFunction.body = context.createIrBuilder(irFunction.symbol).irBlockBody {
                    +irReturn(
                        irCall(symbols.genericToArray, symbols.genericToArray.owner.returnType).apply {
                            putValueArgument(
                                0,
                                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.dispatchReceiverParameter!!.symbol)
                            )
                            putValueArgument(1, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.valueParameters[0].symbol))
                        })
                }

                irClass.declarations.add(irFunction)
            }
        } else {
            (genericToArray as IrFunctionImpl).visibility = Visibilities.PUBLIC
        }

        if (nonGenericToArray == null) {
            if (isDirectCollectionSubClass) {
                val functionDescriptor = WrappedSimpleFunctionDescriptor()
                val irFunction = IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.TO_ARRAY,
                    IrSimpleFunctionSymbolImpl(functionDescriptor),
                    toArrayName,
                    Visibilities.PUBLIC,
                    Modality.OPEN,
                    returnType = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType),
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false
                )
                functionDescriptor.bind(irFunction)
                irFunction.parent = irClass

                val dispatchReceiverParameterDescriptor = WrappedValueParameterDescriptor()
                irFunction.dispatchReceiverParameter = IrValueParameterImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.TO_ARRAY,
                    IrValueParameterSymbolImpl(dispatchReceiverParameterDescriptor),
                    Name.special("<this>"),
                    index = -1,
                    type = irClass.defaultType,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false
                ).apply {
                    parent = irFunction
                }

                irFunction.body = context.createIrBuilder(irFunction.symbol).irBlockBody {
                    +irReturn(
                        irCall(symbols.nonGenericToArray, symbols.nonGenericToArray.owner.returnType).apply {
                            putValueArgument(
                                0,
                                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.dispatchReceiverParameter!!.symbol)
                            )
                        })
                }

                irClass.declarations.add(irFunction)
            }
        } else {
            (nonGenericToArray as IrFunctionImpl).visibility = Visibilities.PUBLIC
        }
    }
}

private val IrClass.superClasses
    get() = superTypes.mapNotNull { it.getClass() }

// Have to check by name, since irBuiltins is unreliable.
internal fun IrClass.isCollectionSubClass() =
    DFS.ifAny(listOf(this), IrClass::superClasses) { it.defaultType.isCollection() }

// If this class inherits from another Kotlin class that implements Collection, it already has toArray.
private fun IrClass.isDirectCollectionSubClass() =
    isCollectionSubClass() && !superClasses.any {
        it.isClass && it.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && it.isCollectionSubClass()
    }


internal fun IrSimpleFunction.isGenericToArray(context: JvmBackendContext): Boolean {
    if (name.asString() != "toArray") return false
    if (typeParameters.size != 1 || valueParameters.size != 1 || extensionReceiverParameter != null) return false

    val paramType = valueParameters[0].type

    if (!returnType.isArray() || !paramType.isArray()) return false

    val elementType = typeParameters[0].defaultType
    val expectedType = context.ir.symbols.array.typeWith(elementType)
    return expectedType == paramType && expectedType == returnType
}

internal fun IrSimpleFunction.isNonGenericToArray(): Boolean {
    if (name.asString() != "toArray") return false
    if (typeParameters.isNotEmpty() || valueParameters.isNotEmpty() || extensionReceiverParameter != null) return false
    return returnType.isArray()
}