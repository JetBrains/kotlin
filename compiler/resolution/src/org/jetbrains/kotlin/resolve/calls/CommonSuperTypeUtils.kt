/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.util.lowerIfFlexible
import org.jetbrains.kotlin.utils.DFS

fun commonSuperType(types: List<KotlinType>): KotlinType =
    with(CommonSuperTypeCalculator) { SimpleClassicTypeSystemContext.commonSuperType(types) as KotlinType }

fun topologicallySortSuperclassesAndRecordAllInstances(
    type: SimpleType,
    constructorToAllInstances: MutableMap<TypeConstructor, MutableSet<SimpleType>>,
    visited: MutableSet<TypeConstructor>
): List<TypeConstructor> {
    return DFS.dfs(
        listOf(type),
        { current ->
            val substitutor = TypeSubstitutor.create(current)
            val supertypes = current.constructor.supertypes
            val result = mutableListOf<SimpleType>()
            for (supertype in supertypes) {
                if (visited.contains(supertype.constructor)) continue
                result.add(substitutor.safeSubstitute(supertype, Variance.INVARIANT).lowerIfFlexible())
            }
            result
        },
        { current -> visited.add(current.constructor) },
        object : DFS.NodeHandlerWithListResult<SimpleType, TypeConstructor>() {
            override fun beforeChildren(current: SimpleType): Boolean {
                val instances = constructorToAllInstances.computeIfAbsent(current.constructor) { LinkedHashSet() }
                instances.add(current)
                return true
            }

            override fun afterChildren(current: SimpleType) {
                result.addFirst(current.constructor)
            }
        }
    )
}