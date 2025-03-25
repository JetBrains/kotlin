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
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class IrInterpreterHelpersSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private val HELPERS_PATH = "ir/interpreter/helpers"
        private val STDLIB_PATH = "stdlib"
        private val UNSIGNED_PATH = arrayOf(
            "$STDLIB_PATH/unsigned/src/kotlin",
            "$STDLIB_PATH/jvm/src/kotlin/util/UnsignedJVM.kt"
        )
        private val RUNTIME_PATHS = arrayOf(
            "$STDLIB_PATH/src/kotlin/ranges/Progressions.kt",
            "$STDLIB_PATH/src/kotlin/ranges/ProgressionIterators.kt",
            "$STDLIB_PATH/src/kotlin/internal/progressionUtil.kt",
            "$STDLIB_PATH/jvm/runtime/kotlin/TypeAliases.kt",
            "$STDLIB_PATH/jvm/runtime/kotlin/text/TypeAliases.kt",
            "$STDLIB_PATH/jvm/src/kotlin/collections/TypeAliases.kt",
            "$STDLIB_PATH/src/kotlin/text/regex/MatchResult.kt",
            "$STDLIB_PATH/src/kotlin/collections/Sequence.kt",
        )
        private val ANNOTATIONS_PATHS = arrayOf(
            "$STDLIB_PATH/src/kotlin/annotations/WasExperimental.kt",
            "$STDLIB_PATH/src/kotlin/annotations/ExperimentalStdlibApi.kt",
            "$STDLIB_PATH/src/kotlin/annotations/OptIn.kt",
            "$STDLIB_PATH/src/kotlin/internal/Annotations.kt",
            "$STDLIB_PATH/src/kotlin/experimental/inferenceMarker.kt",
            "$STDLIB_PATH/jvm/runtime/kotlin/jvm/annotations/JvmPlatformAnnotations.kt",
        )
        private val REFLECT_PATH = "$STDLIB_PATH/jvm/src/kotlin/reflect"
        private val EXCLUDES = listOf(
            "src/kotlin/UStrings.kt", "src/kotlin/UMath.kt", "src/kotlin/UNumbers.kt", "src/kotlin/reflect/TypesJVM.kt",
            "stdlib/unsigned/src/kotlin/UnsignedCommon.kt",
        ).map(::File)
    }

    override val directiveContainers: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    private fun getTestFilesForEachDirectory(vararg directories: String): List<TestFile> {
        val stdlibPath = File(this::class.java.classLoader.getResource(STDLIB_PATH)!!.toURI())

        return directories.flatMap { directory ->
            val resourceUri = this::class.java.classLoader.getResource(directory)!!.toURI()
            when (resourceUri.scheme) {
                "file" -> {
                    File(resourceUri).walkTopDown()
                        .mapNotNull { file ->
                            val canonicalPath = file.parentFile.canonicalPath
                            val relativePath = runIf(canonicalPath.startsWith(stdlibPath.canonicalPath)) {
                                canonicalPath.removePrefix(stdlibPath.canonicalPath + File.separatorChar)
                            }
                            file.takeIf { it.isFile }
                                ?.takeUnless { EXCLUDES.any { file.endsWith(it) } }
                                ?.toTestFile(relativePath)
                        }
                        .toList()
                }
                // TODO(KT-76305) add support for resources in jars
                else -> throw UnsupportedOperationException("Unsupported URI scheme: ${resourceUri.scheme}")
            }
        }
    }

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        return getTestFilesForEachDirectory(
            HELPERS_PATH,
            *UNSIGNED_PATH,
            *RUNTIME_PATHS,
            *ANNOTATIONS_PATHS,
            REFLECT_PATH
        )
    }
}
