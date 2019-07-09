@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*

class DependencyMapper(val predicate: (ResolvedArtifact) -> Boolean, val mapping: (ResolvedArtifact) -> MappedDependency?) {
    companion object {
        fun forProject(path: String, mapping: (ResolvedArtifact) -> MappedDependency?): DependencyMapper {
            return DependencyMapper(
                predicate = { artifact ->
                    val identifier = artifact.id.componentIdentifier as? ProjectComponentIdentifier
                    identifier?.projectPath == path
                },
                mapping = mapping
            )
        }

        fun forModule(group: String, module: String, version: String?, mapping: (ResolvedArtifact) -> MappedDependency?): DependencyMapper {
            return DependencyMapper(
                predicate = { artifact ->
                    val identifier = artifact.id.componentIdentifier as? ModuleComponentIdentifier
                    identifier?.group == group && identifier?.module == module && (version == null || identifier?.version == version)
                },
                mapping = mapping
            )
        }
    }
}

class MappedDependency(val main: PDependency?, val deferred: List<PDependency> = emptyList())

class ParserContext(val dependencyMappers: List<DependencyMapper>, val variant: PillExtension.Variant)