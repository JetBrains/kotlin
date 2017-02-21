/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractNavigationTest : KotlinLightCodeInsightFixtureTestCase() {
    protected abstract fun getSourceAndTargetElements(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData?

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected open fun configureExtra(mainFileBaseName: String, mainFileText: String) {

    }

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val fileText = FileUtil.loadFile(mainFile, true)
        val addKotlinRuntime = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// WITH_RUNTIME") != null

        try {
            if (addKotlinRuntime) {
                ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
            ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

            myFixture.testDataPath = "${KotlinTestUtils.getHomeDirectory()}/${mainFile.parent}"

            val mainFileName = mainFile.name
            val mainFileBaseName = mainFileName.substring(0, mainFileName.indexOf('.'))
            configureExtra(mainFileBaseName, fileText)
            mainFile.parentFile
                    .listFiles { _, name ->
                        name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java") || name.endsWith(".xml"))
                    }
                    .forEach{ myFixture.configureByFile(it.name) }
            val file = myFixture.configureByFile(mainFileName)

            NavigationTestUtils.assertGotoDataMatching(editor, getSourceAndTargetElements(editor, file))
        }
        finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
            if (addKotlinRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
        }
    }
}

