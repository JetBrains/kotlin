/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.mergeIncrementalModuleInfo
import org.junit.After
import org.junit.Before
import org.junit.Test

class IncrementalModuleInfoTest: TestWithWorkingDir()  {
    private lateinit var mainInfo: IncrementalModuleInfo
    private lateinit var includedBuildInfo: IncrementalModuleInfo

    @Before
    override fun setUp() {
        super.setUp()

        val projectRoot = workingDir.resolve("root")
        // Faked module entry
        val appEntry = IncrementalModuleEntry(":app", "app", projectRoot, projectRoot, projectRoot)
        val libEntry = IncrementalModuleEntry(":lib", "lib", projectRoot, projectRoot, projectRoot)

        mainInfo = IncrementalModuleInfo(
            projectRoot = projectRoot,
            rootProjectBuildDir = projectRoot.resolve("build"),
            dirToModule = mapOf(projectRoot.resolve("file-1") to appEntry,
                                projectRoot.resolve("file-2") to libEntry),
            nameToModules = mapOf("app" to setOf(appEntry),
                                  "lib" to setOf(libEntry)),
            jarToClassListFile = mapOf(),
            jarToModule = mapOf(),
            jarToAbiSnapshot = mapOf()
        )

        includedBuildInfo = IncrementalModuleInfo(
            projectRoot = projectRoot.resolve("included"),
            rootProjectBuildDir = projectRoot.resolve("build").resolve("included"),
            dirToModule = mapOf(projectRoot.resolve("file-1") to appEntry,
                                projectRoot.resolve("file-2") to libEntry),
            nameToModules = mapOf("app" to setOf(appEntry.copy(projectPath = ":included-build", name = ":included-build")),
                                  "lib" to setOf(libEntry.copy())),
            jarToClassListFile = mapOf(),
            jarToModule = mapOf(),
            jarToAbiSnapshot = mapOf()
        )
    }

    @Test
    fun testKeepMainInfo() {
        val merged = mergeIncrementalModuleInfo(mainInfo, includedBuildInfo)
        assertEquals(merged.projectRoot, mainInfo.projectRoot)
        assertEquals(merged.rootProjectBuildDir, mainInfo.rootProjectBuildDir)
    }

    @Test
    fun testMergeNameToModules() {
        val merged = mergeIncrementalModuleInfo(mainInfo, includedBuildInfo)
        assertEquals(merged.nameToModules.get("lib")?.size ?: -1, 1)
        assertEquals(merged.nameToModules.get("app")?.size ?: -1, 2)
    }


    @After
    override fun tearDown() {
        super.tearDown()
    }
}