/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.test.CompilerTestDirectives
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.configureCompilerOptions
import org.jetbrains.kotlin.idea.test.rollbackCompilerOptions
import org.jetbrains.kotlin.resolve.TargetPlatform
import java.io.File

abstract class KotlinFixtureCompletionBaseTestCase : KotlinLightCodeInsightFixtureTestCase() {
    abstract fun getPlatform(): TargetPlatform

    protected open fun complete(completionType: CompletionType, invocationCount: Int): Array<LookupElement>? =
        myFixture.complete(completionType, invocationCount)

    protected abstract fun defaultCompletionType(): CompletionType
    protected open fun defaultInvocationCount(): Int = 0

    open fun doTest(testPath: String) {
        setUpFixture(testPath)

        val fileText = FileUtil.loadFile(File(testPath), true)
        val configured = configureCompilerOptions(fileText, project, module)
        try {

            assertTrue("\"<caret>\" is missing in file \"$testPath\"", fileText.contains("<caret>"))

            if (ExpectedCompletionUtils.shouldRunHighlightingBeforeCompletion(fileText)) {
                myFixture.doHighlighting()
            }

            testCompletion(
                fileText,
                getPlatform(),
                { completionType, count -> complete(completionType, count) },
                defaultCompletionType(),
                defaultInvocationCount(),
                additionalValidDirectives = CompilerTestDirectives.ALL_COMPILER_TEST_DIRECTIVES
            )
        } finally {
            if (configured) {
                rollbackCompilerOptions(project, module)
            }
            tearDownFixture()
        }
    }

    protected open fun setUpFixture(testPath: String) {
        //TODO: this is a hacky workaround for js second completion tests failing with PsiInvalidElementAccessException
        LibraryModificationTracker.getInstance(project).incModificationCount()

        myFixture.configureByFile(testPath)
    }

    protected open fun tearDownFixture() {

    }
}
