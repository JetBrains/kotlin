/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.systemIndependentPath
import junit.framework.TestCase
import java.io.File
import java.util.*
import java.util.regex.Pattern

class CodeConformanceTest : TestCase() {
    companion object {
        private val JAVA_FILE_PATTERN = Pattern.compile(".+\\.java")
        private val SOURCES_FILE_PATTERN = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.js)")
        private val SOURCES_BUNCH_FILE_PATTERN = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.js)(\\.\\w+)?")
        private const val MAX_STEPS_COUNT = 100
        private val EXCLUDED_FILES_AND_DIRS = listOf(
            "build/js",
            "buildSrc",
            "compiler/fir/lightTree/testData",
            "compiler/testData/psi/kdoc",
            "compiler/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
            "compiler/util/src/org/jetbrains/kotlin/config/MavenComparableVersion.java",
            "core/reflection.jvm/src/kotlin/reflect/jvm/internal/pcollections",
            "dependencies",
            "dependencies/protobuf/protobuf-relocated/build",
            "dist",
            "idea/testData/codeInsight/renderingKDoc",
            "js/js.tests/.gradle",
            "js/js.translator/qunit/qunit.js",
            "js/js.translator/testData/node_modules",
            "libraries/kotlin.test/js/it/.gradle",
            "libraries/kotlin.test/js/it/node_modules",
            "libraries/reflect/api/src/java9/java/kotlin/reflect/jvm/internal/impl",
            "libraries/reflect/build",
            "libraries/stdlib/js-ir/.gradle",
            "libraries/stdlib/js-ir/build",
            "libraries/stdlib/js-ir-minimal-for-test/.gradle",
            "libraries/stdlib/js-ir-minimal-for-test/build",
            "libraries/stdlib/js-v1/.gradle",
            "libraries/stdlib/js-v1/build",
            "libraries/tools/binary-compatibility-validator/src/main/kotlin/org.jetbrains.kotlin.tools",
            "libraries/tools/kotlin-gradle-plugin-core/gradle_api_jar/build/tmp",
            "libraries/tools/kotlin-js-tests/src/test/web/qunit.js",
            "libraries/tools/kotlin-maven-plugin/target",
            "libraries/tools/kotlin-test-js-runner/.gradle",
            "libraries/tools/kotlin-test-js-runner/lib",
            "libraries/tools/kotlin-test-js-runner/node_modules",
            "libraries/tools/kotlin-test-nodejs-runner/.gradle",
            "libraries/tools/kotlin-test-nodejs-runner/node_modules",
            "libraries/tools/kotlinp/src",
            "out"
        ).map(::File)

        private val COPYRIGHT_EXCLUDED_FILES_AND_DIRS = listOf(
            "build",
            "buildSrc/prepare-deps/build",
            "compiler/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt",
            "dependencies",
            "dependencies/android-sdk/build",
            "dependencies/protobuf/protobuf-relocated/build",
            "dist",
            "idea/idea-jvm/src/org/jetbrains/kotlin/idea/copyright",
            "js/js.tests/.gradle",
            "js/js.translator/testData/node_modules",
            "libraries/kotlin.test/js/it/.gradle",
            "libraries/kotlin.test/js/it/node_modules",
            "libraries/stdlib/common/build",
            "libraries/stdlib/js-ir/.gradle",
            "libraries/stdlib/js-ir/build",
            "libraries/stdlib/js-ir/build/",
            "libraries/stdlib/js-ir/runtime/longjs.kt",
            "libraries/stdlib/js-ir-minimal-for-test/.gradle",
            "libraries/stdlib/js-ir-minimal-for-test/build",
            "libraries/stdlib/js-v1/.gradle",
            "libraries/stdlib/js-v1/build",
            "libraries/stdlib/js-v1/node_modules",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/build",
            "libraries/tools/kotlin-maven-plugin-test/target",
            "libraries/tools/kotlin-test-js-runner/.gradle",
            "libraries/tools/kotlin-test-js-runner/lib",
            "libraries/tools/kotlin-test-js-runner/node_modules",
            "out"
        )
    }

    fun testParserCode() {
        val pattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE)

        for (sourceFile in FileUtil.findFilesByMask(JAVA_FILE_PATTERN, File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            val matcher = pattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    private fun isCorrectExtension(filename: String, extensions: Set<String>): Boolean {
        val additionalExtensions = listOf(
            "after", "new", "before", "expected",
            "todo", "delete", "touch", "prefix", "postfix", "map",
            "fragment", "after2", "result", "log", "messages", "conflicts", "match", "imports", "txt", "xml"
        )
        val possibleAdditionalExtensions = extensions.plus(additionalExtensions)
        val fileExtensions = filename.split("\\.").drop(1)
        if (fileExtensions.size < 2) {
            return true
        }
        val extension = fileExtensions.last()

        return !(extension !in possibleAdditionalExtensions && extension.toIntOrNull() ?: MAX_STEPS_COUNT >= MAX_STEPS_COUNT)
    }

    fun testForgottenBunchDirectivesAndFiles() {
        val root = File("").absoluteFile
        val extensions = File(root, ".bunch").readLines().map { it.split("_") }.flatten().toSet()
        val failBuilder = mutableListOf<String>()
        for (sourceFile in FileUtil.findFilesByMask(SOURCES_BUNCH_FILE_PATTERN, root)) {
            if (EXCLUDED_FILES_AND_DIRS.any { FileUtil.isAncestor(it, sourceFile, false) }) continue

            val matches = Regex("BUNCH (\\w+)").findAll(sourceFile.readText())
                .map { it.groupValues[1] }.toSet().filterNot { it in extensions }
            for (bunch in matches) {
                val filename = FileUtil.toSystemIndependentName(sourceFile.absoluteFile.toRelativeString(root))
                failBuilder.add("$filename has unregistered $bunch bunch directive")
            }

            if (!isCorrectExtension(sourceFile.name, extensions)) {
                val filename = FileUtil.toSystemIndependentName(sourceFile.absoluteFile.toRelativeString(root))
                failBuilder.add("$filename has unknown bunch extension")
            }
        }
        if (failBuilder.isNotEmpty()) {
            fail("\n" + failBuilder.joinToString("\n"))
        }
    }

    fun testNoBadSubstringsInProjectCode() {
        class TestData(val message: String, val filter: (String) -> Boolean) {
            val result: MutableList<File> = ArrayList()
        }

        val atAuthorPattern = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL)

        val tests = listOf(
            TestData(
                "%d source files contain @author javadoc tag.\nPlease remove them or exclude in this test:\n%s",
                { source ->
                    // substring check is an optimization
                    "@author" in source && atAuthorPattern.matcher(source).find() &&
                            "ASM: a very small and fast Java bytecode manipulation framework" !in source &&
                            "package org.jetbrains.kotlin.tools.projectWizard.settings.version.maven" !in source
                }
            ),
            TestData(
                "%d source files use something from com.beust.jcommander.internal package.\n" +
                        "This code won't work when there's no TestNG in the classpath of our IDEA plugin, " +
                        "because there's only an optional dependency on testng.jar.\n" +
                        "Most probably you meant to use Guava's Lists, Maps or Sets instead. " +
                        "Please change references in these files to com.google.common.collect:\n%s",
                { source ->
                    "com.beust.jcommander.internal" in source
                }
            ),
            TestData(
                "%d source files contain references to package org.jetbrains.jet.\n" +
                        "Package org.jetbrains.jet is deprecated now in favor of org.jetbrains.kotlin. " +
                        "Please consider changing the package in these files:\n%s",
                { source ->
                    "org.jetbrains.jet" in source
                }
            ),
            TestData(
                "%d source files contain references to package kotlin.reflect.jvm.internal.impl.\n" +
                        "This package contains internal reflection implementation and is a result of a " +
                        "post-processing of kotlin-reflect.jar by jarjar.\n" +
                        "Most probably you meant to use classes from org.jetbrains.kotlin.**.\n" +
                        "Please change references in these files or exclude them in this test:\n%s",
                { source ->
                    "kotlin.reflect.jvm.internal.impl" in source
                }
            ),
            TestData(
                "%d source files contain references to package org.objectweb.asm.\n" +
                        "Package org.jetbrains.org.objectweb.asm should be used instead to avoid troubles with different asm versions in classpath. " +
                        "Please consider changing the package in these files:\n%s",
                { source ->
                    " org.objectweb.asm" in source
                })
        )

        for (sourceFile in FileUtil.findFilesByMask(SOURCES_FILE_PATTERN, File("."))) {
            if (EXCLUDED_FILES_AND_DIRS.any { FileUtil.isAncestor(it, sourceFile, false) }) continue

            val source = sourceFile.readText()
            for (test in tests) {
                if (test.filter(source)) test.result.add(sourceFile)
            }
        }

        if (tests.flatMap { it.result }.isNotEmpty()) {
            fail(buildString {
                for (test in tests) {
                    if (test.result.isNotEmpty()) {
                        append(test.message.format(test.result.size, test.result.joinToString("\n")))
                        appendLine()
                        appendLine()
                    }
                }
            })
        }
    }

    fun testThirdPartyCopyrights() {
        val filesWithUnlistedCopyrights = mutableListOf<String>()
        val root = File(".").absoluteFile
        val knownThirdPartyCode = loadKnownThirdPartyCodeList()
        val copyrightRegex = Regex("""\bCopyright\b""")
        for (sourceFile in FileUtil.findFilesByMask(SOURCES_FILE_PATTERN, root)) {
            val relativePath = FileUtil.toSystemIndependentName(sourceFile.toRelativeString(root))
            if (COPYRIGHT_EXCLUDED_FILES_AND_DIRS.any { relativePath.startsWith(it) } ||
                knownThirdPartyCode.any { relativePath.startsWith(it) }) continue

            sourceFile.useLines { lineSequence ->
                for (line in lineSequence) {
                    if (copyrightRegex in line && "JetBrains" !in line) {
                        filesWithUnlistedCopyrights.add("$relativePath: $line")
                    }
                }
            }
        }
        if (filesWithUnlistedCopyrights.isNotEmpty()) {
            fail(
                "The following files contain third-party copyrights and no license information. " +
                        "Please update license/README.md accordingly:\n${filesWithUnlistedCopyrights.joinToString("\n")}"
            )
        }
    }

    fun testTemporaryRepositoriesAbuse() {
        val extensions = setOf("java", "kt", "gradle", "kts")
        val repositories = setOf(
            "https://dl.bintray.com/kotlin/kotlin-dev"
        )
        val allowList = setOf(
            "libraries/tools/new-project-wizard/new-project-wizard-cli/testData",
            "gradle/cacheRedirector.gradle.kts",
            "kotlin-ultimate/prepare/mobile-plugin/build.gradle.kts",
            "kotlin-ultimate/gradle/cidrPluginTools.gradle.kts",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/build/resources/test/testProject/new-mpp-fat-framework/smoke/build.gradle.kts",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/build/resources/test/testProject/kotlin2JsProjectWithSourceMapInline/build.gradle",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/build/resources/test/testProject/new-mpp-android/build.gradle",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/new-mpp-fat-framework/smoke/build.gradle.kts",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/kotlin2JsProjectWithSourceMapInline/build.gradle",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/testProject/new-mpp-android/build.gradle",
            "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/VariantAwareDependenciesIT.kt",
            "libraries/tools/new-project-wizard/src/org/jetbrains/kotlin/tools/projectWizard/core/service/KotlinVersionProviderService.kt",
            "idea/testData/perfTest/native/_common/settings.gradle.kts",
            "idea/testData/gradle/nativeLibraries/commonIOSWithDisabledPropagation/settings.gradle.kts",
            "idea/testData/gradle/packagePrefixImport/packagePrefixNonMPP/build.gradle",
            "idea/testData/gradle/gradleFacetImportTest/jvmImportWithCustomSourceSets_1_1_2/build.gradle",
            "idea/testData/gradle/gradleFacetImportTest/jvmImport_1_1_2/build.gradle",
            "idea/idea-gradle/tests/org/jetbrains/kotlin/idea/codeInsight/gradle/MultiplePluginVersionGradleImportingTestCase.kt"
        ).map(::File)
        val fullIgnoreList = EXCLUDED_FILES_AND_DIRS + allowList
        val excludeFileNames = fullIgnoreList.filter { it.isFile }.map { it.name }.toSet()
        val excludedDirNames = fullIgnoreList.filter { it.isDirectory }.map { it.name }.toSet()
        val excludedPaths = fullIgnoreList.map { it.systemIndependentPath }.toSet()
        val root = File(".")
        val filesWithRepositories = root.walkTopDown()
            .onEnter { dir ->
                !(dir.name in excludedDirNames && dir.relativeTo(root).systemIndependentPath in excludedPaths)
            }
            .filter { file -> file.extension in extensions && file.isFile }
            .filter { file -> !(file.name in excludeFileNames && file.isFile && file.relativeTo(root).systemIndependentPath in excludedPaths) }
            .filter { file ->
                file.useLines { lines ->
                    lines.any { line ->
                        repositories.any { repository -> line.contains(repository) }
                    }
                }
            }
            .toList()

        if (filesWithRepositories.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("The following files use temporal repositories and not listed in the allowed list:")
                    filesWithRepositories.forEach { appendLine("  ${it.relativeTo(root).systemIndependentPath}") }
                    appendLine("List of monitored repositories:")
                    repositories.forEach { appendLine("  $it") }
                }
            )
        }
    }

    private fun loadKnownThirdPartyCodeList(): List<String> {
        File("license/README.md").useLines { lineSequence ->
            return lineSequence
                .filter { it.startsWith(" - Path: ") }
                .map { it.removePrefix(" - Path: ").trim().ensureFileOrEndsWithSlash() }
                .toList()

        }
    }
}

private fun String.ensureFileOrEndsWithSlash() =
    if (endsWith("/") || "." in substringAfterLast('/')) this else this + "/"
