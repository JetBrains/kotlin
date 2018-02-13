/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.junit.Assert
import java.io.File
import java.io.IOException
import java.nio.file.Path

open class ConfigureKotlinInTempDirTest : AbstractConfigureKotlinTest() {
    override fun getProjectDirOrFile(): Path {
        val tempDir = FileUtil.generateRandomTemporaryPath()
        FileUtil.createTempDirectory("temp", null)
        myFilesToDelete.add(tempDir)

        FileUtil.copyDir(File(projectRoot), tempDir)

        val projectRoot = tempDir.path

        val projectFilePath = projectRoot + "/projectFile.ipr"
        if (!File(projectFilePath).exists()) {
            val dotIdeaPath = projectRoot + "/.idea"
            Assert.assertTrue("Project file or '.idea' dir should exists in " + projectRoot, File(dotIdeaPath).exists())
            return File(projectRoot).toPath()
        }

        return File(projectFilePath).toPath()
    }

    @Throws(IOException::class)
    fun testNoKotlincExistsNoSettingsRuntime10() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = false
        Assert.assertEquals(LanguageVersion.KOTLIN_1_0, module.languageVersionSettings.languageVersion)
        Assert.assertEquals(LanguageVersion.KOTLIN_1_0, myProject.getLanguageVersionSettings(null).languageVersion)
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") == null)
    }

    fun testNoKotlincExistsNoSettingsLatestRuntime() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = false
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, module.languageVersionSettings.languageVersion)
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, myProject.getLanguageVersionSettings(null).languageVersion)
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") == null)
    }

    fun testKotlincExistsNoSettingsLatestRuntimeNoVersionAutoAdvance() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = false
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, module.languageVersionSettings.languageVersion)
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, myProject.getLanguageVersionSettings(null).languageVersion)
        KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
            autoAdvanceLanguageVersion = false
            autoAdvanceApiVersion = false
        }
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") != null)
    }

    fun testDropKotlincOnVersionAutoAdvance() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = false
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, module.languageVersionSettings.languageVersion)
        KotlinCommonCompilerArgumentsHolder.getInstance(project).update {
            autoAdvanceLanguageVersion = true
            autoAdvanceApiVersion = true
        }
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") == null)
    }

    fun testProject106InconsistentVersionInConfig() {
        val settings = KotlinFacetSettingsProvider.getInstance(myProject).getInitializedSettings(module)
        Assert.assertEquals(false, settings.useProjectSettings)
        Assert.assertEquals("1.0", settings.languageLevel!!.description)
        Assert.assertEquals("1.0", settings.apiLevel!!.description)
    }

    fun testProject107InconsistentVersionInConfig() {
        val settings = KotlinFacetSettingsProvider.getInstance(myProject).getInitializedSettings(module)
        Assert.assertEquals(false, settings.useProjectSettings)
        Assert.assertEquals("1.0", settings.languageLevel!!.description)
        Assert.assertEquals("1.0", settings.apiLevel!!.description)
    }

    fun testFacetWithProjectSettings() {
        val settings = KotlinFacetSettingsProvider.getInstance(myProject).getInitializedSettings(module)
        Assert.assertEquals(true, settings.useProjectSettings)
        Assert.assertEquals("1.1", settings.languageLevel!!.description)
        Assert.assertEquals("1.1", settings.apiLevel!!.description)
        Assert.assertEquals("-version -Xallow-kotlin-package -Xskip-metadata-version-check", settings.compilerSettings!!.additionalArguments)
    }

    fun testLoadAndSaveProjectWithV2FacetConfig() {
        val moduleFileContentBefore = String(module.moduleFile!!.contentsToByteArray())
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = false
        application.saveAll()
        val moduleFileContentAfter = String(module.moduleFile!!.contentsToByteArray())
        Assert.assertEquals(moduleFileContentBefore, moduleFileContentAfter)
    }

    fun testApiVersionWithoutLanguageVersion() {
        KotlinCommonCompilerArgumentsHolder.getInstance(myProject)
        val settings = myProject.getLanguageVersionSettings()
        Assert.assertEquals(ApiVersion.KOTLIN_1_1, settings.apiVersion)
    }

    //todo[Sedunov]: wait for fix in platform to avoid misunderstood from Java newbies (also PluginStartupComponent)
    /*fun testKotlinSdkAdded() {
        Assert.assertTrue(ProjectJdkTable.getInstance().allJdks.any { it.sdkType is KotlinSdkType })
    }*/
}
