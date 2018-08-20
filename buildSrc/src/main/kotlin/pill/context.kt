@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.artifacts.*

class DependencyMapper(
    val group: String,
    val module: String,
    vararg val configurations: String,
    val version: String? = null,
    val mapping: (ResolvedDependency) -> MappedDependency?
)

class MappedDependency(val main: PDependency?, val deferred: List<PDependency> = emptyList())

class ParserContext(val dependencyMappers: List<DependencyMapper>, val variant: PillExtension.Variant)