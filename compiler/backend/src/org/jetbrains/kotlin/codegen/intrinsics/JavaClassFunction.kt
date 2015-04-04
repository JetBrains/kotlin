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
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.codegen.AsmUtil.putJavaLangClassInstance
import org.jetbrains.kotlin.codegen.ExtendedCallable
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType

public class JavaClassFunction : IntrinsicMethod() {
    override fun generateImpl(codegen: ExpressionCodegen, v: InstructionAdapter, expectedType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): Type {
        val resolvedCall = (element as JetElement).getResolvedCallWithAssert(codegen.getBindingContext())
        val returnType = resolvedCall.getResultingDescriptor().getReturnType()
        assert(returnType != null)

        val type = returnType!!.getArguments().get(0).getType()

        codegen.putReifierMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.JAVA_CLASS_MARKER_METHOD_NAME)

        putJavaLangClassInstance(v, codegen.getState().getTypeMapper().mapType(type))

        return getType(javaClass<Class<Any>>())
    }

    override fun supportCallable(): Boolean {
        return true
    }

    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): ExtendedCallable {
        val javaClass = resolvedCall.getResultingDescriptor().getReturnType()!!.getArguments().get(0).getType()
        return object: IntrinsicCallable(getType(javaClass<Class<Any>>()), listOf(), null, null) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                codegen.putReifierMarkerIfTypeIsReifiedParameter(javaClass, ReifiedTypeInliner.JAVA_CLASS_MARKER_METHOD_NAME)
                putJavaLangClassInstance(v, codegen.getState().getTypeMapper().mapType(javaClass))
            }
        }

    }
}
