/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.incremental.IncrementalTestsGeneratorUtil.IcTestTypes.*
import org.jetbrains.kotlin.incremental.IncrementalTestsGeneratorUtil.incrementalJvmTestData
import org.jetbrains.kotlin.test.TargetBackend

fun main(args: Array<String>) {
    generateTestGroupSuite(args) {
        testGroup("compiler/incremental-compilation-impl/tests-gen", "jps/jps-plugin/testData") {
            testClass<AbstractIncrementalK1JvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    targetBackend = TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ".*SinceK2",
                        WITH_JAVA to "(^javaToKotlin)|(^javaToKotlinAndBack)|(^kotlinToJava)|(^packageFileAdded)|(^changeNotUsedSignature)" // KT-56681
                    )
                )
            )

            // K2
            testClass<AbstractIncrementalK2JvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ExcludePattern.forK2
                    )
                )
            )

            testClass<AbstractIncrementalK2FirICJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ExcludePattern.forK2,
                        WITH_JAVA to "^classToPackageFacade" // KT-56698
                    )
                )
            )
            testClass<AbstractIncrementalK2PsiJvmCompilerRunnerTest>(
                init = incrementalJvmTestData(
                    TargetBackend.JVM_IR,
                    folderToExcludePatternMap = mapOf(
                        PURE_KOTLIN to ExcludePattern.forK2
                    )
                )
            )

            testClass<AbstractIncrementalK1JsKlibCompilerRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest("incremental", "pureKotlin", extension = null, recursive = false, excludedPattern = "(^sealed.*)|(.*SinceK2)")
                modelForDirectoryBasedTest("incremental", "classHierarchyAffected", extension = null, recursive = false)
                model("incremental/js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibMultiModuleCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/multiModule", "common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK2JsKlibMultiModuleCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/multiModule", "common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest("incremental", "pureKotlin", extension = null, recursive = false, excludedPattern = "(^sealed.*)|(.*SinceK2)")
                modelForDirectoryBasedTest("incremental", "classHierarchyAffected", extension = null, recursive = false)
                modelForDirectoryBasedTest("incremental", "js", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("incremental", "scopeExpansion", extension = null, excludeParentDirs = true)
            }

            // TODO: https://youtrack.jetbrains.com/issue/KT-61602/JS-K2-ICL-Fix-muted-tests
            testClass<AbstractIncrementalK2JsKlibCompilerWithScopeExpansionRunnerTest> {
                // IC of sealed interfaces are not supported in JS
                modelForDirectoryBasedTest(
                    "incremental", "pureKotlin", extension = null, recursive = false,
                    // TODO: 'fileWithConstantRemoved' should be fixed in https://youtrack.jetbrains.com/issue/KT-58824
                    excludedPattern = "^(sealed.*|fileWithConstantRemoved|propertyRedeclaration|funRedeclaration|funVsConstructorOverloadConflict)"
                )
                modelForDirectoryBasedTest(
                    "incremental", "classHierarchyAffected", extension = null, recursive = false,
                    excludedPattern = "secondaryConstructorAdded"
                )
                modelForDirectoryBasedTest("incremental", "js", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalK1JsKlibCompilerRunnerWithFriendModulesDisabledTest> {
                modelForDirectoryBasedTest("incremental/js", "friendsModuleDisabled", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalMultiplatformJvmCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/mpp", "allPlatforms", extension = null, excludeParentDirs = true)
                modelForDirectoryBasedTest("incremental/mpp", "jvmOnlyK1", extension = null, excludeParentDirs = true)
            }
            testClass<AbstractIncrementalK1JsKlibMultiplatformJsCompilerRunnerTest> {
                modelForDirectoryBasedTest("incremental/mpp", "allPlatforms", extension = null, excludeParentDirs = true)
            }
            //TODO: write a proper k2 multiplatform test runner KT-63183
        }
    }
}

private object IncrementalTestsGeneratorUtil {
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
        modelForDirectoryBasedTest(
            "incremental", PURE_KOTLIN.folderName,
            extension = null,
            recursive = false,
            targetBackend = targetBackend,
            excludedPattern = buildExcludePattern(listOfNotNull(folderToExcludePatternMap?.get(PURE_KOTLIN), excludeForAllTestData))
        )
        modelForDirectoryBasedTest(
            "incremental", CLASS_HIERARCHY_AFFECTED.folderName,
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
        modelForDirectoryBasedTest(
            "incremental", INLINE_FUN_CALL_SITE.folderName,
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
        modelForDirectoryBasedTest(
            "incremental", WITH_JAVA.folderName,
            extension = null,
            excludeParentDirs = true,
            targetBackend = targetBackend,
            excludedPattern = buildExcludePattern(listOfNotNull(folderToExcludePatternMap?.get(WITH_JAVA), excludeForAllTestData))
        )
        modelForDirectoryBasedTest(
            "incremental", INCREMENTAL_JVM_COMPILER_ONLY.folderName,
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

private object ExcludePattern {
    private const val MEMBER_ALIAS = "(^removeMemberTypeAlias)|(^addMemberTypeAlias)"

    private const val ALL_EXPECT = "(^.*Expect.*)"

    val forK2 = listOf(
        ALL_EXPECT, // KT-63125 - Partially related to single-module expect-actual tests, but regexp is really wide
        MEMBER_ALIAS, // KT-55195 - Invalid for K2
    ).joinToString("|")
}
