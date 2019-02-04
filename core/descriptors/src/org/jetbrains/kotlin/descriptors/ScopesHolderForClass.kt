/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.isExpectClass
import org.jetbrains.kotlin.utils.DFS

class ScopesHolderForClass<T : MemberScope>(private val scopeOrMemoizedFunction: Any) {

    fun getScope(moduleDescriptor: ModuleDescriptor): T =
        @Suppress("UNCHECKED_CAST")
        when (scopeOrMemoizedFunction) {
            is MemoizedFunctionToNotNull<*, *> ->
                (scopeOrMemoizedFunction as MemoizedFunctionToNotNull<ModuleDescriptor, T>).invoke(moduleDescriptor)
            else -> scopeOrMemoizedFunction as T
        }

    companion object {
        fun <T : MemberScope> create(
            classDescriptor: ClassDescriptor,
            storageManager: StorageManager,
            scopeFactory: (ModuleDescriptor) -> T
        ): NotNullLazyValue<ScopesHolderForClass<T>> {
              return storageManager.createLazyValue {
                  val typeConstructor = classDescriptor.typeConstructor

                  val value: Any =
                      if (typeConstructor.areThereExpectSupertypes())
                          storageManager.createMemoizedFunction(scopeFactory)
                      else
                          scopeFactory(classDescriptor.module)

                  ScopesHolderForClass<T>(value)
              }
        }
    }
}

fun TypeConstructor.areThereExpectSupertypes(): Boolean {
    var result = false
    DFS.dfs(
        listOf(this),
        DFS.Neighbors { current ->
            current.supertypes.map { it.constructor }
        },
        DFS.VisitedWithSet(),
        object : DFS.AbstractNodeHandler<TypeConstructor, Unit>() {
            override fun beforeChildren(current: TypeConstructor): Boolean {
                if (current.isExpectClass()) {
                    result = true
                    return false
                }
                return true
            }

            override fun result() = Unit
        }
    )

    return result
}
