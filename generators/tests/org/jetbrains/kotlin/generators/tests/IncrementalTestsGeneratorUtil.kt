/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.tests.IncrementalTestsGeneratorUtil.Companion.IcTestTypes.*
import org.jetbrains.kotlin.test.TargetBackend

class IncrementalTestsGeneratorUtil {
    companion object {
        enum class IcTestTypes(val folderName: String) {
            ALL("all"),
            PURE_KOTLIN("pureKotlin"),
            CLASS_HIERARCHY_AFFECTED("classHierarchyAffected"),
            INLINE_FUN_CALL_SITE("inlineFunCallSite"),
            WITH_JAVA("withJava"),
            INCREMENTAL_JVM_COMPILER_ONLY("incrementalJvmCompilerOnly")
        }

        private fun buildExcludePattern(excludePatterns: List<String>): String? =
            excludePatterns.joinToString("|") { "($it)" }.ifBlank { null }

        fun incrementalJvmTestData(
            targetBackend: TargetBackend,
            folderToExcludePatternMap: Map<IcTestTypes, String>? = null
        ): TestGroup.TestClass.() -> Unit = {
            val excludeForAllTestData = folderToExcludePatternMap?.get(ALL)
            model(
                "incremental/${PURE_KOTLIN.folderName}",
                extension = null,
                recursive = false,
                targetBackend = targetBackend,
                excludedPattern = buildExcludePattern(listOfNotNull(folderToExcludePatternMap?.get(PURE_KOTLIN), excludeForAllTestData))
            )
            model(
                "incremental/${CLASS_HIERARCHY_AFFECTED.folderName}",
                extension = null,
                recursive = false,
                targetBackend = targetBackend,
                excludedPattern = buildExcludePattern(
                    listOfNotNull(
                        folderToExcludePatternMap?.get(CLASS_HIERARCHY_AFFECTED),
                        excludeForAllTestData
                    )
                )
            )
            model(
                "incremental/${INLINE_FUN_CALL_SITE.folderName}",
                extension = null,
                excludeParentDirs = true,
                targetBackend = targetBackend,
                excludedPattern = buildExcludePattern(
                    listOfNotNull(
                        folderToExcludePatternMap?.get(INLINE_FUN_CALL_SITE),
                        excludeForAllTestData
                    )
                )
            )
            model(
                "incremental/${WITH_JAVA.folderName}",
                extension = null,
                excludeParentDirs = true,
                targetBackend = targetBackend,
                excludedPattern = buildExcludePattern(listOfNotNull(folderToExcludePatternMap?.get(WITH_JAVA), excludeForAllTestData))
            )
            model(
                "incremental/${INCREMENTAL_JVM_COMPILER_ONLY.folderName}",
                extension = null,
                excludeParentDirs = true,
                targetBackend = targetBackend,
                excludedPattern = buildExcludePattern(
                    listOfNotNull(
                        folderToExcludePatternMap?.get(INCREMENTAL_JVM_COMPILER_ONLY),
                        excludeForAllTestData
                    )
                )
            )
        }
    }
}