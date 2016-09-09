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

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.stubindex.resolve.PluginDeclarationProviderFactory
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.LocalDescriptorResolver
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

class IdeaLocalDescriptorResolver(
        private val resolveElementCache: ResolveElementCache,
        private val absentDescriptorHandler: AbsentDescriptorHandler
) : LocalDescriptorResolver {
    override fun resolveLocalDeclaration(declaration: KtDeclaration): DeclarationDescriptor {
        val context = resolveElementCache.resolveToElements(listOf(declaration), BodyResolveMode.FULL)
        return context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
               ?: absentDescriptorHandler.diagnoseDescriptorNotFound(declaration)
    }
}

class IdeaAbsentDescriptorHandler(
        private val declarationProviderFactory: DeclarationProviderFactory
) : AbsentDescriptorHandler {
    override fun diagnoseDescriptorNotFound(declaration: KtDeclaration): DeclarationDescriptor {
        throw NoDescriptorForDeclarationException(
                declaration,
                (declarationProviderFactory as? PluginDeclarationProviderFactory)?.debugToString()
        )
    }
}