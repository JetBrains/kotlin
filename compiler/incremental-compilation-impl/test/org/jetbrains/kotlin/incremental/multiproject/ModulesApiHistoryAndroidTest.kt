/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.daemon.common.IncrementalModuleEntry
import org.jetbrains.kotlin.daemon.common.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.util.Either
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModulesApiHistoryAndroidTest {
    @JvmField
    @Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var appRoot: File
    private lateinit var appHistory: File
    private lateinit var libRoot: File
    private lateinit var libHistory: File

    private lateinit var androidHistory: ModulesApiHistoryAndroid

    @Before
    fun setUp() {
        val projectRoot = tmpFolder.newFolder()

        appRoot = projectRoot.resolve("app")
        appHistory = appRoot.resolve("build/tmp/kotlin/app_history.bin")
        val appEntry = IncrementalModuleEntry(":app", "app", appRoot.resolve("build"), appHistory)
        appRoot.resolve("build/intermediates/classes/meta-inf/").apply {
            mkdirs();
            resolve("app.kotlin_module").createNewFile()
        }

        libRoot = projectRoot.resolve("lib")
        libHistory = libRoot.resolve("lib/build/tmp/kotlin/lib_history.bin")
        val libEntry = IncrementalModuleEntry(":lib", "lib", libRoot.resolve("build"), libHistory)
        libRoot.resolve("build/intermediates/classes/meta-inf/").apply {
            mkdirs();
            resolve("lib.kotlin_module").createNewFile()
        }

        val info = IncrementalModuleInfo(
            projectRoot = projectRoot,
            dirToModule = mapOf(appRoot to appEntry, libRoot to libEntry),
            nameToModules = mapOf("app" to setOf(appEntry), "lib" to setOf(libEntry)),
            jarToClassListFile = mapOf()
        )

        androidHistory = ModulesApiHistoryAndroid(info)
    }

    @Test
    fun testClassChangeInAppTopLevel() {
        val changed = appRoot.resolve("build/intermediates/classes/Changed.class").let {
            it.mkdirs()
            it
        }

        val changedFiles = androidHistory.historyFilesForChangedFiles(setOf(changed))
        changedFiles as Either.Success<Set<File>>
        assertEquals(setOf(appHistory), changedFiles.value)
    }

    @Test
    fun testClassChangeInAppInPackage() {
        val changed = appRoot.resolve("build/intermediates/classes/com/exampleChanged.class").let {
            it.mkdirs()
            it
        }

        val changedFiles = androidHistory.historyFilesForChangedFiles(setOf(changed))
        changedFiles as Either.Success<Set<File>>
        assertEquals(setOf(appHistory), changedFiles.value)
    }

    @Test
    fun testClassMultipleChanges() {

        val classesRoot = appRoot.resolve("build/intermediates/classes/com/example/")
        classesRoot.mkdirs()
        val changed = 1.until(10).map { classesRoot.resolve("MyClass_$it.class") }.toSet()

        val changedFiles = androidHistory.historyFilesForChangedFiles(changed)
        changedFiles as Either.Success<Set<File>>
        assertEquals(setOf(appHistory), changedFiles.value)
    }

    @Test
    fun testClassChangeInAppOutsideClasses() {
        val changed = appRoot.resolve("build/Changed.class").let {
            it.mkdirs()
            it
        }

        val changedFiles = androidHistory.historyFilesForChangedFiles(setOf(changed))
        assertTrue(changedFiles is Either.Error, "Fetching history should fail for file outside classes dir.")
    }

    @Test
    fun testClassChangeInAppAndLib() {
        val changedApp = appRoot.resolve("build/intermediates/classes/com/exampleChanged.class").let {
            it.mkdirs()
            it
        }
        val changedLib = libRoot.resolve("build/intermediates/classes/com/exampleChanged.class").let {
            it.mkdirs()
            it
        }

        val changedFiles = androidHistory.historyFilesForChangedFiles(setOf(changedApp, changedLib))
        changedFiles as Either.Success<Set<File>>
        assertEquals(setOf(appHistory, libHistory), changedFiles.value)
    }

    @Test
    fun testJarChangeInApp() {
        val jarFile = appRoot.resolve("build/intermediates/classes.jar")
        jarFile.parentFile.mkdirs()

        ZipOutputStream(jarFile.outputStream()).use {
            it.putNextEntry(ZipEntry("meta-inf/app.kotlin_module"))
            it.closeEntry()
        }

        val changedFiles = androidHistory.historyFilesForChangedFiles(setOf(jarFile))
        changedFiles as Either.Success<Set<File>>
        assertEquals(setOf(appHistory), changedFiles.value)
    }

    @Test
    fun testJarChangesInAppAndLib() {
        val appJar = appRoot.resolve("build/intermediates/classes.jar")
        appJar.parentFile.mkdirs()
        ZipOutputStream(appJar.outputStream()).use {
            it.putNextEntry(ZipEntry("meta-inf/app.kotlin_module"))
            it.closeEntry()
        }

        val libJar = libRoot.resolve("build/intermediates/classes.jar")
        libJar.parentFile.mkdirs()
        ZipOutputStream(libJar.outputStream()).use {
            it.putNextEntry(ZipEntry("META-INF/lib.kotlin_module"))
            it.closeEntry()
        }

        val changedFiles = androidHistory.historyFilesForChangedFiles(setOf(appJar, libJar))
        changedFiles as Either.Success<Set<File>>
        assertEquals(setOf(appHistory, libHistory), changedFiles.value)
    }
}
