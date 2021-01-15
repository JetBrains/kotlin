/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import java.io.File

data class TestModule(
    val name: String,
    val targetPlatform: TargetPlatform,
    val targetBackend: TargetBackend?,
    val frontendKind: FrontendKind<*>,
    val files: List<TestFile>,
    val dependencies: List<DependencyDescription>,
    val friends: List<DependencyDescription>,
    val directives: RegisteredDirectives,
    val languageVersionSettings: LanguageVersionSettings
) {
    override fun toString(): String {
        return buildString {
            appendLine("Module: $name")
            appendLine("targetPlatform = $targetPlatform")
            appendLine("Dependencies:")
            dependencies.forEach { appendLine("  $it") }
            appendLine("Directives:\n  $directives")
            files.forEach { appendLine(it) }
        }
    }
}

class TestFile(
    val relativePath: String,
    val originalContent: String,
    val originalFile: File,
    val startLineNumberInOriginalFile: Int, // line count starts with 0
    /*
     * isAdditional means that this file provided as addition to sources of testdata
     *   and there is no need to apply any handlers or preprocessors over it
     */
    val isAdditional: Boolean,
    val directives: RegisteredDirectives
) {
    val name: String = relativePath.split("/").last()
}

enum class DependencyRelation {
    Dependency,
    DependsOn
}

enum class DependencyKind {
    Source,
    KLib,
    Binary
}

data class DependencyDescription(
    val moduleName: String,
    val kind: DependencyKind,
    val relation: DependencyRelation
)
