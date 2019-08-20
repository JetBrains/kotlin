/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases;

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.RefactoringCase
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.RandomMoveRefactoringResult
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsHandler
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.RefactoringConflictsFoundException
import org.jetbrains.kotlin.psi.KtClass

internal class MoveRefactoringCase : RefactoringCase {

    override fun tryCreateAndRun(project: Project): RandomMoveRefactoringResult {

        val projectFiles = project.files()

        if (projectFiles.isEmpty()) RandomMoveRefactoringResult.Failed

        fun getRandomKotlinClassOrNull(): KtClass? {
            val classes = PsiTreeUtil.collectElementsOfType(projectFiles.random().toPsiFile(project), KtClass::class.java)
            return if (classes.isNotEmpty()) return classes.random() else null
        }

        val targetClass: KtClass? = null
        val sourceClass = getRandomKotlinClassOrNull() ?: return RandomMoveRefactoringResult.Failed
        val sourceClassAsArray = arrayOf(sourceClass)

        val testDataKeeper = TestDataKeeper("no data")

        val handler = MoveKotlinDeclarationsHandler(MoveKotlinDeclarationsHandlerTestActions(testDataKeeper))

        if (!handler.canMove(sourceClassAsArray, targetClass)) {
            return RandomMoveRefactoringResult.Failed
        }

        try {
            handler.doMove(project, sourceClassAsArray, targetClass, null)
        } catch (e: Throwable) {
            return when (e) {
                is NotImplementedError,
                is FailedToRunCaseException,
                is RefactoringConflictsFoundException,
                is ConfigurationException -> RandomMoveRefactoringResult.Failed

                else -> RandomMoveRefactoringResult.ExceptionCaused(
                    testDataKeeper.caseData,
                    "${e.javaClass.typeName}: ${e.message ?: "No message"}"
                )
            }
        }

        return RandomMoveRefactoringResult.Success(testDataKeeper.caseData)
    }
}
