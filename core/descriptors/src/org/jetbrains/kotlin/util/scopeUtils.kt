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
import java.util.*

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

fun <T> concatInOrder(c1: Collection<T>?, c2: Collection<T>?): Collection<T> {
    val result = if (c1 == null || c1.isEmpty())
        c2
    else if (c2 == null || c2.isEmpty())
        c1
    else {
        val result = LinkedHashSet<T>()
        result.addAll(c1)
        result.addAll(c2)
        result
    }
    return result ?: emptySet()
}

inline fun <Scope, T> getFromAllScopes(scopes: List<Scope>, callback: (Scope) -> Collection<T>): Collection<T> {
    if (scopes.isEmpty()) return emptySet()
    var result: Collection<T>? = null
    for (scope in scopes) {
        result = result.concat(callback(scope))
    }
    return result ?: emptySet()
}

inline fun <Scope, T> getFromAllScopes(firstScope: Scope, restScopes: List<Scope>, callback: (Scope) -> Collection<T>): Collection<T> {
    var result: Collection<T>? = callback(firstScope)
    for (scope in restScopes) {
        result = result.concat(callback(scope))
    }
    return result ?: emptySet()
}

inline fun <Scope, T : ClassifierDescriptor> getFirstClassifierDiscriminateHeaders(scopes: List<Scope>, callback: (Scope) -> T?): T? {
    // NOTE: This is performance-sensitive; please don't replace with map().firstOrNull()
    var result: T? = null
    for (scope in scopes) {
        val newResult = callback(scope)
        if (newResult != null) {
            if (newResult is ClassifierDescriptorWithTypeParameters && newResult.isHeader) {
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
