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
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.util.io.FileUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestActionEvent
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.targetPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractCodeInsightActionTest : KotlinLightCodeInsightFixtureTestCase() {
    protected open fun createAction(fileText: String): CodeInsightAction {
        val actionClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// ACTION_CLASS: ")
        return Class.forName(actionClassName).newInstance() as CodeInsightAction
    }

    protected open fun configure(mainFilePath: String, mainFileText: String) {
        myFixture.configureByFile(mainFilePath) as KtFile
    }

    protected open fun checkExtra() {

    }

    protected open fun testAction(action: AnAction, forced: Boolean): Presentation {
        val e = TestActionEvent(action)
        action.beforeActionPerformedUpdate(e)
        if (forced || (e.presentation.isEnabled && e.presentation.isVisible)) {
            action.actionPerformed(e)
        }
        return e.presentation
    }

    protected open fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)

        val conflictFile = File("$path.messages")
        val afterFile = File("$path.after")

        var mainPsiFile: KtFile? = null

        try {
            ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

            val mainFile = File(path)
            val mainFileName = mainFile.name
            val fileNameBase = mainFile.nameWithoutExtension + "."
            val rootDir = mainFile.parentFile
            rootDir
                    .list { _, name ->
                        name.startsWith(fileNameBase) && name != mainFileName && (name.endsWith(".kt") || name.endsWith(".java"))
                    }
                    .forEach {
                        myFixture.configureByFile(File(rootDir, it).path.replace(File.separator, "/"))
                    }

            configure(path, fileText)
            mainPsiFile = myFixture.file as KtFile

            val targetPlatformName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// PLATFORM: ")
            if (targetPlatformName != null) {
                val targetPlatform = when (targetPlatformName) {
                    "JVM" -> JvmPlatform
                    "JavaScript" -> JsPlatform
                    "Common" -> TargetPlatform.Common
                    else -> error("Unexpected platform name: $targetPlatformName")
                }
                mainPsiFile.targetPlatform = targetPlatform
            }

            val action = createAction(fileText)

            val isApplicableExpected = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NOT_APPLICABLE")
            val isForced = InTextDirectivesUtils.isDirectiveDefined(fileText, "// FORCED")

            val presentation = testAction(action, isForced)
            if (!isForced) {
                TestCase.assertEquals(isApplicableExpected, presentation.isEnabled)
            }

            assert(!conflictFile.exists()) { "Conflict file $conflictFile should not exist" }

            if (isForced || isApplicableExpected) {
                TestCase.assertTrue(afterFile.exists())
                myFixture.checkResult(FileUtil.loadFile(afterFile, true))
                checkExtra()
            }
        }
        catch (e: ComparisonFailure) {
            KotlinTestUtils.assertEqualsToFile(afterFile, myFixture.editor)
        }
        catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
            KotlinTestUtils.assertEqualsToFile(conflictFile, e.message!!)
        }
        finally {
            mainPsiFile?.targetPlatform = null
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
