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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LexicalChainedScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

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

    private val inheritanceScope: () -> LexicalScope = storageManager.createLazyValueWithPostCompute(
            {
                classDescriptor.getAllSuperclassesAndMeWithoutAny().asReversed().fold(getOuterScope()) { scope, currentClass ->
                    createInheritanceScope(parent = scope, ownerDescriptor = classDescriptor, classDescriptor = currentClass)
                }
            },
            { createInheritanceScope(getOuterScope(), classDescriptor, classDescriptor) },
            {}
    )


    public val scopeForMemberDeclarationResolution: () -> LexicalScope = storageManager.createLazyValue {
        val scopeWithGenerics = scopeWithGenerics(inheritanceScope(),
                                                  "Scope with generics for ${classDescriptor.name}")
        LexicalScopeImpl(scopeWithGenerics, classDescriptor, true, classDescriptor.thisAsReceiverParameter,
                              "Scope for member declaration resolution: ${classDescriptor.name}")
    }

    public val scopeForStaticMemberDeclarationResolution: () -> LexicalScope = storageManager.createLazyValue {
        if (classDescriptor.kind.isSingleton) {
            scopeForMemberDeclarationResolution()
        }
        else {
            LexicalScopeImpl(inheritanceScope(), classDescriptor, false, null,
                             "Scope for static member declaration resolution: ${classDescriptor.name}")
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


    public fun ClassDescriptor.getAllSuperclassesAndMeWithoutAny(): List<ClassDescriptor> {
        val superClassesAndMe = SmartList<ClassDescriptor>()
        var parent: ClassDescriptor? = this
        do {
            superClassesAndMe.add(parent)
            parent = parent!!.getSuperClassNotAny()
        }
        while(parent != null && parent != this) // possible recursion in inheritance

        return superClassesAndMe
    }

    private fun createInheritanceScope(
            parent: LexicalScope,
            ownerDescriptor: DeclarationDescriptor,
            classDescriptor: ClassDescriptor
    ): LexicalScope {
        val staticScopes = ArrayList<JetScope>(3)

        // todo filter fake overrides
        staticScopes.add(classDescriptor.staticScope)

        staticScopes.add(classDescriptor.unsubstitutedInnerClassesScope)
        staticScopes.addIfNotNull(classDescriptor.companionObjectDescriptor?.unsubstitutedInnerClassesScope)

        return LexicalChainedScope(parent, ownerDescriptor, false,
                            classDescriptor.companionObjectDescriptor?.thisAsReceiverParameter,
                            "Scope with static members, nested classes and companion object for ${classDescriptor.name}",
                            memberScopes = *staticScopes.toTypedArray(), isStaticScope = true)
    }

}