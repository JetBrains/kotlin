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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.DFS

internal val toArrayPhase = makeIrFilePhase(
    ::ToArrayLowering,
    name = "ToArray",
    description = "Handle toArray functions"
)

private class ToArrayLowering(private val context: JvmBackendContext) : ClassLoweringPass {
    private val symbols = context.ir.symbols

    override fun lower(irClass: IrClass) {
        if (irClass.isJvmInterface || !irClass.isCollectionSubClass) return

        val indirectCollectionSubClass = generateSequence(irClass.superClass, IrClass::superClass).firstOrNull {
            it.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        }?.isCollectionSubClass == true

        irClass.findOrCreate(indirectCollectionSubClass, { it.isGenericToArray(context) }) {
            irClass.addFunction {
                name = Name.identifier("toArray")
                origin = JvmLoweredDeclarationOrigin.TO_ARRAY
                modality = Modality.OPEN
            }.apply {
                val elementType = addTypeParameter {
                    name = Name.identifier("T")
                    origin = JvmLoweredDeclarationOrigin.TO_ARRAY
                    superTypes.add(context.irBuiltIns.anyNType)
                }
                returnType = context.irBuiltIns.arrayClass.typeWith(elementType.defaultType)
                val receiver = addDispatchReceiver {
                    type = irClass.defaultType
                    origin = JvmLoweredDeclarationOrigin.TO_ARRAY
                }
                val prototype = addValueParameter("array", returnType, JvmLoweredDeclarationOrigin.TO_ARRAY)
                body = context.createIrBuilder(symbol).irBlockBody {
                    +irReturn(irCall(symbols.genericToArray, symbols.genericToArray.owner.returnType).apply {
                        putValueArgument(0, irGet(receiver))
                        putValueArgument(1, irGet(prototype))
                    })
                }
            }
        }

        irClass.findOrCreate(indirectCollectionSubClass, { it.isNonGenericToArray(context) }) {
            irClass.addFunction {
                name = Name.identifier("toArray")
                origin = JvmLoweredDeclarationOrigin.TO_ARRAY
                modality = Modality.OPEN
                returnType = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.anyNType)
            }.apply {
                val receiver = addDispatchReceiver {
                    type = irClass.defaultType
                    origin = JvmLoweredDeclarationOrigin.TO_ARRAY
                }
                body = context.createIrBuilder(symbol).irBlockBody {
                    +irReturn(irCall(symbols.nonGenericToArray, symbols.nonGenericToArray.owner.returnType).apply {
                        putValueArgument(0, irGet(receiver))
                    })
                }
            }
        }
    }

    private fun IrClass.findOrCreate(indirectSubclass: Boolean, matcher: (IrSimpleFunction) -> Boolean, fallback: () -> IrSimpleFunction) {
        val existing = functions.find(matcher) as? IrFunctionImpl
        if (existing != null) {
            // This is an explicit override of a method defined in `kotlin.collections.AbstractCollection`
            // or `java.util.Collection`. From here on, the frontend will check the existence of implementations;
            // we just need to match visibility in the former case to the latter.
            existing.visibility = Visibilities.PUBLIC
            return
        }
        if (indirectSubclass) {
            // There's a Kotlin class up the hierarchy that should already have `toArray`.
            return
        }
        fallback()
    }
}

private val IrClass.superClass: IrClass?
    get() = superTypes.mapNotNull { it.getClass()?.takeIf { superClass -> superClass.isClass } }.singleOrNull()

internal val IrClass.isCollectionSubClass: Boolean
    get() = DFS.ifAny(superTypes, { it.getClass()?.superTypes ?: listOf() }) { it.isCollection() }

private fun IrType.isArrayOrNullableArrayOf(context: JvmBackendContext, element: IrClassifierSymbol): Boolean =
    this is IrSimpleType && (isArray() || isNullableArray()) && arguments.size == 1 && element == when (val it = arguments[0]) {
        is IrStarProjection -> context.irBuiltIns.anyClass
        is IrTypeProjection -> if (it.variance == Variance.IN_VARIANCE) context.irBuiltIns.anyClass else it.type.classifierOrNull
        else -> null
    }

// Match `fun <T> toArray(prototype: Array<T>): Array<T>`
internal fun IrSimpleFunction.isGenericToArray(context: JvmBackendContext): Boolean =
    name.asString() == "toArray" && typeParameters.size == 1 && valueParameters.size == 1 &&
            extensionReceiverParameter == null &&
            returnType.isArrayOrNullableArrayOf(context, typeParameters[0].symbol) &&
            valueParameters[0].type.isArrayOrNullableArrayOf(context, typeParameters[0].symbol)

// Match `fun toArray(): Array<Any?>`
internal fun IrSimpleFunction.isNonGenericToArray(context: JvmBackendContext): Boolean =
    name.asString() == "toArray" && typeParameters.isEmpty() && valueParameters.isEmpty() &&
            extensionReceiverParameter == null && returnType.isArrayOrNullableArrayOf(context, context.irBuiltIns.anyClass)
