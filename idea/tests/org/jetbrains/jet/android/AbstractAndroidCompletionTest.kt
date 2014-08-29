/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.android

import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.openapi.application.PathManager
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jet.completion.util.testCompletion
import org.jetbrains.jet.plugin.project.TargetPlatform
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.plugin.actions.internal.KotlinInternalMode
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.PsiManager
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver
import com.android.SdkConstants
import com.intellij.codeInsight.CodeInsightSettings

public abstract class AbstractAndroidCompletionTest : KotlinAndroidTestCase() {
    private var kotlinInternalModeOriginalValue: Boolean = false

    override fun setUp() {

        System.setProperty(KotlinAndroidTestCaseBase.SDK_PATH_PROPERTY, PathManager.getHomePath() + "/androidSDK/")
        System.setProperty(KotlinAndroidTestCaseBase.PLATFORM_DIR_PROPERTY, "android-17")

        super.setUp()
        myFixture!!.setTestDataPath(getTestDataPath())
        (StartupManager.getInstance(getProject()) as StartupManagerImpl).runPostStartupActivities()
        VfsRootAccess.allowRootAccess(JetTestCaseBuilder.getHomeDirectory())

        kotlinInternalModeOriginalValue = KotlinInternalMode.enabled
        KotlinInternalMode.enabled = true
        setAutoCompleteSetting(false)
    }

    override fun createManifest() {
        myFixture!!.copyFileToProject("idea/testData/android/AndroidManifest.xml", SdkConstants.FN_ANDROID_MANIFEST_XML)
    }

    override fun tearDown() {
        KotlinInternalMode.enabled = kotlinInternalModeOriginalValue
        VfsRootAccess.disallowRootAccess(JetTestCaseBuilder.getHomeDirectory())

        val builtInsSources = getProject()!!.getComponent<BuiltInsReferenceResolver>(javaClass<BuiltInsReferenceResolver>())!!.getBuiltInsSources()!!
        val fileManager = (PsiManager.getInstance(getProject()!!) as PsiManagerEx).getFileManager()

        super.tearDown()
        // Restore mapping between PsiFiles and VirtualFiles dropped in FileManager.cleanupForNextTest(),
        // otherwise built-ins psi elements will become invalid in next test.
        for (source in builtInsSources) {
            val provider = source.getViewProvider()
            fileManager.setViewProvider(provider.getVirtualFile(), provider)
        }
    }
    private fun setAutoCompleteSetting(value: Boolean): Boolean {
        val settings = CodeInsightSettings.getInstance()
        val oldValue: Boolean
        if (completionType() == CompletionType.SMART) {
            oldValue = settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION
            settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = value
        }
        else {
            oldValue = settings.AUTOCOMPLETE_COMMON_PREFIX
            settings.AUTOCOMPLETE_ON_CODE_COMPLETION = value
        }
        return oldValue
    }

    private fun completionType() = CompletionType.BASIC

    fun doTest(testPath: String?) {
        myFixture!!.copyDirectoryToProject("res/", "res")
        myFixture!!.configureByFile(testPath!! + getTestName(true) + ".kt");
        val fileText = FileUtil.loadFile(File(testPath + getTestName(true) + ".kt"), true)
        testCompletion(fileText, TargetPlatform.JVM, {
            count -> myFixture!!.complete(completionType())
        })
    }


    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/android/completion/" + getTestName(true) + "/"
    }


    override fun requireRecentSdk() = true

}
