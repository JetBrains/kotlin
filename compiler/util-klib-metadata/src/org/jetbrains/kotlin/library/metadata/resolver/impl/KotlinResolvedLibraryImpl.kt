package org.jetbrains.kotlin.library.metadata.resolver.impl

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary

class KotlinResolvedLibraryImpl(override val library: KotlinLibrary) : KotlinResolvedLibrary {
    override val resolvedDependencies: List<KotlinResolvedLibrary>
        field = mutableListOf<KotlinResolvedLibrary>()

    internal fun addDependency(resolvedLibrary: KotlinResolvedLibrary): Boolean = resolvedDependencies.add(resolvedLibrary)

    override fun toString(): String = "library=$library, dependsOn=${resolvedDependencies.joinToString { it.library.toString() }}"
}
