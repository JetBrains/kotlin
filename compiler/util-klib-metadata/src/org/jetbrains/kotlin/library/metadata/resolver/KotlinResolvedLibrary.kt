package org.jetbrains.kotlin.library.metadata.resolver

import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * A [KotlinLibrary] wrapper that is used for resolving library's dependencies.
 */
interface KotlinResolvedLibrary {

    // The library itself.
    val library: KotlinLibrary

    // Dependencies on other libraries.
    val resolvedDependencies: List<KotlinResolvedLibrary>
}
