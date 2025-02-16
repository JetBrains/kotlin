/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import java.io.File

data class TestModule(
    val name: String,
    val files: List<TestFile>,
    val allDependencies: List<DependencyDescription>,
    val directives: RegisteredDirectives,
    val languageVersionSettings: LanguageVersionSettings
) {
    val regularDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.RegularDependency }
    val friendDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.FriendDependency }
    val dependsOnDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.DependsOnDependency }

    override fun equals(other: Any?): Boolean =
        other is TestModule && name == other.name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String {
        return buildString {
            appendLine("Module: $name")
            appendLine("Dependencies:")
            allDependencies.forEach { appendLine("  $it") }
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

    override fun toString(): String = relativePath

    fun copy(): TestFile = TestFile(
        relativePath,
        originalContent,
        originalFile,
        startLineNumberInOriginalFile,
        isAdditional,
        directives
    )
}

val TestFile.nameWithoutExtension: String
    get() = name.substringBeforeLast(".")

/**
 * This enum represents the relation between the module and its dependency (assume that B depends on A)
 * - [RegularDependency] means that B depend on A as a regular library dependency (A is passed to classpath of B);
 * - [FriendDependency] is the same as [RegularDependency], but in addition B can access internal declarations of A (like test-main relation);
 * - [DependsOnDependency] represents the dependency between modules inside the same HMPP hierarchy.
 *   In the real compilation A and B will be compiled together, and declarations from B might actualize expect declarations from A.
 */
enum class DependencyRelation {
    RegularDependency,
    FriendDependency,
    DependsOnDependency
}

enum class DependencyKind {
    Source,
    KLib,
    Binary
}

data class DependencyDescription(
    val dependencyModule: TestModule,
    val kind: DependencyKind,
    val relation: DependencyRelation
)
