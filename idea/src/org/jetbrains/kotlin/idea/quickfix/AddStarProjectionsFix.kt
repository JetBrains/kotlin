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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.expressions.TypeReconstructionUtil
import org.jetbrains.kotlin.utils.sure

open class AddStarProjectionsFix private constructor(element: KtUserType,
                                                     private val argumentCount: Int) : KotlinQuickFixAction<KtUserType>(element) {
    override fun getFamilyName() = "Add star projections"
    override fun getText() = "Add '${TypeReconstructionUtil.getTypeNameAndStarProjectionsString("", argumentCount)}'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        assert(element.typeArguments.isEmpty())

        val typeString = TypeReconstructionUtil.getTypeNameAndStarProjectionsString(element.text, argumentCount)
        val replacement = KtPsiFactory(file).createType(typeString).typeElement.sure { "No type element after parsing " + typeString }
        element.replace(replacement)
    }

    object IsExpressionFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val diagnosticWithParameters = Errors.NO_TYPE_ARGUMENTS_ON_RHS.cast(diagnostic)
            val typeElement: KtTypeElement = diagnosticWithParameters.psiElement.typeElement ?: return null
            val unwrappedType = generateSequence(typeElement) { (it as? KtNullableType)?.innerType }.lastOrNull() as? KtUserType ?: return null
            return AddStarProjectionsFix(unwrappedType, diagnosticWithParameters.a)
        }
    }
}
