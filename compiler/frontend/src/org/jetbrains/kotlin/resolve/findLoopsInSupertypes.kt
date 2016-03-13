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

@file:JvmName("FindLoopsInSupertypes")
package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartList

class SupertypeLoopCheckerImpl : SupertypeLoopChecker {
    override fun findLoopsInSupertypesAndDisconnect(
            currentTypeConstructor: TypeConstructor,
            superTypes: Collection<KotlinType>,
            neighbors: (TypeConstructor) -> Iterable<KotlinType>,
            reportLoop: (KotlinType) -> Unit
    ): Collection<KotlinType> {
        val graph = DFS.Neighbors<TypeConstructor> { node -> neighbors(node).map { it.constructor } }

        val superTypesToRemove = SmartList<KotlinType>()

        for (superType in superTypes) {
            if (isReachable(superType.constructor, currentTypeConstructor, graph)) {
                superTypesToRemove.add(superType)
                reportLoop(superType)
            }
        }

        return if (superTypesToRemove.isEmpty()) superTypes else superTypes - superTypesToRemove
    }
}

private fun isReachable(
        from: TypeConstructor, to: TypeConstructor,
        neighbors: DFS.Neighbors<TypeConstructor>
): Boolean {
    var result = false
    DFS.dfs(listOf(from), neighbors, DFS.VisitedWithSet(), object : DFS.AbstractNodeHandler<TypeConstructor, Unit>() {
        override fun beforeChildren(current: TypeConstructor): Boolean {
            if (current == to) {
                result = true
                return false
            }
            return true
        }

        override fun result() = Unit
    })

    return result
}
