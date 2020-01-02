/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.refactoring.RefactoringActionHandler
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSupportProvider
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractIntroduceAction

class ExtractFunctionAction : AbstractIntroduceAction() {
    override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? =
        (provider as? KotlinRefactoringSupportProvider)?.getExtractFunctionHandler()
}

class ExtractFunctionToScopeAction : AbstractIntroduceAction() {
    override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? =
        (provider as? KotlinRefactoringSupportProvider)?.getExtractFunctionToScopeHandler()
}
