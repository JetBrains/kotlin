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

package org.jetbrains.jet.plugin.refactoring.extractFunction

import com.intellij.refactoring.actions.BasePlatformRefactoringAction
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.lang.refactoring.RefactoringSupportProvider
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.jet.plugin.refactoring.JetRefactoringSupportProvider

public abstract class AbstractExtractFunctionAction: BasePlatformRefactoringAction() {
    {
        setInjectedContext(true)
    }

    override fun isAvailableInEditorOnly(): Boolean = true

    protected override fun isEnabledOnElements(elements: Array<out PsiElement>): Boolean = 
            elements.all { it is JetElement }
}

public class ExtractFunctionAction: AbstractExtractFunctionAction() {
    override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? =
            (provider as? JetRefactoringSupportProvider)?.getExtractFunctionHandler()
}

public class ExtractFunctionToScopeAction: AbstractExtractFunctionAction() {
    override fun getRefactoringHandler(provider: RefactoringSupportProvider): RefactoringActionHandler? =
            (provider as? JetRefactoringSupportProvider)?.getExtractFunctionToScopeHandler()
}
