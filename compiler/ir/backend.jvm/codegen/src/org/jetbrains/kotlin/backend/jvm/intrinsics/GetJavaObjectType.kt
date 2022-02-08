/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.codegen.materialize
import org.jetbrains.kotlin.backend.jvm.mapping.mapTypeAsDeclaration
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

object GetJavaObjectType : IntrinsicMethod() {

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? =
        when (val receiver = expression.extensionReceiver) {
            is IrClassReference -> {
                val symbol = receiver.symbol
                if (symbol is IrTypeParameterSymbol) {
                    val success = codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter(
                        receiver.classType,
                        ReifiedTypeInliner.OperationKind.JAVA_CLASS
                    )
                    assert(success) {
                        "Non-reified type parameter under ::class should be rejected by type checker: ${receiver.render()}"
                    }
                }
                codegen.mv.aconst(AsmUtil.boxType(codegen.typeMapper.mapTypeAsDeclaration(receiver.classType)))

                with(codegen) { expression.onStack }
            }

            is IrGetClass -> {
                val argumentValue = receiver.argument.accept(codegen, data)
                argumentValue.materialize()
                val argumentType = argumentValue.type
                when {
                    argumentType == Type.VOID_TYPE ->
                        codegen.mv.aconst(AsmTypes.UNIT_TYPE)

                    AsmUtil.isPrimitive(argumentType) ||
                            AsmUtil.unboxPrimitiveTypeOrNull(argumentType) != null -> {
                        AsmUtil.pop(codegen.mv, argumentType)
                        codegen.mv.aconst(AsmUtil.boxType(argumentType))
                    }

                    else ->
                        codegen.mv.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                }

                with(codegen) { expression.onStack }
            }

            else ->
                null
        }
}