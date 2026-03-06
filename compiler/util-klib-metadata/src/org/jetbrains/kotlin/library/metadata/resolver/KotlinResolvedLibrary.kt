package org.jetbrains.kotlin.library.metadata.resolver

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler

/**
 * A [KotlinLibrary] wrapper that is used for resolving library's dependencies.
 */
interface KotlinResolvedLibrary : PackageAccessHandler {

    // The library itself.
    val library: KotlinLibrary

    // Dependencies on other libraries.
    val resolvedDependencies: List<KotlinResolvedLibrary>

    // Any package fragment within this library has beed visited during frontend resolve phase.
    // You need to utilize PackageAccessHandler to make it work for you.
    val isNeededForLink: Boolean

    // Is provided by the distribution?
    val isDefault: Boolean
}
