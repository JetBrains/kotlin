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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.org.objectweb.asm.Type

object CodegenContextUtil {
    @JvmStatic
    fun getImplementationOwnerClassType(owner: CodegenContext<*>): Type? =
        when (owner) {
            is MultifileClassFacadeContext -> owner.filePartType
            is DelegatingToPartContext -> owner.implementationOwnerClassType
            else -> null
        }

    @JvmStatic
    fun isImplementationOwner(owner: CodegenContext<*>, descriptor: DeclarationDescriptor): Boolean {
        if (descriptor.containingDeclaration?.isInlineClass() == true) {
            val isInErasedMethod = owner.contextKind == OwnerKind.ERASED_INLINE_CLASS
            when (descriptor) {
                is FunctionDescriptor -> return isInErasedMethod
                is PropertyDescriptor -> return !isInErasedMethod
            }
        }
        return owner !is MultifileClassFacadeContext
    }
}
