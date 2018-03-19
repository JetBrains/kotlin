/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class KClassJavaObjectTypeProperty : IntrinsicMethod() {
    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        return object: IrIntrinsicFunction(expression, signature, context) {
            override fun invoke(v: InstructionAdapter, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, data: BlockInfo): StackValue {
                val extensionReceiver = expression.extensionReceiver
                if (extensionReceiver is IrGetClass) {
                    val extensionReceiverType = codegen.gen(extensionReceiver.argument, data).type
                    when {
                        extensionReceiverType == Type.VOID_TYPE -> {
                            StackValue.unit().put(AsmTypes.UNIT_TYPE, v)
                            v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                        }
                        AsmUtil.isPrimitive(extensionReceiverType) -> {
                            AsmUtil.pop(v, extensionReceiverType)
                            v.aconst(AsmUtil.boxType(extensionReceiverType))
                        }
                        else -> v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                    }
                }
                else {
                    assert(extensionReceiver is IrClassReference)
                    val classType = (extensionReceiver as IrClassReference).classType
                    var lhsType = codegen.typeMapper.mapType(classType)
                    if (AsmUtil.isPrimitive(lhsType)) {
                        lhsType = AsmUtil.boxType(lhsType)
                    } else {
                        if (TypeUtils.isTypeParameter(classType)) {
                            assert(TypeUtils.isReifiedTypeParameter(classType)) { "Non-reified type parameter under ::class should be rejected by type checker: " + classType }
                            ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameter(
                                classType,
                                ReifiedTypeInliner.OperationKind.JAVA_CLASS,
                                v,
                                codegen
                            )
                        }
                    }
                    v.aconst(lhsType)
                }

                return StackValue.onStack(signature.returnType)
            }
        }
    }
}
