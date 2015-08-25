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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.resolve.scopes.LexicalChainedScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.storage.StorageManager

class ClassResolutionScopesSupport(
        private val classDescriptor: ClassDescriptor,
        storageManager: StorageManager,
        private val getOuterScope: () -> LexicalScope,
        private val primaryConstructorParameters: List<JetParameter>? = null
) {
    private fun scopeWithGenerics(parent: LexicalScope, debugName: String): LexicalScopeImpl {
        return LexicalScopeImpl(parent, classDescriptor, false, null, debugName) {
            classDescriptor.typeConstructor.parameters.forEach { addClassifierDescriptor(it) }
        }
    }

    public val scopeForClassHeaderResolution: () -> LexicalScope = storageManager.createLazyValue {
        scopeWithGenerics(getOuterScope(), "Scope for class header resolution for ${classDescriptor.name}")
    }

    private val scopeWithStaticMembersAndCompanionObjectReceiver = storageManager.createLazyValue {
        val staticScopes = classDescriptor.companionObjectDescriptor?.let {
            arrayOf(classDescriptor.staticScope, it.unsubstitutedInnerClassesScope)
        } ?: arrayOf(classDescriptor.staticScope)

        LexicalChainedScope(getOuterScope(), classDescriptor, false,
                            classDescriptor.companionObjectDescriptor?.thisAsReceiverParameter,
                            "Scope with static members and companion object for ${classDescriptor.name}",
                            memberScopes = *staticScopes)
    }

    public val scopeForMemberDeclarationResolution: () -> LexicalScope = storageManager.createLazyValue {
        val scopeWithGenerics = scopeWithGenerics(scopeWithStaticMembersAndCompanionObjectReceiver(),
                                                  "Scope with generics for ${classDescriptor.name}")
        LexicalChainedScope(scopeWithGenerics, classDescriptor, true, classDescriptor.thisAsReceiverParameter,
                            "Scope for member declaration resolution: ${classDescriptor.name}",
                            classDescriptor.unsubstitutedInnerClassesScope)
    }

    public val scopeForStaticMemberDeclarationResolution: () -> LexicalScope = storageManager.createLazyValue {
        if (classDescriptor.kind.isSingleton) {
            scopeForMemberDeclarationResolution()
        }
        else {
            LexicalChainedScope(scopeWithStaticMembersAndCompanionObjectReceiver(), classDescriptor, false, null,
                                "Scope for static member declaration resolution: ${classDescriptor.name}",
                                classDescriptor.unsubstitutedInnerClassesScope)
        }
    }

    public val scopeForInitializerResolution: () -> LexicalScope = storageManager.createLazyValue {
        val primaryConstructor = classDescriptor.unsubstitutedPrimaryConstructor ?:
                                 return@createLazyValue scopeForMemberDeclarationResolution()
        assert(primaryConstructorParameters != null) {
            "primary constructor parameters must be not null, because primary constructor exist: $primaryConstructor"
        }
        LexicalScopeImpl(scopeForMemberDeclarationResolution(), primaryConstructor, false, null,
                         "Scope for initializer resolution: ${classDescriptor.name}") {
            primaryConstructorParameters!!.forEachIndexed {
                index, parameter ->
                if (!parameter.hasValOrVar()) {
                    addVariableDescriptor(primaryConstructor.valueParameters[index])
                }
            }
        }
    }

}