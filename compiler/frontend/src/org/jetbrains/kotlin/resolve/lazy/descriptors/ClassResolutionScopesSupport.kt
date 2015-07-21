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
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.StorageManager

class ClassResolutionScopesSupport(
        private val classDescriptor: ClassDescriptor,
        storageManager: StorageManager,
        private val getOuterScope: () -> JetScope
) {
    private val staticScope = StaticScopeForKotlinClass(classDescriptor)

    private val scopeForClassHeaderResolution = storageManager.createLazyValue { computeScopeForClassHeaderResolution() }
    private val scopeForMemberDeclarationResolution = storageManager.createLazyValue { computeScopeForMemberDeclarationResolution() }

    fun getStaticScope(): JetScope = staticScope

    fun getScopeForClassHeaderResolution(): JetScope = scopeForClassHeaderResolution()

    fun getScopeForMemberDeclarationResolution(): JetScope = scopeForMemberDeclarationResolution()

    private fun computeScopeForClassHeaderResolution(): JetScope {
        val scope = WritableScopeImpl(JetScope.Empty, classDescriptor, RedeclarationHandler.DO_NOTHING, "Scope with type parameters for " + classDescriptor.getName())
        for (typeParameterDescriptor in classDescriptor.getTypeConstructor().getParameters()) {
            scope.addClassifierDescriptor(typeParameterDescriptor)
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING)

        return ChainedScope(classDescriptor, "ScopeForClassHeaderResolution: " + classDescriptor.getName(), scope, getOuterScope())
    }

    private fun computeScopeForMemberDeclarationResolution(): JetScope {
        val thisScope = WritableScopeImpl(JetScope.Empty, classDescriptor, RedeclarationHandler.DO_NOTHING,
                                          "Scope with 'this' for " + classDescriptor.getName(), classDescriptor.getThisAsReceiverParameter(), classDescriptor)
        thisScope.changeLockLevel(WritableScope.LockLevel.READING)

        return ChainedScope(
                classDescriptor,
                "ScopeForMemberDeclarationResolution: " + classDescriptor.getName(),
                thisScope,
                classDescriptor.getUnsubstitutedMemberScope(),
                getScopeForClassHeaderResolution(),
                getCompanionObjectScope(),
                classDescriptor.getStaticScope())
    }

    private fun getCompanionObjectScope(): JetScope {
        val companionObjectDescriptor = classDescriptor.getCompanionObjectDescriptor()
        return if ((companionObjectDescriptor != null)) CompanionObjectMixinScope(companionObjectDescriptor) else JetScope.Empty
    }
}