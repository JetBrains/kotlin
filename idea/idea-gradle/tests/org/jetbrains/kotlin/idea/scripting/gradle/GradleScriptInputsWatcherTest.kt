/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationLoadingTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
open class GradleScriptInputsWatcherTest : AbstractScriptConfigurationLoadingTest() {
    companion object {
        internal const val outsidePlaceholder = "// OUTSIDE_SECTIONS"
        internal const val insidePlaceholder = "// INSIDE_SECTIONS"
    }

    private lateinit var testFiles: TestFiles

    data class TestFiles(val buildKts: KtFile, val settings: KtFile, val prop: PsiFile)

    override fun setUp() {
        super.setUp()

        // should be initialized explicitly because we do not have a real Gradle Project in this test
        project.service<GradleScriptInputsWatcher>().startWatching()
    }

    override fun setUpTestProject() {
        val rootDir = "idea/testData/script/definition/loading/gradle/"

        val settings: KtFile = addFileToProject(rootDir + GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME)
        val prop: PsiFile = addFileToProject(rootDir + "gradle.properties")

        val buildGradleKts = File(rootDir).walkTopDown().find { it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME }
            ?: error("Couldn't find main script")
        configureScriptFile(rootDir, buildGradleKts)
        val build = (myFile as? KtFile) ?: error("")

        testFiles = TestFiles(build, settings, prop)
    }

    private inline fun <reified T : Any> addFileToProject(file: String): T {
        createFileAndSyncDependencies(File(file))
        return (myFile as? T) ?: error("Couldn't configure project by $file")
    }

    fun testSectionChange() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        changeBuildKtsInsideSections()

        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    fun testSpacesInSectionsChange() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        changeBuildKtsInsideSections("// INSIDE PLUGINS\n")

        assertConfigurationUpToDate(testFiles.buildKts)
    }

    fun testCommentsInSectionsChange() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        changeBuildKtsInsideSections("// My test comment\n")

        assertConfigurationUpToDate(testFiles.buildKts)
    }

    fun testOutsideSectionChange() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        changeBuildKtsOutsideSections()

        assertConfigurationUpToDate(testFiles.buildKts)
    }

    fun testSectionsInSettingsChange() {
        assertAndLoadInitialConfiguration(testFiles.settings)

        changeSettingsKtsInsideSections()

        assertConfigurationUpdateWasDone(testFiles.settings)
    }

    fun testOutsideSectionsInSettingsChange() {
        assertAndLoadInitialConfiguration(testFiles.settings)

        changeSettingsKtsOutsideSections()

        assertConfigurationUpToDate(testFiles.settings)
    }

    fun testChangeOutsideSectionsInvalidatesOtherFiles() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        changeBuildKtsOutsideSections()

        assertConfigurationUpToDate(testFiles.buildKts)
        assertConfigurationUpdateWasDone(testFiles.settings)
    }

    fun testChangeInsideSectionsInvalidatesOtherFiles() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        changeBuildKtsInsideSections()

        assertConfigurationUpdateWasDone(testFiles.buildKts)
        assertConfigurationUpdateWasDone(testFiles.settings)
    }

    fun testChangeInsideNonKtsFileInvalidatesOtherFiles() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        changePropertiesFile()

        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    fun testTwoFilesChanged() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        changePropertiesFile()
        changeSettingsKtsOutsideSections()

        assertConfigurationUpdateWasDone(testFiles.settings)
    }

    fun testFileAttributes() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        ScriptConfigurationManager.clearCaches(project)

        assertConfigurationUpToDate(testFiles.buildKts)
    }

    fun testFileAttributesUpToDate() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        ScriptConfigurationManager.clearCaches(project)

        changeBuildKtsInsideSections()

        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    fun testFileAttributesUpToDateAfterChangeOutsideSections() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        ScriptConfigurationManager.clearCaches(project)

        changeBuildKtsOutsideSections()

        assertConfigurationUpToDate(testFiles.buildKts)
    }

    fun testFileAttributesUpdateAfterChangeOutsideSectionsOfOtherFile() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        ScriptConfigurationManager.clearCaches(project)

        changeSettingsKtsOutsideSections()

        assertConfigurationUpToDate(testFiles.settings)
        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    fun testConfigurationUpdateAfterProjectClosing() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        changeSettingsKtsOutsideSections()

        project.service<GradleScriptInputsWatcher>().clearAndRefillState()

        assertConfigurationUpToDate(testFiles.settings)
        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    fun testConfigurationUpdateAfterProjectClosing2() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        changeSettingsKtsOutsideSections()

        val ts = System.currentTimeMillis()
        markFileChanged(testFiles.buildKts.virtualFile, ts)
        markFileChanged(testFiles.settings.virtualFile, ts)

        assertConfigurationUpdateWasDone(testFiles.settings)
        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    fun testConfigurationUpdateAfterProjectClosing3() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)
        assertAndLoadInitialConfiguration(testFiles.settings)

        val ts = System.currentTimeMillis()
        markFileChanged(testFiles.buildKts.virtualFile, ts)
        markFileChanged(testFiles.settings.virtualFile, ts)

        changePropertiesFile()

        assertConfigurationUpdateWasDone(testFiles.settings)
        assertConfigurationUpdateWasDone(testFiles.buildKts)
    }

    private fun markFileChanged(virtualFile: VirtualFile, ts: Long) {
        project.service<GradleScriptInputsWatcher>().fileChanged(virtualFile.path, ts)
    }

    fun testLoadedConfigurationWhenExternalFileChanged() {
        assertAndLoadInitialConfiguration(testFiles.buildKts)

        changePropertiesFile()

        assertConfigurationUpdateWasDone(testFiles.buildKts)
        assertConfigurationUpToDate(testFiles.buildKts)
    }

    private fun assertConfigurationUpToDate(file: KtFile) {
        scriptConfigurationManager.updater.ensureUpToDatedConfigurationSuggested(file)
        assertNoBackgroundTasks()
        assertNoLoading()
    }

    private fun assertConfigurationUpdateWasDone(file: KtFile) {
        scriptConfigurationManager.updater.ensureUpToDatedConfigurationSuggested(file)
        assertAndDoAllBackgroundTasks()
        assertSingleLoading()
    }

    private fun changeBuildKtsInsideSections(text: String = "application") {
        changeBuildKts(insidePlaceholder, text)
    }

    private fun changeBuildKtsOutsideSections() {
        changeBuildKts(outsidePlaceholder, "compile(\"\")")
    }

    private fun changeSettingsKtsInsideSections() {
        changeSettingsKts(insidePlaceholder, "mavenCentral()")
    }

    private fun changeSettingsKtsOutsideSections() {
        changeSettingsKts(outsidePlaceholder, "include: 'aaa'")
    }

    private fun changeBuildKts(placeHolder: String, text: String) {
        changeContents(testFiles.buildKts.text.replace(placeHolder, text), testFiles.buildKts)
        testFiles = testFiles.copy(buildKts = myFile as KtFile)
    }

    private fun changeSettingsKts(placeHolder: String, text: String) {
        changeContents(testFiles.settings.text.replace(placeHolder, text), testFiles.settings)
        testFiles = testFiles.copy(settings = myFile as KtFile)
    }

    private fun changePropertiesFile() {
        changeContents(testFiles.prop.text.replace(outsidePlaceholder.replace("//", "#"), "myProp = true"), testFiles.prop)
        testFiles = testFiles.copy(prop = myFile)
    }
}