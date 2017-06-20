/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.genInvokeAppendMethod
import org.jetbrains.kotlin.codegen.AsmUtil.genStringBuilderConstructor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class Concat : IntrinsicMethod() {
    fun generateImpl(
            codegen: ExpressionCodegen,
            v: InstructionAdapter,
            returnType: Type,
            element: PsiElement?,
            arguments: List<KtExpression>,
            receiver: StackValue
    ): Type {
        if (element is KtBinaryExpression && element.operationReference.getReferencedNameElementType() == KtTokens.PLUS) {
            // LHS + RHS
            genStringBuilderConstructor(v)
            codegen.invokeAppend(v, element.left)
            codegen.invokeAppend(v, element.right)
        }
        else {
            // LHS?.plus(RHS)
            receiver.put(AsmTypes.OBJECT_TYPE, v)
            genStringBuilderConstructor(v)
            v.swap()
            genInvokeAppendMethod(v, returnType)
            codegen.invokeAppend(v, arguments.get(0))
        }

        v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        return JAVA_STRING_TYPE
    }

    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        val argsTypes = expression.receiverAndArgs().asmTypes(context).toMutableList()
        argsTypes[0] = AsmTypes.JAVA_STRING_TYPE

        return object : IrIntrinsicFunction(expression, signature, context, argsTypes) {

            override fun genInvokeInstruction(v: InstructionAdapter) {
                AsmUtil.genInvokeAppendMethod(v, argsTypes[1])
                v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
            }

            override fun invoke(v: InstructionAdapter, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, data: BlockInfo): StackValue {
                v.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
                v.dup()

                return super.invoke(v, codegen, data)
            }


            override fun genArg(expression: IrExpression, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, index: Int, data: BlockInfo) {
                super.genArg(expression, codegen, index, data)
                if (index == 0) {
                    codegen.mv.invokespecial("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false)
                }
            }
        }
    }

    override fun toCallable(method: CallableMethod): Callable =
            object : IntrinsicCallable(method) {
                override fun invokeMethodWithArguments(
                        resolvedCall: ResolvedCall<*>,
                        receiver: StackValue,
                        codegen: ExpressionCodegen
                ): StackValue {
                    if (resolvedCall.call.callElement.parent is KtCallableReferenceExpression) {
                        // NB we come here only in case of inlined callable reference to String::plus.
                        // This will map arguments properly, invoking callbacks defined in Callable.
                        return super.invokeMethodWithArguments(resolvedCall, receiver, codegen)
                    }
                    return StackValue.operation(returnType) {
                        val arguments = resolvedCall.call.valueArguments.map { it.getArgumentExpression()!! }
                        val actualType = generateImpl(
                                codegen, it, returnType,
                                resolvedCall.call.callElement,
                                arguments,
                                StackValue.receiver(resolvedCall, receiver, codegen, this)
                        )
                        StackValue.coerce(actualType, returnType, it)
                    }
                }

                override fun afterReceiverGeneration(v: InstructionAdapter, frameMap: FrameMap) {
                    v.generateNewInstanceDupAndPlaceBeforeStackTop(frameMap, AsmTypes.JAVA_STRING_TYPE, "java/lang/StringBuilder")
                    v.invokespecial("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false)
                }

                override fun invokeIntrinsic(v: InstructionAdapter) {
                    // String::plus has type String.(Any?) -> String, thus we have no argument type information
                    // in case of callable reference passed to a generic function, e.g.:
                    //      charArrayOf('O', 'K').fold("", String::plus)
                    // TODO Make String::plus generic, and invoke proper StringBuilder#append.
                    AsmUtil.genInvokeAppendMethod(v, AsmTypes.OBJECT_TYPE)
                    v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                }
            }
}
