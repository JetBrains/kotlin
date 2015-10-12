/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testIntegration.GotoTestOrCodeHandler
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

abstract class AbstractGotoTestOrCodeActionTest : JetLightCodeInsightFixtureTestCase() {
    private object Handler: GotoTestOrCodeHandler() {
        public override fun getSourceAndTargetElements(editor: Editor?, file: PsiFile?) = super.getSourceAndTargetElements(editor, file)
    }

    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val fileText = FileUtil.loadFile(mainFile, true)
        val addKotlinRuntime = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// WITH_RUNTIME") != null

        try {
            if (addKotlinRuntime) {
                ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
            ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

            myFixture.testDataPath = "${JetTestUtils.getHomeDirectory()}/${mainFile.getParent()}"

            val mainFileName = mainFile.name
            val mainFileBaseName = mainFileName.substring(0, mainFileName.indexOf('.'))
            mainFile.parentFile
                    .listFiles { file, name ->
                        name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java"))
                    }
                    .forEach{ myFixture.configureByFile(it.name) }
            val file = myFixture.configureByFile(mainFileName)

            NavigationTestUtils.assertGotoDataMatching(editor, Handler.getSourceAndTargetElements(editor, file))
        }
        finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
            if (addKotlinRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
        }
    }
}
