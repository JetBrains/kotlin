/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import com.intellij.testFramework.PlatformTestUtil.getTestName
import junit.framework.TestCase
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.KotlinTestUtils.getHomeDirectory
import java.io.File

class KlibInfoProviderTest : TestCase() {

    fun testOldStyleKlibsFromNativeDistributionRecognized() = doTest()

    fun testKlibsFromNativeDistributionWithSingleComponentRecognized() = doTest()

    fun testKlibsFromNativeDistributionWithMultipleComponentsRecognized() = doTest()

    private fun doTest() {
        val kotlinNativeHome = testDataDir.resolve(getTestName(name, true)).resolve("kotlin-native-PLATFORM-VERSION")
        val sourcesDir = kotlinNativeHome.resolve("sources")

        val klibProvider = KlibInfoProvider(kotlinNativeHome = kotlinNativeHome)

        val potentialKlibPaths = mutableListOf<File>()
        potentialKlibPaths += externalLibsDir.children()
        potentialKlibPaths += kotlinNativeHome.resolve("klib", "common").children()
        potentialKlibPaths += kotlinNativeHome.resolve("klib", "platform", "macos_x64").children()

        val actualKlibs = potentialKlibPaths
            .mapNotNull { klibProvider.getKlibInfo(it) as? NativeDistributionKlibInfo }
            .associateBy { it.path.relativeTo(kotlinNativeHome) }

        val expectedKlibsFromDistribution = generateExpectedKlibsFromDistribution(
            kotlinNativeHome = kotlinNativeHome,
            sourcesDir = sourcesDir
        )

        assertEquals(expectedKlibsFromDistribution.keys, actualKlibs.keys)
        for (klibPath in actualKlibs.keys) {
            val actualKlib = actualKlibs.getValue(klibPath)
            val expectedKlib = expectedKlibsFromDistribution.getValue(klibPath)

            assertEquals(expectedKlib.path, actualKlib.path)
            assertEquals(expectedKlib.name, actualKlib.name)

            val actualSources = actualKlib.sourcePaths.map { it.relativeTo(sourcesDir) }.toSet()
            val expectedSources = expectedKlib.sourcePaths.map { it.relativeTo(sourcesDir) }.toSet()

            assertEquals(expectedSources, actualSources)
            assertEquals(expectedKlib.target, actualKlib.target)
        }
    }

    private fun generateExpectedKlibsFromDistribution(kotlinNativeHome: File, sourcesDir: File) = listOf(
        NativeDistributionKlibInfo(
            path = kotlinNativeHome.resolve("klib", "common", "stdlib"),
            sourcePaths = listOf(
                sourcesDir.resolve("kotlin-stdlib-native-sources.zip"),
                sourcesDir.resolve("kotlin-test-anotations-common-sources.zip")
            ),
            name = "stdlib",
            target = null
        ),
        NativeDistributionKlibInfo(
            path = kotlinNativeHome.resolve("klib", "common", "kotlinx-cli"),
            sourcePaths = listOf(
                sourcesDir.resolve("kotlinx-cli-common-sources.zip"),
                sourcesDir.resolve("kotlinx-cli-native-sources.zip")
            ),
            name = "kotlinx-cli",
            target = null
        ),
        NativeDistributionKlibInfo(
            path = kotlinNativeHome.resolve("klib", "platform", "macos_x64", "foo"),
            sourcePaths = emptyList(),
            name = "foo",
            target = KonanTarget.MACOS_X64
        ),
        NativeDistributionKlibInfo(
            path = kotlinNativeHome.resolve("klib", "platform", "macos_x64", "bar"),
            sourcePaths = emptyList(),
            name = "bar",
            target = KonanTarget.MACOS_X64
        ),
        NativeDistributionKlibInfo(
            path = kotlinNativeHome.resolve("klib", "platform", "macos_x64", "baz"),
            sourcePaths = emptyList(),
            name = "baz",
            target = KonanTarget.MACOS_X64
        )
    ).associateBy { it.path.relativeTo(kotlinNativeHome) }

    companion object {
        private val testDataDir = File(getHomeDirectory() + "/idea/testData/configuration/klib")
            .also { assertTrue("Test data directory does not exist: $it", it.isDirectory) }

        private val externalLibsDir = testDataDir.resolve("external-libs")

        private fun File.children(): List<File> = (listFiles()?.toList() ?: emptyList())
            .also { assertTrue("$this does not have children files or directories", it.isNotEmpty()) }

        private fun File.resolve(relative: String, next: String, vararg others: String): File {
            var temp = resolve(relative).resolve(next)
            for (other in others)
                temp = temp.resolve(other)

            return temp
        }
    }
}
