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

import org.jetbrains.kotlin.codegen.AsmUtil.putJavaLangClassInstance
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class JavaClassFunction : IntrinsicMethod() {
    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable {
        val javaClass = resolvedCall.resultingDescriptor.returnType!!.arguments.first().type
        return object : IntrinsicCallable(getType(Class::class.java), listOf(), null, null) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter(javaClass, ReifiedTypeInliner.OperationKind.JAVA_CLASS)
                putJavaLangClassInstance(v, codegen.getState().typeMapper.mapType(javaClass))
            }
        }
    }
}
