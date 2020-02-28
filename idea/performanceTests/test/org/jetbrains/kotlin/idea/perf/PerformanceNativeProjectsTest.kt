/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.ide.konan.NativeLibraryKind
import org.jetbrains.kotlin.idea.caches.project.isMPPModule
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.parseIDELibraryName
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.detectLibraryKind
import org.jetbrains.kotlin.idea.perf.Stats.Companion.WARM_UP
import org.jetbrains.kotlin.idea.perf.Stats.Companion.tcSuite
import org.jetbrains.kotlin.idea.testFramework.ProjectOpenAction.GRADLE_PROJECT
import org.jetbrains.kotlin.idea.testFramework.logMessage
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.library.KOTLIN_STDLIB_NAME
import org.jetbrains.kotlin.platform.konan.isNative
import org.junit.Ignore
import java.io.File

@Ignore(value = "[VD] disabled temporary for further investigation: it fails on TC agents")
class PerformanceNativeProjectsTest : AbstractPerformanceProjectsTest() {

    companion object {
        private const val GRADLE_VERSION = "6.0.1"
        private const val KOTLIN_PLUGIN_VERSION = "1.4.0-dev-3273" // TODO: use 1.4-M1 when it's published
        
        private var warmedUp: Boolean = false
    }

    override fun setUp() {
        super.setUp()

        // warm up: open simple small project
        if (!warmedUp) {
            val projectTemplate = "HelloWorld"
            val enableCommonizer = true

            val projectName = "${projectName(projectTemplate, enableCommonizer)}-$WARM_UP"

            // don't share this stats instance with another one used in "Hello World" test
            Stats(projectName).use { stats ->
                warmUpProject(stats, "src/iosX64Main/kotlin/HelloMain.kt") {
                    perfOpenTemplateGradleProject(stats, projectTemplate, enableCommonizer, WARM_UP)
                }
            }

            warmedUp = true
        }
    }

    fun testHelloWorldProjectWithCommonizer() = doTestHighlighting(
        "HelloWorld",
        true,
        "src/iosX64Main/kotlin/HelloMain.kt",
        "src/iosX64Main/kotlin/HelloMain2.kt",
        "src/iosX64Main/kotlin/HelloMain3.kt",
        "src/iosX64Test/kotlin/HelloTest.kt",
        "src/iosX64Test/kotlin/HelloTest2.kt",
        "src/iosX64Test/kotlin/HelloTest3.kt"
    )

    fun testHelloWorldProjectWithoutCommonizer() = doTestHighlighting(
        "HelloWorld",
        false,
        "src/iosX64Main/kotlin/HelloMain.kt",
        "src/iosX64Main/kotlin/HelloMain2.kt",
        "src/iosX64Main/kotlin/HelloMain3.kt",
        "src/iosX64Test/kotlin/HelloTest.kt",
        "src/iosX64Test/kotlin/HelloTest2.kt",
        "src/iosX64Test/kotlin/HelloTest3.kt"
    )

    fun testCvsParserProjectWithCommonizer() = doTestHighlighting(
        "CsvParser",
        true,
        "src/iosX64Main/kotlin/CsvParser.kt",
        "src/iosX64Main/kotlin/CsvParser2.kt",
        "src/iosX64Main/kotlin/CsvParser3.kt"
    )

    fun testCvsParserProjectWithoutCommonizer() = doTestHighlighting(
        "CsvParser",
        false,
        "src/iosX64Main/kotlin/CsvParser.kt",
        "src/iosX64Main/kotlin/CsvParser2.kt",
        "src/iosX64Main/kotlin/CsvParser3.kt"
    )

    fun testUIKitAppProjectWithCommonizer() = doTestHighlighting(
        "UIKitApp",
        true,
        "src/iosX64Main/kotlin/UIKitApp.kt",
        "src/iosX64Main/kotlin/UIKitApp2.kt",
        "src/iosX64Main/kotlin/UIKitApp3.kt"
    )

    fun testUIKitAppProjectWithoutCommonizer() = doTestHighlighting(
        "UIKitApp",
        false,
        "src/iosX64Main/kotlin/UIKitApp.kt",
        "src/iosX64Main/kotlin/UIKitApp2.kt",
        "src/iosX64Main/kotlin/UIKitApp3.kt"
    )

    private fun doTestHighlighting(
        templateName: String,
        enableCommonizer: Boolean,
        vararg filesToHighlight: String
    ) {
        assertTrue(filesToHighlight.isNotEmpty())

        val projectName = projectName(templateName, enableCommonizer)
        tcSuite(projectName) {
            Stats(projectName).use { stats ->
                myProject = perfOpenTemplateGradleProject(stats, templateName, enableCommonizer)

                // highlight
                filesToHighlight.forEach { perfHighlightFile(it, stats) }
            }
        }
    }

    private fun perfOpenTemplateGradleProject(
        stats: Stats,
        templateName: String,
        enableCommonizer: Boolean,
        note: String = ""
    ): Project {
        val templateRoot = File("idea/testData/perfTest/native/").resolve(templateName)
        val projectRoot = FileUtil.createTempDirectory("project", "", false)

        templateRoot.walkTopDown()
            .filter { it.isFile }
            .forEach { source ->
                val destination = projectRoot.resolve(source.relativeTo(templateRoot))

                val filename = source.name
                if (filename == "build.gradle.kts" || filename == "gradle-wrapper.properties" || filename == "gradle.properties") {
                    val text = source.readText()
                        .replace("{{kotlin_plugin_version}}", KOTLIN_PLUGIN_VERSION)
                        .replace("{{gradle_version}}", GRADLE_VERSION)
                        .replace("{{disable_commonizer}}", (!enableCommonizer).toString())

                    destination.parentFile.mkdirs()
                    destination.writeText(text)
                } else {
                    source.copyTo(destination)
                }
            }

        val projectName = projectName(templateName, enableCommonizer, separator = '-')

        val project = perfOpenProject(
            name = projectName,
            stats = stats,
            note = note,
            path = projectRoot.absolutePath,
            openAction = GRADLE_PROJECT
        )
        runProjectSanityChecks(project)

        return project
    }

    // goal: make sure that the project imported from Gradle is valid
    private fun runProjectSanityChecks(project: Project) {

        val nativeModules: Map<Module, Set<String>> = runReadAction {
            project.allModules().mapNotNull { module ->
                val facetSettings = KotlinFacet.get(module)?.configuration?.settings ?: return@mapNotNull null
                if (!facetSettings.isMPPModule || !facetSettings.targetPlatform.isNative()) return@mapNotNull null

                // workaround to skip top-level common test module, which in fact does not get any Kotlin/Native KLIB libraries
                // but accidentally gets Kotlin facet with Native platform
                if (module.name == "${project.name}.$COMMON_TEST_SOURCE_SET_NAME") return@mapNotNull null

                val nativeLibraries = module.rootManager.orderEntries
                    .asSequence()
                    .filterIsInstance<LibraryOrderEntry>()
                    .mapNotNull { it.library }
                    .filter { library ->
                        val libraryKind = detectLibraryKind(library.getFiles(OrderRootType.CLASSES))
                        libraryKind == NativeLibraryKind
                                // TODO: remove this check for CommonLibraryKind when detection of K/N KLIBs in
                                //  org.jetbrains.kotlin.ide.konan.KotlinNativePluginUtilKt.isKonanLibraryRoot
                                //  is correctly implemented
                                || libraryKind == CommonLibraryKind
                    }
                    .mapNotNull inner@{ library ->
                        val libraryNameParts = parseIDELibraryName(library.name.orEmpty()) ?: return@inner null
                        val (_, pureLibraryName, platformPart) = libraryNameParts
                        pureLibraryName + if (platformPart != null) " [$platformPart]" else ""
                    }
                    .toSet()

                module to nativeLibraries
            }.toMap()
        }

        assertTrue("No Native modules found in project $project", nativeModules.isNotEmpty())

        nativeModules.forEach { (module, nativeLibraries) ->
            assertTrue(
                "$KOTLIN_STDLIB_NAME not found for for Native module $module in project $project",
                KOTLIN_STDLIB_NAME in nativeLibraries
            )

            assertTrue(
                "No Native libraries except for $KOTLIN_STDLIB_NAME for Native module $module in project $project",
                (nativeLibraries - KOTLIN_STDLIB_NAME).isNotEmpty()
            )

            logMessage { "$module has ${nativeLibraries.size} native libraries" }
        }
    }

    private fun projectName(templateName: String, enableCommonizer: Boolean, separator: Char = ' ') =
        "$templateName project ${if (enableCommonizer) "with" else "without"} commonizer".replace(' ', separator)
}
