/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.mock.MockProjectEx
import com.intellij.openapi.Disposable
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.scripting.gradle.GradleKotlinScriptConfigurationInputs
import org.jetbrains.kotlin.idea.scripting.gradle.LastModifiedFiles
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel

abstract class AbstractGradleBuildRootsLocatorTest : TestCase() {
    private val scripts = mutableMapOf<String, ScriptFixture>()
    private val locator by lazy { MyRootsLocator() }

    class ScriptFixture(val introductionTs: Long, val info: GradleScriptInfo)

    lateinit var disposable: Disposable

    override fun setUp() {
        super.setUp()

        disposable = Disposable { }
    }

    override fun tearDown() {
        disposable.dispose()

        super.tearDown()
    }

    private inner class MyRootsLocator : GradleBuildRootsLocator(MockProjectEx(disposable)) {
        override fun getScriptFirstSeenTs(path: String): Long = scripts[path]?.introductionTs ?: 0
        override fun getScriptInfo(localPath: String): GradleScriptInfo? = scripts[localPath]?.info
        fun accessRoots() = roots
    }

    private fun add(root: GradleBuildRoot) {
        locator.accessRoots().add(root)
    }

    fun newImportedGradleProject(
        dir: String,
        relativeProjectRoots: List<String> = listOf(""),
        relativeScripts: List<String> = listOf("build.gradle.kts"),
        ts: Long = 0
    ) {
        val pathPrefix = "$dir/"
        val root = Imported(
            dir,
            GradleBuildRootData(
                ts,
                relativeProjectRoots.map { (pathPrefix + it).removeSuffix("/") },
                "gradleHome",
                "javaHome",
                listOf()
            ),
            LastModifiedFiles()
        )

        add(root)

        relativeScripts.forEach {
            val path = pathPrefix + it
            scripts[path] = ScriptFixture(
                0,
                GradleScriptInfo(
                    root,
                    null,
                    KotlinDslScriptModel(
                        file = path,
                        inputs = GradleKotlinScriptConfigurationInputs("", 0, dir),
                        classPath = listOf(),
                        sourcePath = listOf(),
                        imports = listOf(),
                        messages = listOf()
                    )
                )
            )
        }
    }

    fun findScriptBuildRoot(filePath: String, searchNearestLegacy: Boolean = true) =
        locator.findScriptBuildRoot(filePath, searchNearestLegacy)

    fun assertNotificationKind(filePath: String, notificationKind: GradleBuildRootsLocator.NotificationKind) {
        assertEquals(notificationKind, findScriptBuildRoot(filePath)?.notificationKind)
    }
}