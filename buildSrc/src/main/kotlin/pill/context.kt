@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.artifacts.*

class DependencyMapper(
    val predicate: (ResolvedDependency) -> Boolean,
    vararg val configurations: String,
    val mapping: (ResolvedDependency) -> MappedDependency?
) {
    constructor(
        group: String,
        module: String,
        vararg configurations: String,
        version: String? = null,
        mapping: (ResolvedDependency) -> MappedDependency?
    ) : this(
            { dep ->
                dep.moduleGroup == group
                && dep.moduleName == module
                && (version == null || dep.moduleVersion == version)
            },
            configurations = *configurations,
            mapping = mapping
        )
}

class MappedDependency(val main: PDependency?, val deferred: List<PDependency> = emptyList())

class ParserContext(val dependencyMappers: List<DependencyMapper>, val variant: PillExtension.Variant)