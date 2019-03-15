/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationImpl
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.project.getLanguageVersionSettings
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.junit.Assert
import java.io.IOException

open class ConfigureKotlinInTempDirTest : AbstractConfigureKotlinInTempDirTest() {
    @Throws(IOException::class)
    fun testNoKotlincExistsNoSettingsRuntime10() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = true
        Assert.assertEquals(LanguageVersion.KOTLIN_1_0, module.languageVersionSettings.languageVersion)
        Assert.assertEquals(LanguageVersion.KOTLIN_1_0, myProject.getLanguageVersionSettings(null).languageVersion)
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") == null)
    }

    fun testNoKotlincExistsNoSettingsLatestRuntime() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = true
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, module.languageVersionSettings.languageVersion)
        Assert.assertEquals(LanguageVersion.LATEST_STABLE, myProject.getLanguageVersionSettings(null).languageVersion)
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") == null)
    }

    fun testKotlincExistsNoSettingsLatestRuntimeNoVersionAutoAdvance() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = true
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
        application.isSaveAllowed = true
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
        application.isSaveAllowed = true
        application.saveAll()
        val moduleFileContentAfter = String(module.moduleFile!!.contentsToByteArray())
        Assert.assertEquals(moduleFileContentBefore, moduleFileContentAfter)
    }

    fun testApiVersionWithoutLanguageVersion() {
        KotlinCommonCompilerArgumentsHolder.getInstance(myProject)
        val settings = myProject.getLanguageVersionSettings()
        Assert.assertEquals(ApiVersion.KOTLIN_1_1, settings.apiVersion)
    }

    fun testNoKotlincExistsNoSettingsLatestRuntimeNullizeEmptyStrings() {
        val application = ApplicationManager.getApplication() as ApplicationImpl
        application.isSaveAllowed = true
        Kotlin2JsCompilerArgumentsHolder.getInstance(project).update {
            sourceMapPrefix = ""
            sourceMapEmbedSources = ""
        }
        application.saveAll()
        Assert.assertTrue(project.baseDir.findFileByRelativePath(".idea/kotlinc.xml") == null)
    }
}
