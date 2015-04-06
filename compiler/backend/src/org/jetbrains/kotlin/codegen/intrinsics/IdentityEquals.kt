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

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.org.objectweb.asm.Type

public class IdentityEquals : IntrinsicMethod() {


    override fun toCallable(method: CallableMethod): Callable {
        return object : IntrinsicCallable(method)  {
            override fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, codegen: ExpressionCodegen): StackValue {
                val element = resolvedCall.getCall().getCallElement()
                val left: StackValue
                val right: StackValue
                if (element is JetCallExpression) {
                    left = StackValue.receiver(resolvedCall, receiver, codegen, this)
                    right = codegen.gen(resolvedCall.getValueArgumentsByIndex()!!.single().getArguments().single().getArgumentExpression())
                }
                else {
                    val e = element as JetBinaryExpression
                    left = codegen.gen(e.getLeft())
                    right = codegen.gen(e.getRight())
                }
                return StackValue.cmp(JetTokens.EQEQEQ, OBJECT_TYPE, left, right)
            }
        }
    }
}
