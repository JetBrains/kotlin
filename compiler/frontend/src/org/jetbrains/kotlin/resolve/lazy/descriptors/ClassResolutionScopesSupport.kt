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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.ThrowingLexicalScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class ClassResolutionScopesSupport(
        private val classDescriptor: ClassDescriptor,
        storageManager: StorageManager,
        private val getOuterScope: () -> LexicalScope
) {
    private fun scopeWithGenerics(parent: LexicalScope): LexicalScopeImpl {
        return LexicalScopeImpl(parent, classDescriptor, false, null, LexicalScopeKind.CLASS_HEADER) {
            classDescriptor.declaredTypeParameters.forEach { addClassifierDescriptor(it) }
        }
    }

    val scopeForClassHeaderResolution: () -> LexicalScope = storageManager.createLazyValue {
        scopeWithGenerics(getOuterScope())
    }

    val scopeForConstructorHeaderResolution: () -> LexicalScope = storageManager.createLazyValue {
        scopeWithGenerics(inheritanceScopeWithMe())
    }

    private val inheritanceScopeWithoutMe: () -> LexicalScope = storageManager.createLazyValue(onRecursion = createThrowingLexicalScope) {
        classDescriptor.getAllSuperclassesWithoutAny().asReversed().fold(getOuterScope()) { scope, currentClass ->
            createInheritanceScope(parent = scope, ownerDescriptor = classDescriptor, classDescriptor = currentClass)
        }
    }

    private val inheritanceScopeWithMe: () -> LexicalScope = storageManager.createLazyValue(onRecursion = createThrowingLexicalScope) {
        createInheritanceScope(parent = inheritanceScopeWithoutMe(), ownerDescriptor = classDescriptor, classDescriptor = classDescriptor)
    }

    val scopeForCompanionObjectHeaderResolution: () -> LexicalScope = storageManager.createLazyValue(onRecursion = createThrowingLexicalScope) {
        createInheritanceScope(inheritanceScopeWithoutMe(), classDescriptor, classDescriptor, withCompanionObject = false)
    }

    val scopeForMemberDeclarationResolution: () -> LexicalScope = storageManager.createLazyValue {
        val scopeWithGenerics = scopeWithGenerics(inheritanceScopeWithMe())
        LexicalScopeImpl(scopeWithGenerics, classDescriptor, true, classDescriptor.thisAsReceiverParameter, LexicalScopeKind.CLASS_MEMBER_SCOPE)
    }

    val scopeForStaticMemberDeclarationResolution: () -> LexicalScope = storageManager.createLazyValue(onRecursion = createThrowingLexicalScope) {
        if (classDescriptor.kind.isSingleton) {
            scopeForMemberDeclarationResolution()
        }
        else {
            inheritanceScopeWithMe()
        }
    }

    private fun createInheritanceScope(
            parent: LexicalScope,
            ownerDescriptor: DeclarationDescriptor,
            classDescriptor: ClassDescriptor,
            withCompanionObject: Boolean = true
    ): LexicalScope {
        val staticScopes = ArrayList<MemberScope>(3)

        // todo filter fake overrides
        staticScopes.add(classDescriptor.staticScope)

        staticScopes.add(classDescriptor.unsubstitutedInnerClassesScope)

        val implicitReceiver: ReceiverParameterDescriptor?

        val parentForNewScope: LexicalScope

        if (withCompanionObject) {
            staticScopes.addIfNotNull(classDescriptor.companionObjectDescriptor?.unsubstitutedInnerClassesScope)
            implicitReceiver = classDescriptor.companionObjectDescriptor?.thisAsReceiverParameter

            parentForNewScope = classDescriptor.companionObjectDescriptor?.let {
                it.getAllSuperclassesWithoutAny().asReversed().fold(parent) { scope, currentClass ->
                    createInheritanceScope(parent = scope, ownerDescriptor = ownerDescriptor, classDescriptor = currentClass, withCompanionObject = false)
                }
            } ?: parent
        }
        else {
            implicitReceiver = null
            parentForNewScope = parent
        }

        return LexicalChainedScope(parentForNewScope, ownerDescriptor, false,
                                   implicitReceiver,
                                   LexicalScopeKind.CLASS_INHERITANCE,
                                   memberScopes = staticScopes, isStaticScope = true)
    }

    private fun <T : Any> StorageManager.createLazyValue(onRecursion: ((Boolean) -> T), compute: () -> T) =
            createLazyValueWithPostCompute(compute, onRecursion, {})

    companion object {
        private val createThrowingLexicalScope: (Boolean) -> LexicalScope =  { ThrowingLexicalScope() }
    }
}

fun scopeForInitializerResolution(
        classDescriptor: LazyClassDescriptor,
        parentDescriptor: DeclarationDescriptor,
        primaryConstructorParameters: List<KtParameter>
): LexicalScope {
    return LexicalScopeImpl(
            classDescriptor.scopeForMemberDeclarationResolution,
            parentDescriptor,
            false,
            null,
            LexicalScopeKind.CLASS_INITIALIZER
    ) {
        if (primaryConstructorParameters.isNotEmpty()) {
            val parameterDescriptors = classDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters
            assert(parameterDescriptors.size == primaryConstructorParameters.size)
            for ((parameter, descriptor) in primaryConstructorParameters.zip(parameterDescriptors)) {
                if (!parameter.hasValOrVar()) {
                    addVariableDescriptor(descriptor)
                }
            }
        }
    }
}
