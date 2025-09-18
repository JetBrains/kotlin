/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.superclasses

internal fun getNewMembers(kClass: KClass<*>): List<DescriptorKCallable<*>> {
    val result = ArrayList<DescriptorKCallable<*>>()
    result.addAll(kClass.declaredDescriptorKCallableMembers)
    val visited = HashSet<KClass<*>>()
    visited.add(kClass)
    for (superclass in kClass.superclasses) {
        getSupertypesMembersRecursive(superclass, visited, result)
    }
    return result
}

private fun getSupertypesMembersRecursive(
    kClass: KClass<*>,
    visited: HashSet<KClass<*>>,
    out: ArrayList<DescriptorKCallable<*>>,
) {
    if (!visited.add(kClass)) return
    out.addAll(kClass.declaredDescriptorKCallableMembers.filter { it.visibility != KVisibility.PRIVATE })
    for (superclass in kClass.superclasses) {
        getSupertypesMembersRecursive(superclass, visited, out)
    }
}

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredDescriptorKCallableMembers: Collection<DescriptorKCallable<*>>
    get() = declaredMembers as Collection<DescriptorKCallable<*>>
