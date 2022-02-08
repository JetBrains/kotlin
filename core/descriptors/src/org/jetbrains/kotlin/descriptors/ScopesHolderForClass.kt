/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.TypeRefinement

class ScopesHolderForClass<T : MemberScope> private constructor(
    private val classDescriptor: ClassDescriptor,
    storageManager: StorageManager,
    private val scopeFactory: (KotlinTypeRefiner) -> T,
    private val kotlinTypeRefinerForOwnerModule: KotlinTypeRefiner
) {
    private val scopeForOwnerModule by storageManager.createLazyValue {
        scopeFactory(kotlinTypeRefinerForOwnerModule)
    }

    @OptIn(TypeRefinement::class)
    fun getScope(kotlinTypeRefiner: KotlinTypeRefiner): T {
        /*
         * That check doesn't break anything, because scopeForOwnerModule _will_ anyway refine supertypes from module of
         *   class descriptor.
         *
         * Without that fastpass there is problem wit recursion types such that:
         *
         *   interface A<T : A<T>>
         *
         *   interface B : B<T>
         *
         * In this case (without check) we start compute default type of class descriptor B, go to isRefinementNeededForTypeConstructor,
         *   ask for supertypes of B, see A<B>, ask default type of class B and fail with recursion problem
         */
        if (!kotlinTypeRefiner.isRefinementNeededForModule(classDescriptor.module)) return scopeForOwnerModule

        if (!kotlinTypeRefiner.isRefinementNeededForTypeConstructor(classDescriptor.typeConstructor)) return scopeForOwnerModule
        return kotlinTypeRefiner.getOrPutScopeForClass(classDescriptor) { scopeFactory(kotlinTypeRefiner) }
    }

    companion object {
        fun <T : MemberScope> create(
            classDescriptor: ClassDescriptor,
            storageManager: StorageManager,
            kotlinTypeRefinerForOwnerModule: KotlinTypeRefiner,
            scopeFactory: (KotlinTypeRefiner) -> T
        ): ScopesHolderForClass<T> {
            return ScopesHolderForClass(classDescriptor, storageManager, scopeFactory, kotlinTypeRefinerForOwnerModule)
        }
    }
}
