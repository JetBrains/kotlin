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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils

interface LocalDescriptorResolver {
    fun resolveLocalDeclaration(declaration: KtDeclaration): DeclarationDescriptor
}

class CompilerLocalDescriptorResolver(
        private val lazyDeclarationResolver: LazyDeclarationResolver
) : LocalDescriptorResolver {
    override fun resolveLocalDeclaration(declaration: KtDeclaration): DeclarationDescriptor {
        return lazyDeclarationResolver.resolveToDescriptor(declaration)
    }
}