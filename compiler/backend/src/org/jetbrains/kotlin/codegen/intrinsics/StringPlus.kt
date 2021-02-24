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

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class StringPlus : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable =
        object : IntrinsicCallable(method) {
            private lateinit var generator: StringConcatGenerator

            override fun invokeMethodWithArguments(
                resolvedCall: ResolvedCall<*>,
                receiver: StackValue,
                codegen: ExpressionCodegen
            ): StackValue {
                generator = StringConcatGenerator.create(codegen.state, codegen.v)
                return super.invokeMethodWithArguments(resolvedCall, receiver, codegen)
            }

            override fun genInvokeInstruction(v: InstructionAdapter) {
                if (!generator.mode.isDynamic) {
                    v.invokestatic(
                        IntrinsicMethods.INTRINSICS_CLASS_NAME, "stringPlus",
                        "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;", false
                    )
                } else {
                    generator.invokeAppend(AsmTypes.JAVA_STRING_TYPE)
                    //TODO: process constants properly, do not upcast to object
                    generator.invokeAppend(AsmTypes.OBJECT_TYPE)
                    generator.genToString()
                }
            }
        }
}
