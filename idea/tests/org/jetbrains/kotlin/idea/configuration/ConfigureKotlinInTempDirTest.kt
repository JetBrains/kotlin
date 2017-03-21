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
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.junit.Assert
import java.io.File
import java.io.IOException

class ConfigureKotlinInTempDirTest : AbstractConfigureKotlinTest() {
    @Throws(IOException::class)
    override fun getIprFile(): File {
        val tempDir = FileUtil.generateRandomTemporaryPath()
        FileUtil.createTempDirectory("temp", null)

        FileUtil.copyDir(File(projectRoot), tempDir)

        val projectRoot = tempDir.path

        val projectFilePath = projectRoot + "/projectFile.ipr"
        if (!File(projectFilePath).exists()) {
            val dotIdeaPath = projectRoot + "/.idea"
            Assert.assertTrue("Project file or '.idea' dir should exists in " + projectRoot, File(dotIdeaPath).exists())
            return File(projectRoot)
        }
        return File(projectFilePath)
    }

    @Throws(IOException::class)
    fun testKotlincExistsNoSettingsRuntime10() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.doNotSave(false)
        Assert.assertEquals(LanguageVersion.KOTLIN_1_0, myProject.getLanguageVersionSettings(null).languageVersion)
        Assert.assertEquals(LanguageVersion.KOTLIN_1_0, module.languageVersionSettings.languageVersion)
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") != null)
    }

    fun testKotlincExistsNoSettingsRuntime11() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.doNotSave(false)
        Assert.assertEquals(LanguageVersion.KOTLIN_1_1, myProject.getLanguageVersionSettings(null).languageVersion)
        Assert.assertEquals(LanguageVersion.KOTLIN_1_1, module.languageVersionSettings.languageVersion)
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
}
