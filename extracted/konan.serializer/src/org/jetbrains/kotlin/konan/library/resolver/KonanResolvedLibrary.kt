package org.jetbrains.kotlin.konan.library.resolver

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.name.FqName

interface PackageAccessedHandler {

    fun markPackageAccessed(fqName: FqName)
}

/**
 * A [KonanLibrary] wrapper that is used for resolving library's dependencies.
 */
interface KonanResolvedLibrary: PackageAccessedHandler {

    // the library itself
    val library: KonanLibrary

    // dependencies on other libraries
    val resolvedDependencies: List<KonanResolvedLibrary>

    // if it's needed to linker
    val isNeededForLink: Boolean

    // is provided by the distribution
    val isDefault: Boolean
}
