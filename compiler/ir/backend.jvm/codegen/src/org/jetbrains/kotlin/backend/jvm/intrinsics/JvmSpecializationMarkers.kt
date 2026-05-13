/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.mapping.asSpecTypeParameterUsage
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.org.objectweb.asm.Type

object JvmBoxMarker : IntrinsicMethod() {
    override fun invoke(
        expression: IrFunctionAccessExpression,
        codegen: ExpressionCodegen,
        data: BlockInfo,
    ): PromisedValue {
        val type = expression.typeArguments[0]!!
        val typeUsage = type.asSpecTypeParameterUsage()!!

        val value = expression.arguments[0]!!.accept(codegen, data)

        return object : PromisedValue(codegen, value.type, value.irType) {
            override fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
                value.materializeAt(target, irTarget, castForReified)
                codegen.mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics",
                    "boxMarker${typeUsage.encode()}",
                    "(${target})Lkotlin/jvm/internal/SpecBoxedDecoy${typeUsage.encode()};",
                    false,
                )
            }

            override fun discard() {
                value.discard()
            }
        }
    }
}

object JvmUnboxMarker : IntrinsicMethod() {
    override fun invoke(
        expression: IrFunctionAccessExpression,
        codegen: ExpressionCodegen,
        data: BlockInfo,
    ): PromisedValue {
        val type = expression.typeArguments[0]!!
        val typeUsage = type.asSpecTypeParameterUsage()!!

        val value = expression.arguments[0]!!.accept(codegen, data)

        return object : PromisedValue(codegen, value.type, value.irType) {
            override fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
                value.materializeAt(target, irTarget, castForReified)
                codegen.mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics",
                    "unboxMarker${typeUsage.encode()}",
                    "(${target})Lkotlin/jvm/internal/SpecUnboxedDecoy${typeUsage.encode()};",
                    false,
                )
            }

            override fun discard() {
                value.discard()
            }
        }
    }
}

object JvmSpecializedArgumentMarker : IntrinsicMethod() {
    override fun invoke(
        expression: IrFunctionAccessExpression,
        codegen: ExpressionCodegen,
        data: BlockInfo,
    ): PromisedValue {
        val value = expression.arguments[0]!!.accept(codegen, data)

        return object : PromisedValue(codegen, value.type, value.irType) {
            override fun materializeAt(target: Type, irTarget: IrType, castForReified: Boolean) {
                value.materializeAt(target, irTarget, castForReified)
                codegen.mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics",
                    "specializedArgumentMarker",
                    "($target)$target",
                    false,
                )
            }

            override fun discard() {
                value.discard()
            }
        }
    }
}
