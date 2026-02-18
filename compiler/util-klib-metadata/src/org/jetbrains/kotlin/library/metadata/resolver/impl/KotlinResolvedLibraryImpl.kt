package org.jetbrains.kotlin.library.metadata.resolver.impl

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary

class KotlinResolvedLibraryImpl(override val library: KotlinLibrary) : KotlinResolvedLibrary {

    private val _resolvedDependencies = mutableListOf<KotlinResolvedLibrary>()
    private val _emptyPackages by lazy { parseModuleHeader(library.metadata.moduleHeaderData).emptyPackageList }

    override val resolvedDependencies: List<KotlinResolvedLibrary>
        get() = _resolvedDependencies

    internal fun addDependency(resolvedLibrary: KotlinResolvedLibrary) = _resolvedDependencies.add(resolvedLibrary)

    override var isNeededForLink: Boolean = false
        private set

    override fun markNeededForLink(packageFqName: String) {
        if (!isNeededForLink // fast path
            && !_emptyPackages.contains(packageFqName)
        ) {
            isNeededForLink = true
        }
    }

    override fun toString() = "library=$library, dependsOn=${_resolvedDependencies.joinToString { it.library.toString() }}"
}
