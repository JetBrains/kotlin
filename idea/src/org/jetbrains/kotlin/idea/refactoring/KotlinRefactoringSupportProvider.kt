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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeSignatureHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter.KotlinIntroduceLambdaParameterHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter.KotlinIntroduceParameterHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty.KotlinIntroducePropertyHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.refactoring.pullUp.KotlinPullUpHandler
import org.jetbrains.kotlin.idea.refactoring.safeDelete.*
import org.jetbrains.kotlin.psi.*

public class KotlinRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isSafeDeleteAvailable(element: PsiElement) = element.canDeleteElement()

    override fun getIntroduceVariableHandler() = KotlinIntroduceVariableHandler()

    override fun getIntroduceParameterHandler() = KotlinIntroduceParameterHandler()

    public fun getIntroduceLambdaParameterHandler(): RefactoringActionHandler = KotlinIntroduceLambdaParameterHandler()

    public fun getIntroducePropertyHandler(): RefactoringActionHandler = KotlinIntroducePropertyHandler()

    public fun getExtractFunctionHandler(): RefactoringActionHandler =
            ExtractKotlinFunctionHandler()

    public fun getExtractFunctionToScopeHandler(): RefactoringActionHandler =
            ExtractKotlinFunctionHandler(true, ExtractKotlinFunctionHandler.InteractiveExtractionHelper)

    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        when (element) {
            is JetProperty -> {
                if (element.isLocal()) return true
            }
            is JetMultiDeclarationEntry -> return true
            is JetFunction -> {
                if (element.isLocal()) return true
            }
            is JetParameter -> {
                val parent = element.getParent()
                if (parent is JetForExpression) {
                    return true
                }
                if (parent is JetParameterList) {
                    val grandparent = parent.getParent()
                    return grandparent is JetCatchClause || grandparent is JetFunctionLiteral
                }
            }
        }
        return false
    }

    override fun getChangeSignatureHandler() = JetChangeSignatureHandler()

    override fun getPullUpHandler() = KotlinPullUpHandler()
}
