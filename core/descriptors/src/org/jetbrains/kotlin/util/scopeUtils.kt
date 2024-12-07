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

package org.jetbrains.kotlin.util.collectionUtils

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.filterIsInstanceAndTo

/**
 * Concatenates the contents of this collection with the given collection, avoiding allocations if possible.
 * Can modify `this` if it is a mutable collection.
 */
fun <T> Collection<T>?.concat(collection: Collection<T>): Collection<T>? {
    if (collection.isEmpty()) {
        return this
    }
    if (this == null) {
        return collection
    }
    if (this is LinkedHashSet) {
        addAll(collection)
        return this
    }

    val result = LinkedHashSet(this)
    result.addAll(collection)
    return result
}

inline fun <Scope, T> getFromAllScopes(scopes: Array<Scope>, callback: (Scope) -> Collection<T>): Collection<T> =
    when (scopes.size) {
        0 -> emptyList()
        1 -> callback(scopes[0])
        else -> {
            var result: Collection<T>? = null
            for (scope in scopes) {
                result = result.concat(callback(scope))
            }
            result ?: emptySet()
        }
    }

inline fun <Scope, T> getFromAllScopes(firstScope: Scope, restScopes: Array<Scope>, callback: (Scope) -> Collection<T>): Collection<T> {
    var result: Collection<T>? = callback(firstScope)
    for (scope in restScopes) {
        result = result.concat(callback(scope))
    }
    return result ?: emptySet()
}

inline fun <Scope, R> flatMapScopes(scope1: Scope?, scope2: Scope?, transform: (Scope) -> Collection<R>): Collection<R> {
    val results1 = if (scope1 != null) transform(scope1) else emptyList()
    if (scope2 == null) return results1
    else {
        val results2 = transform(scope2)
        if (results1.isEmpty()) return results2
        else return results1.toMutableList().also {
            it.addAll(results2)
        }
    }
}

inline fun <Scope> forEachScope(scope1: Scope?, scope2: Scope?, action: (Scope) -> Unit) {
    if (scope1 != null) action(scope1)
    if (scope2 != null) action(scope2)
}

fun listOfNonEmptyScopes(vararg scopes: MemberScope?): SmartList<MemberScope> =
    scopes.filterTo(SmartList<MemberScope>()) { it != null && it !== MemberScope.Empty }

fun listOfNonEmptyScopes(scopes: Iterable<MemberScope?>): SmartList<MemberScope> =
    scopes.filterTo(SmartList<MemberScope>()) { it != null && it !== MemberScope.Empty }

inline fun <Scope, T : ClassifierDescriptor> getFirstClassifierDiscriminateHeaders(scopes: Array<Scope>, callback: (Scope) -> T?): T? {
    // NOTE: This is performance-sensitive; please don't replace with map().firstOrNull()
    var result: T? = null
    for (scope in scopes) {
        val newResult = callback(scope)
        if (newResult != null) {
            if (newResult is ClassifierDescriptorWithTypeParameters && newResult.isExpect) {
                if (result == null) result = newResult
            }
            // this class is Impl or usual class
            else {
                return newResult
            }
        }
    }
    return result
}

inline fun <reified R> Iterable<*>.filterIsInstanceAnd(predicate: (R) -> Boolean): List<R> {
    return filterIsInstanceAndTo(SmartList(), predicate)
}
