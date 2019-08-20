/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.openapi.project.Project

internal sealed class RandomMoveRefactoringResult {
    internal class Success(val caseData: String) : RandomMoveRefactoringResult()
    internal class ExceptionCaused(val caseData: String, val message: String) : RandomMoveRefactoringResult()
    internal companion object Failed : RandomMoveRefactoringResult()
}

internal interface RefactoringCase {
    fun tryCreateAndRun(project: Project): RandomMoveRefactoringResult
}