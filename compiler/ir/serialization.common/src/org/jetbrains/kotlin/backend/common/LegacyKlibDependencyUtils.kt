/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.topologicalSort

/**
 * This functions allows sorting the libraries in the reverse topological order using the legacy manifest-based mechanics.
 * I.e. [KLIB_PROPERTY_UNIQUE_NAME] and [KLIB_PROPERTY_DEPENDS] properties.
 *
 * TODO(KT-70118): This is a temporary solution for the Kotlin/Native compiler's second stage. It is going to be
 *  replaced by the IR linker-based dependency computation or another sustainable solution.
 */
fun Collection<KotlinLibrary>.legacyKlibReverseTopoSort(): List<KotlinLibrary> {
    if (size <= 1) return toList()

    val dependencies = LegacyKlibDependencies(this)

    return topologicalSort(
        nodes = this,
        reportCycle = { library -> error("Cyclic dependency in library graph for: ${library.location}") },
        dependencies = { dependencies.getDependenciesFor(this@topologicalSort) }
    ).reversed()
}

/**
 * This class allows computing the dependencies of a library using the legacy manifest-based mechanics.
 * I.e. [KLIB_PROPERTY_UNIQUE_NAME] and [KLIB_PROPERTY_DEPENDS] properties.
 *
 * TODO(KT-70118): This is a temporary solution for the Kotlin/Native compiler's second stage. It is going to be
 *  replaced by the IR linker-based dependency computation or another sustainable solution.
 */
class LegacyKlibDependencies(libraries: Collection<KotlinLibrary>) {
    private val uniqueNameToLibrary: Map<String, KotlinLibrary> = libraries.associateBy { it.uniqueName }

    fun getDependenciesFor(library: KotlinLibrary): Collection<KotlinLibrary> {
        return library.manifestProperties
            .propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true)
            .mapNotNull(uniqueNameToLibrary::get)
    }
}
