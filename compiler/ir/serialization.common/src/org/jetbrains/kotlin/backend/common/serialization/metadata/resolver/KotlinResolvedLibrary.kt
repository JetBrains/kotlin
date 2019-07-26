package org.jetbrains.kotlin.library.resolver

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.PackageAccessedHandler

/**
 * A [KotlinLibrary] wrapper that is used for resolving library's dependencies.
 */
interface KotlinResolvedLibrary: PackageAccessedHandler {

    // The library itself.
    val library: KotlinLibrary

    // Dependencies on other libraries.
    val resolvedDependencies: List<KotlinResolvedLibrary>

    // Whether it is needed to linker.
    val isNeededForLink: Boolean

    // Is provided by the distribution?
    val isDefault: Boolean
}
