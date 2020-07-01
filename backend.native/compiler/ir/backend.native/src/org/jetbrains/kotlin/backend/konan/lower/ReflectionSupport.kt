/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.ir.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.ir.typeWithoutArguments
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

internal class KTypeGenerator(
        private val context: KonanBackendContext,
        private val eraseTypeParameters: Boolean = false
) {
    private val symbols = context.ir.symbols

    fun IrBuilderWithScope.irKType(type: IrType): IrExpression = if (type !is IrSimpleType) {
        // Represent as non-denotable type:
        irKTypeImpl(
                kClassifier = irNull(),
                irTypeArguments = emptyList(),
                isMarkedNullable = false
        )
    } else {
        val classifier = type.classifier

        if (classifier is IrClassSymbol) {
            irKTypeImpl(
                    kClassifier = irKClass(classifier),
                    irTypeArguments = type.arguments,
                    isMarkedNullable = type.hasQuestionMark
            )
        } else {
            if (eraseTypeParameters) {
                irKType(context.irBuiltIns.anyNType)
            } else {
                irCall(symbols.kTypeImplForGenerics.constructors.single())
            }
        }
    }

    private fun IrBuilderWithScope.irKTypeImpl(
            kClassifier: IrExpression,
            irTypeArguments: List<IrTypeArgument>,
            isMarkedNullable: Boolean
    ): IrExpression = irCall(symbols.kTypeImpl.constructors.single()).apply {
        putValueArgument(0, kClassifier)
        putValueArgument(1, irKTypeProjectionsList(irTypeArguments))
        putValueArgument(2, irBoolean(isMarkedNullable))
    }

    private fun IrBuilderWithScope.irKClass(symbol: IrClassSymbol) = irKClass(this@KTypeGenerator.context, symbol)

    private fun IrBuilderWithScope.irKTypeProjectionsList(
            irTypeArguments: List<IrTypeArgument>
    ): IrMemberAccessExpression<*> {
        val kTypeProjectionType = symbols.kTypeProjection.typeWithoutArguments

        return if (irTypeArguments.isEmpty()) {
            irCall(symbols.emptyList, listOf(kTypeProjectionType))
        } else {
            irCall(symbols.listOf, listOf(kTypeProjectionType)).apply {
                putValueArgument(0, IrVarargImpl(
                        startOffset,
                        endOffset,
                        type = symbols.array.typeWith(kTypeProjectionType),
                        varargElementType = kTypeProjectionType,
                        elements = irTypeArguments.map { irKTypeProjection(it) }
                ))
            }
        }
    }

    private fun IrBuilderWithScope.irKTypeProjection(argument: IrTypeArgument): IrExpression {
        return when (argument) {
            is IrTypeProjection -> irCall(symbols.kTypeProjectionFactories.getValue(argument.variance)).apply {
                dispatchReceiver = irGetObject(symbols.kTypeProjectionCompanion)
                putValueArgument(0, irKType(argument.type))
            }

            is IrStarProjection -> irCall(symbols.kTypeProjectionStar.owner.getter!!).apply {
                dispatchReceiver = irGetObject(symbols.kTypeProjectionCompanion)
            }

            else -> error("Unexpected IrTypeArgument: $argument (${argument::class})")
        }
    }
}

internal fun IrBuilderWithScope.irKClass(context: KonanBackendContext, symbol: IrClassSymbol): IrExpression {
    val symbols = context.ir.symbols
    return when {
        symbol.descriptor.isObjCClass() ->
            irKClassUnsupported(context, "KClass for Objective-C classes is not supported yet")

        symbol.descriptor.getAllSuperClassifiers().any {
            it is ClassDescriptor && it.fqNameUnsafe == InteropFqNames.nativePointed
        } -> irKClassUnsupported(context, "KClass for interop types is not supported yet")

        else -> irCall(symbols.kClassImplConstructor.owner).apply {
            putValueArgument(0, irCall(symbols.getClassTypeInfo, listOf(symbol.typeWithStarProjections)))
        }
    }
}

private fun IrBuilderWithScope.irKClassUnsupported(context: KonanBackendContext, message: String) =
        irCall(context.ir.symbols.kClassUnsupportedImplConstructor.owner).apply {
            putValueArgument(0, irString(message))
        }
