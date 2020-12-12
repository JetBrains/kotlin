/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.vfs.LocalFileSystem
import junit.framework.AssertionFailedError
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.applySuggestedScriptConfiguration
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.script.configuration.utils.areSimilar
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.core.script.hasSuggestedScriptConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.test.JUnitParameterizedWithIdeaConfigurationRunner
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(value = JUnitParameterizedWithIdeaConfigurationRunner::class)
class GradleKtsImportTest : GradleImportingTestCase() {
    companion object {
        @JvmStatic
        @Parameters(name = "{index}: with Gradle-{0}")
        fun data(): Collection<Array<Any?>> = listOf(arrayOf<Any?>("6.0.1"))
    }

    val scriptConfigurationManager get() = ScriptConfigurationManager.getInstance(myProject) as CompositeScriptConfigurationManager
    val projectDir get() = File(GradleSettings.getInstance(myProject).linkedProjectsSettings.first().externalProjectPath)

    override fun testDataDirName(): String {
        return "gradleKtsImportTest"
    }

    override fun importProject() {
        importProject(true)
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
    fun testError() {
        configureByFiles()

        val result = try {
            importProject()
        } catch (e: AssertionFailedError) {
            e
        }

        assert(result is AssertionFailedError) { "Exception should be thrown" }
        assert((result as AssertionFailedError).message?.contains(KotlinIdeaGradleBundle.message("title.kotlin.build.script")) == true)
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
                assertTrue("Configuration for ${kts.file.path} is missing", scriptConfigurationManager.hasConfiguration(kts.psiFile))
                kts.imported = scriptConfigurationManager.getConfiguration(kts.psiFile)!!
            }
        }

        // reload configuration and check this it is not changed
        scripts.forEach {
            val reloadedConfiguration = scriptConfigurationManager.default.runLoader(
                it.psiFile,
                object : DefaultScriptConfigurationLoader(it.psiFile.project) {
                    override fun shouldRunInBackground(scriptDefinition: ScriptDefinition) = false
                    override fun loadDependencies(
                        isFirstLoad: Boolean,
                        ktFile: KtFile,
                        scriptDefinition: ScriptDefinition,
                        context: ScriptConfigurationLoadingContext
                    ): Boolean {
                        val vFile = ktFile.originalFile.virtualFile
                        val result = getConfigurationThroughScriptingApi(ktFile, vFile, scriptDefinition)
                        context.saveNewConfiguration(vFile, result)
                        return true
                    }
                }
            )
            requireNotNull(reloadedConfiguration)
            // todo: script configuration can have different accessors, need investigation
            // assertTrue(areSimilar(it.imported, reloadedConfiguration))
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