package org.jetbrains.kotlin.library.metadata.resolver.impl

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary

class KotlinResolvedLibraryImpl(override val library: KotlinLibrary) : KotlinResolvedLibrary {

    private val _resolvedDependencies = mutableListOf<KotlinResolvedLibrary>()

    override val resolvedDependencies: List<KotlinResolvedLibrary>
        get() = _resolvedDependencies

    internal fun addDependency(resolvedLibrary: KotlinResolvedLibrary) = _resolvedDependencies.add(resolvedLibrary)

    override fun toString() = "library=$library, dependsOn=${_resolvedDependencies.joinToString { it.library.toString() }}"
}
