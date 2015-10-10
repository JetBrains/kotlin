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

package org.jetbrains.kotlin.idea.codeInsight.generate

import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractGenerateActionTest : JetLightCodeInsightFixtureTestCase() {
    private fun setUpTestSourceRoot() {
        val module = myModule
        val model = ModuleRootManager.getInstance(module).modifiableModel
        val entry = model.contentEntries.single()
        val sourceFolderFile = entry.sourceFolderFiles.single()
        entry.removeSourceFolder(entry.sourceFolders.single())
        entry.addSourceFolder(sourceFolderFile, true)
        runWriteAction {
            model.commit()
            module.project.save()
        }
    }

    protected fun doTest(path: String) {
        setUpTestSourceRoot()

        val fileText = FileUtil.loadFile(File(path), true)

        try {
            ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

            myFixture.configureByFile(path)

            val actionClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// ACTION_CLASS: ")
            val action = Class.forName(actionClassName).newInstance() as CodeInsightAction

            val isApplicableExpected = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NOT_APPLICABLE")

            val presentation = myFixture.testAction(action)
            TestCase.assertEquals(isApplicableExpected, presentation.isEnabled)

            if (isApplicableExpected) {
                val afterFile = File(path + ".after")
                TestCase.assertTrue(afterFile.exists())
                myFixture.checkResult(FileUtil.loadFile(afterFile, true))
            }
        }
        finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
        }
    }

    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
