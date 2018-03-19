/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class KClassJavaPrimitiveTypeProperty : IntrinsicMethod() {
    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        return object: IrIntrinsicFunction(expression, signature, context) {
            override fun invoke(v: InstructionAdapter, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, data: BlockInfo): StackValue {
                val extensionReceiver = expression.extensionReceiver
                if (extensionReceiver is IrGetClass) {
                    val extensionReceiverType = codegen.gen(extensionReceiver.argument, data).type
                    when {
                        extensionReceiverType == Type.VOID_TYPE -> {
                            v.aconst(null)
                        }
                        AsmUtil.isPrimitive(extensionReceiverType) -> {
                            AsmUtil.pop(v, extensionReceiverType)
                            v.getstatic(AsmUtil.boxType(extensionReceiverType).getInternalName(), "TYPE", "Ljava/lang/Class;");
                        }
                        else -> {
                            AsmUtil.pop(v, extensionReceiverType)
                            if (AsmUtil.unboxPrimitiveTypeOrNull(extensionReceiverType) != null) {
                                v.getstatic(extensionReceiverType.getInternalName(), "TYPE", "Ljava/lang/Class;");
                            } else {
                                v.aconst(null)
                            }
                        }
                    }
                } else if (extensionReceiver is IrClassReference) {
                    assert(extensionReceiver is IrClassReference)
                    val classType = (extensionReceiver as IrClassReference).classType
                    if (TypeUtils.isTypeParameter(classType)) {
                        // Reified mechanism is not yet able to generate code that should generate a null value or a field access to TYPE
                        // based on a concrete type. To support it, it requires a new kind into ReifiedTypeInliner.OperationKind and add
                        // the corresponding rewriting support, thus keep the old mechanism for the moment.
                        codegen.gen(extensionReceiver!!, data)
                        val mapToCallableMethod =
                            context.state.typeMapper.mapToCallableMethod(expression.descriptor as FunctionDescriptor, false)
                        mapToCallableMethod.genInvokeInstruction(codegen.mv)
                    } else {
                        var lhsType = codegen.typeMapper.mapType(classType)
                        if (AsmUtil.isPrimitive(lhsType)
                            || AsmUtil.unboxPrimitiveTypeOrNull(lhsType) != null
                            || AsmTypes.VOID_TYPE.equals(lhsType)) {
                            if (AsmUtil.isPrimitive(lhsType)) {
                                lhsType = AsmUtil.boxType(lhsType)
                            }
                            v.getstatic(lhsType.getInternalName(), "TYPE", "Ljava/lang/Class;");
                        } else {
                            v.aconst(null)

                        }
                    }
                } else {
                    codegen.gen(extensionReceiver!!, data)
                    val mapToCallableMethod =
                        context.state.typeMapper.mapToCallableMethod(expression.descriptor as FunctionDescriptor, false)
                    mapToCallableMethod.genInvokeInstruction(codegen.mv)
                }

                return StackValue.onStack(signature.returnType)
            }
        }
    }
}
