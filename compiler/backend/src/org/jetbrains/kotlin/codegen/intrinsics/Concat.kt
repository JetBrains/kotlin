/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AsmUtil.genInvokeAppendMethod
import org.jetbrains.kotlin.codegen.AsmUtil.genStringBuilderConstructor
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class Concat : IntrinsicMethod() {
    fun generateImpl(
            codegen: ExpressionCodegen,
            v: InstructionAdapter,
            returnType: Type,
            element: PsiElement?,
            arguments: List<JetExpression>,
            receiver: StackValue
    ): Type {
        if (element is JetBinaryExpression && element.getOperationReference().getReferencedNameElementType() == JetTokens.PLUS) {
            // LHS + RHS
            genStringBuilderConstructor(v)
            codegen.invokeAppend(element.getLeft())
            codegen.invokeAppend(element.getRight())
        }
        else {
            // LHS?.plus(RHS)
            receiver.put(AsmTypes.OBJECT_TYPE, v)
            genStringBuilderConstructor(v)
            v.swap()
            genInvokeAppendMethod(v, returnType)
            codegen.invokeAppend(arguments.get(0))
        }

        v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        return JAVA_STRING_TYPE
    }


    override fun toCallable(method: CallableMethod): Callable =
            object : IntrinsicCallable(method) {
                override fun invokeMethodWithArguments(
                        resolvedCall: ResolvedCall<*>,
                        receiver: StackValue,
                        codegen: ExpressionCodegen
                ): StackValue {
                    return StackValue.operation(returnType) {
                        val arguments = resolvedCall.getCall().getValueArguments().map { it.getArgumentExpression()!! }
                        val actualType = generateImpl(
                                codegen, it, returnType,
                                resolvedCall.getCall().getCallElement(),
                                arguments,
                                StackValue.receiver(resolvedCall, receiver, codegen, this)
                        )
                        StackValue.coerce(actualType, returnType, it)
                    }
                }
            }
}
