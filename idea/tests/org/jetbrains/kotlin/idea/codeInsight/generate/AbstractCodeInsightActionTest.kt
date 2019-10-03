/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.project.forcedTargetPlatform
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
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
        val fileText = FileUtil.loadFile(testDataFile(), true)

        val conflictFile = File("$path.messages")
        val afterFile = File("$path.after")

        var mainPsiFile: KtFile? = null

        try {
            ConfigLibraryUtil.configureLibrariesByDirective(module, PlatformTestUtil.getCommunityPath(), fileText)

            val mainFile = testDataFile()
            val mainFileName = mainFile.name
            val fileNameBase = mainFile.nameWithoutExtension + "."
            val rootDir = mainFile.parentFile
            rootDir
                .list { _, name ->
                    name.startsWith(fileNameBase) && name != mainFileName && (name.endsWith(".kt") || name.endsWith(".java"))
                }
                .forEach {
                    myFixture.configureByFile(it)
                }

            configure(fileName(), fileText)
            mainPsiFile = myFixture.file as KtFile

            val targetPlatformName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// PLATFORM: ")
            if (targetPlatformName != null) {
                val targetPlatform = when (targetPlatformName) {
                    "JVM" -> JvmPlatforms.unspecifiedJvmPlatform
                    "JavaScript" -> JsPlatforms.defaultJsPlatform
                    "Common" -> CommonPlatforms.defaultCommonPlatform
                    else -> error("Unexpected platform name: $targetPlatformName")
                }
                mainPsiFile.forcedTargetPlatform = targetPlatform
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
            mainPsiFile?.forcedTargetPlatform = null
            ConfigLibraryUtil.unconfigureLibrariesByDirective(module, fileText)
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
