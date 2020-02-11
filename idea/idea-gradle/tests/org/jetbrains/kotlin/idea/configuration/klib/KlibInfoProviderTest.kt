/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import com.intellij.testFramework.PlatformTestUtil.getTestName
import junit.framework.TestCase
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.KotlinTestUtils.getHomeDirectory
import java.io.File

class KlibInfoProviderTest : TestCase() {

    fun testOldStyleKlibsFromNativeDistributionRecognized() = doTest(
        ::generateExpectedKlibsFromDistribution
    )

    fun testKlibsFromNativeDistributionWithSingleComponentRecognized() = doTest(
        ::generateExpectedKlibsFromDistribution
    )

    fun testKlibsFromNativeDistributionWithMultipleComponentsRecognized() = doTest(
        ::generateExpectedKlibsFromDistribution
    )

    fun testCommonizedKlibsFromNativeDistributionRecognized() = doTest(
        ::generateExpectedKlibsFromDistribution,
        ::generateExpectedCommonizedKlibsFromDistribution
    )

    private fun doTest(vararg expectedKlibsGenerators: (kotlinNativeHome: File) -> Map<File, KlibInfo>) {
        val kotlinNativeHome = testDataDir.resolve(getTestName(name, true)).resolve("kotlin-native-PLATFORM-VERSION")
        val sourcesDir = kotlinNativeHome.resolve(KONAN_DISTRIBUTION_SOURCES_DIR)

        val klibProvider = KlibInfoProvider(kotlinNativeHome = kotlinNativeHome)

        val potentialKlibPaths = mutableListOf<File>()
        potentialKlibPaths += externalLibsDir.children()

        with(kotlinNativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)) {
            potentialKlibPaths += resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).children()
            potentialKlibPaths += resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "macos_x64").children()

            with(resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR, "ios_arm64-ios_x64-discriminator")) {
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).children()
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "ios_arm64").children()
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "ios_x64").children()
            }

            with(resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR, "ios_arm32-ios_arm64-ios_x64-discriminator")) {
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).children()
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "ios_arm32").children()
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "ios_arm64").children()
                potentialKlibPaths += resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "ios_x64").children()
            }
        }

        val actualKlibs = potentialKlibPaths.mapNotNull { klibProvider.getKlibInfo(it) }
            .associateBy { it.path.relativeTo(kotlinNativeHome) }

        val expectedKlibsFromDistribution = mutableMapOf<File, KlibInfo>().apply {
            expectedKlibsGenerators.forEach { this += it(kotlinNativeHome) }
        }

        assertEquals(expectedKlibsFromDistribution.keys, actualKlibs.keys)
        for (klibPath in actualKlibs.keys) {
            val actualKlib = actualKlibs.getValue(klibPath)
            val expectedKlib = expectedKlibsFromDistribution.getValue(klibPath)

            assertEquals(expectedKlib::class.java, actualKlib::class.java)

            assertEquals(expectedKlib.path, actualKlib.path)
            assertEquals(expectedKlib.name, actualKlib.name)

            val actualSources = actualKlib.sourcePaths.map { it.relativeTo(sourcesDir) }.toSet()
            val expectedSources = expectedKlib.sourcePaths.map { it.relativeTo(sourcesDir) }.toSet()

            assertEquals(expectedSources, actualSources)

            if (expectedKlib is NativeDistributionKlibInfo) {
                require(actualKlib is NativeDistributionKlibInfo)

                assertEquals(expectedKlib.target, actualKlib.target)
            } else if (expectedKlib is NativeDistributionCommonizedKlibInfo) {
                require(actualKlib is NativeDistributionCommonizedKlibInfo)

                assertEquals(expectedKlib.ownTarget, actualKlib.ownTarget)
                assertEquals(expectedKlib.commonizedTargets, actualKlib.commonizedTargets)
            }
        }
    }

    private fun generateExpectedKlibsFromDistribution(kotlinNativeHome: File): Map<File, NativeDistributionKlibInfo> {
        val sourcesDir = kotlinNativeHome.resolve(KONAN_DISTRIBUTION_SOURCES_DIR)
        val basePath = kotlinNativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)

        val result = mutableListOf<NativeDistributionKlibInfo>()

        result += NativeDistributionKlibInfo(
            path = basePath.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR, KONAN_STDLIB_NAME),
            sourcePaths = listOf(
                sourcesDir.resolve("kotlin-stdlib-native-sources.zip"),
                sourcesDir.resolve("kotlin-test-anotations-common-sources.zip")
            ),
            name = KONAN_STDLIB_NAME,
            target = null
        )

        result += NativeDistributionKlibInfo(
            path = basePath.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR, "kotlinx-cli"),
            sourcePaths = listOf(
                sourcesDir.resolve("kotlinx-cli-common-sources.zip"),
                sourcesDir.resolve("kotlinx-cli-native-sources.zip")
            ),
            name = "kotlinx-cli",
            target = null
        )

        result += listOf("foo", "bar", "baz").map { name ->
            NativeDistributionKlibInfo(
                path = basePath.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "macos_x64", name),
                sourcePaths = emptyList(),
                name = name,
                target = KonanTarget.MACOS_X64
            )
        }

        return result.associateBy { it.path.relativeTo(kotlinNativeHome) }
    }

    private fun generateExpectedCommonizedKlibsFromDistribution(kotlinNativeHome: File): Map<File, NativeDistributionCommonizedKlibInfo> {
        val basePath = kotlinNativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR, KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)

        fun generateCommonizedKlibsForDir(commonizedLibsDirName: String): Map<File, NativeDistributionCommonizedKlibInfo> {
            val rawTargets = commonizedLibsDirName.split('-').dropLast(1)
            val targets = rawTargets.map {
                when (it) {
                    "ios_x64" -> KonanTarget.IOS_X64
                    "ios_arm32" -> KonanTarget.IOS_ARM32
                    "ios_arm64" -> KonanTarget.IOS_ARM64
                    else -> error("Unexpected target: $it")
                }
            }.toSet()

            val result = mutableListOf<NativeDistributionCommonizedKlibInfo>()

            with(basePath.resolve(commonizedLibsDirName)) {
                val libraryDirsToTargets: Map<File, KonanTarget?> =
                    targets.associateBy { resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, it.name) } +
                            (resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR) to null)

                libraryDirsToTargets.forEach { (libraryDir, target) ->
                    result += listOf("foo", "bar", "baz").map { name ->
                        NativeDistributionCommonizedKlibInfo(
                            path = libraryDir.resolve(name),
                            sourcePaths = emptyList(),
                            name = name,
                            ownTarget = target,
                            commonizedTargets = targets
                        )
                    }
                }
            }

            return result.associateBy { it.path.relativeTo(kotlinNativeHome) }
        }

        return generateCommonizedKlibsForDir("ios_arm64-ios_x64-discriminator") +
                generateCommonizedKlibsForDir("ios_arm32-ios_arm64-ios_x64-discriminator")
    }

    companion object {
        private val testDataDir = File(getHomeDirectory() + "/idea/testData/configuration/klib")
            .also { assertTrue("Test data directory does not exist: $it", it.isDirectory) }

        private val externalLibsDir = testDataDir.resolve("external-libs")

        private fun File.children(): List<File> = (listFiles()?.toList() ?: emptyList())

        private fun File.resolve(relative: String, next: String, vararg others: String): File {
            var temp = resolve(relative).resolve(next)
            for (other in others)
                temp = temp.resolve(other)

            return temp
        }
    }
}
