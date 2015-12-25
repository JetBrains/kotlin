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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.expressions.TypeReconstructionUtil
import org.jetbrains.kotlin.utils.sure

open class AddStarProjectionsFix private constructor(element: KtUserType,
                                                     private val argumentCount: Int) : KotlinQuickFixAction<KtUserType>(element) {
    override fun getFamilyName() = "Add star projections"
    override fun getText() = "Add '${TypeReconstructionUtil.getTypeNameAndStarProjectionsString("", argumentCount)}'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        assert(element.typeArguments.isEmpty())

        val typeString = TypeReconstructionUtil.getTypeNameAndStarProjectionsString(element.text, argumentCount)
        val replacement = KtPsiFactory(file).createType(typeString).typeElement.sure { "No type element after parsing " + typeString }
        element.replace(replacement)
    }

    object IsExpressionFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val diagnosticWithParameters = Errors.NO_TYPE_ARGUMENTS_ON_RHS.cast(diagnostic)
            val typeElement: KtTypeElement = diagnosticWithParameters.psiElement.typeElement ?: return null
            val unwrappedType = sequence(typeElement) { (it as? KtNullableType)?.innerType }.lastOrNull() as? KtUserType ?: return null
            return AddStarProjectionsFix(unwrappedType, diagnosticWithParameters.a)
        }
    }

    object JavaClassFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val diagnosticWithParameters = Errors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.cast(diagnostic)
            val size = diagnosticWithParameters.a
            val userType = QuickFixUtil.getParentElementOfType(diagnostic, KtUserType::class.java) ?: return null
            return object : AddStarProjectionsFix(userType, size) {
                override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
                    // We are looking for the occurrence of Type in javaClass<Type>()
                    return super.isAvailable(project, editor, file) && isZeroTypeArguments && isInsideJavaClassCall
                }

                private val isZeroTypeArguments: Boolean
                    get() = element.typeArguments.isEmpty()

                // Resolve is expensive so we use a heuristic here: the case is rare enough not to be annoying
                private val isInsideJavaClassCall: Boolean
                    get() {
                        val call = element.parent.parent.parent.parent as? KtCallExpression
                        val callee = call?.calleeExpression as? KtSimpleNameExpression
                        return callee?.getReferencedName() == "javaClass"
                    }
            }
        }
    }
}
