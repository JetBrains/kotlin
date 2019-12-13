/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.applySuggestedScriptConfiguration
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationCacheScope
import org.jetbrains.kotlin.idea.core.script.configuration.utils.areSimilar
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.core.script.hasSuggestedScriptConfiguration
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test
import java.io.File


class GradleKtsImportTest : GradleImportingTestCase() {
    val scriptConfigurationManager get() = ScriptConfigurationManager.getInstance(myProject)
    val projectDir get() = File(GradleSettings.getInstance(myProject).linkedProjectsSettings.first().externalProjectPath)

    override fun testDataDirName(): String {
        return "gradleKtsImportTest"
    }

    @Test
    @TargetVersions("6.0.1+")
    fun testEmpty() {
        configureByFiles()
        importProject()

        checkConfiguration("build.gradle.kts")
    }

    @Test
    @TargetVersions("6.0.1+")
    fun testCompositeBuild() {
        configureByFiles()
        importProject()

        checkConfiguration(
            "settings.gradle.kts",
            "build.gradle.kts",
            "subProject/build.gradle.kts",
            "subBuild/settings.gradle.kts",
            "subBuild/build.gradle.kts",
            "subBuild/subProject/build.gradle.kts",
            "buildSrc/settings.gradle.kts",
            "buildSrc/build.gradle.kts",
            "buildSrc/subProject/build.gradle.kts"
        )
    }

    private fun checkConfiguration(vararg files: String) {
        val scripts = files.map {
            KtsFixture(it).also { kts ->
                assertTrue(scriptConfigurationManager.hasConfiguration(kts.psiFile))
                kts.imported = scriptConfigurationManager.getConfiguration(kts.psiFile)!!
            }
        }

        // reload configuration and check this it is not changed
        scripts.forEach {
            scriptConfigurationManager.updater.postponeConfigurationReload(ScriptConfigurationCacheScope.File(it.psiFile))
            val reloadedConfiguration = scriptConfigurationManager.getConfiguration(it.psiFile)!!
            assertTrue(areSimilar(it.imported, reloadedConfiguration))
            it.assertNoSuggestedConfiguration()
        }

        // clear memory cache and check everything loaded from FS
        ScriptConfigurationManager.clearCaches(myProject)
        scripts.forEach {
            val fromFs = scriptConfigurationManager.getConfiguration(it.psiFile)!!
            assertTrue(areSimilar(it.imported, fromFs))
        }
    }

    inner class KtsFixture(val fileName: String) {
        val file = projectDir.resolve(fileName)

        val virtualFile get() = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!
        val psiFile get() = myProject.getKtFile(virtualFile)!!

        lateinit var imported: ScriptCompilationConfigurationWrapper

        fun assertSuggestedConfiguration() {
            assertTrue(virtualFile.hasSuggestedScriptConfiguration(myProject))
        }

        fun assertAndApplySuggestedConfiguration() {
            assertTrue(virtualFile.applySuggestedScriptConfiguration(myProject))
        }

        fun assertNoSuggestedConfiguration() {
            assertFalse(virtualFile.applySuggestedScriptConfiguration(myProject))
        }
    }
}