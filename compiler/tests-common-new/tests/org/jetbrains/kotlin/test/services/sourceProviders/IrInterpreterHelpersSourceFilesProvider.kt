/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class IrInterpreterHelpersSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private const val HELPERS_PATH = "./compiler/testData/ir/interpreter/helpers"
        private const val UNSIGNED_PATH = "./libraries/stdlib/unsigned/src/kotlin"
        private val RUNTIME_PATHS = arrayOf(
            "./core/builtins/src/kotlin/Progressions.kt",
            "./core/builtins/src/kotlin/ProgressionIterators.kt",
            "./core/builtins/src/kotlin/internal/progressionUtil.kt",
            "./libraries/stdlib/jvm/runtime/kotlin/TypeAliases.kt",
            "./libraries/stdlib/jvm/runtime/kotlin/text/TypeAliases.kt",
            "./libraries/stdlib/jvm/src/kotlin/collections/TypeAliases.kt",
            "./libraries/stdlib/common/src/kotlin/TextH.kt",
            "./libraries/stdlib/src/kotlin/text/regex/MatchResult.kt",
            "./libraries/stdlib/src/kotlin/collections/Sequence.kt",
        )
        private val ANNOTATIONS_PATHS = arrayOf(
            "./libraries/stdlib/src/kotlin/annotations/Experimental.kt",
            "./libraries/stdlib/src/kotlin/annotations/ExperimentalStdlibApi.kt",
            "./libraries/stdlib/src/kotlin/annotations/OptIn.kt",
            "./libraries/stdlib/src/kotlin/internal/Annotations.kt",
            "./libraries/stdlib/src/kotlin/experimental/inferenceMarker.kt",
            "./libraries/stdlib/jvm/runtime/kotlin/jvm/annotations/JvmPlatformAnnotations.kt",
        )
        private const val REFLECT_PATH = "./libraries/stdlib/jvm/src/kotlin/reflect"
        private val EXCLUDES = listOf(
            "src/kotlin/UStrings.kt", "src/kotlin/UMath.kt", "src/kotlin/UNumbers.kt", "src/kotlin/reflect/TypesJVM.kt",
            "core/builtins/src/kotlin/CompileTimeAnnotations.kt"
        )
    }

    override val directiveContainers: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    private fun getTestFilesForEachDirectory(vararg directories: String): List<TestFile> {
        return directories.flatMap { directory ->
            File(directory).walkTopDown().mapNotNull { file ->
                if (file.isDirectory) return@mapNotNull null
                if (EXCLUDES.any { file.absolutePath.endsWith(it.replace('/', File.separatorChar)) }) return@mapNotNull null

                file.toTestFile()
            }.toList()
        }
    }

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        return getTestFilesForEachDirectory(HELPERS_PATH, UNSIGNED_PATH, *RUNTIME_PATHS, *ANNOTATIONS_PATHS, REFLECT_PATH)
    }
}
