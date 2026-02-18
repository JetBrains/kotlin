/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.utils.topologicalSort

/**
 * Computes the transitive `dependsOn` dependencies of [directDependsOnDependencies]. [computeTransitiveDependsOnDependencies] is the
 * default computation strategy to provide [KaModule.transitiveDependsOnDependencies].
 *
 * The algorithm is a depth-first search-based topological sort. `dependsOn` dependencies cannot be cyclical and thus form a DAG, which
 * allows the application of a topological sort.
 */
@KaPlatformInterface
public fun computeTransitiveDependsOnDependencies(directDependsOnDependencies: List<KaModule>): List<KaModule> =
    topologicalSort(directDependsOnDependencies) { this.directDependsOnDependencies }

@KaPlatformInterface
@OptIn(KaExperimentalApi::class)
public fun KaModule.areCompilerPluginsSupported(): Boolean =
    when (this) {
        is KaSourceModule, is KaScriptModule -> true
        else -> false
    }

@KaPlatformInterface
@OptIn(KaExperimentalApi::class)
public fun KaModule.asDebugString(indent: Int = 0): String =
    buildString {
        appendLine("$moduleDescription {")
        appendLine("contentScope: $contentScope")
        appendLine("directRegularDependencies:${System.lineSeparator()}")
        directRegularDependencies.joinTo(this, separator = System.lineSeparator()) { dep ->
            dep.asDebugString(indent + 1)
        }
        appendLine("directDependsOnDependencies:${System.lineSeparator()}")
        directDependsOnDependencies.joinTo(this, separator = System.lineSeparator()) { dep ->
            dep.asDebugString(indent + 1)
        }
        append("}")
    }.prependIndent("  ".repeat(indent))

/**
 * Used by implementations of [KaResolveExtensionProvider][org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider]
 * to store a reference of the [KaModule] for which a [VirtualFile] was generated.
 */
@KaImplementationDetail
public var VirtualFile.resolveExtensionFileModule: KaModule? by UserDataProperty(Key.create("RESOLVE_EXTENSION_FILE_MODULE"))
