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

package org.jetbrains.kotlin.codegen.context

import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

class InlineLambdaContext(
        functionDescriptor: FunctionDescriptor,
        contextKind: OwnerKind,
        parentContext: CodegenContext<*>,
        closure: MutableClosure?,
        private val isCrossInline: Boolean,
        private val isPropertyReference: Boolean
) : MethodContext(functionDescriptor, contextKind, parentContext, closure, false) {

    override fun getFirstCrossInlineOrNonInlineContext(): CodegenContext<*> {
        if (isCrossInline) return this

        val parent = if (isPropertyReference) parentContext as? AnonymousClassContext else  { parentContext as? ClosureContext } ?:
                     throw AssertionError(
                             "Parent of inlining lambda body should be " +
                             "${if (isPropertyReference) "ClosureContext" else "AnonymousClassContext"}, but: $parentContext"
                     )

        val grandParent = parent.parentContext ?:
                          throw AssertionError("Parent context of lambda class context should exist: $contextDescriptor")
        return grandParent.firstCrossInlineOrNonInlineContext
    }

}