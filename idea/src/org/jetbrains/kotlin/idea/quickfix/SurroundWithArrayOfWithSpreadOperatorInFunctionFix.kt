/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.CollectionLiteralResolver.Companion.ARRAY_OF_FUNCTION
import org.jetbrains.kotlin.resolve.CollectionLiteralResolver.Companion.PRIMITIVE_TYPE_TO_ARRAY

class SurroundWithArrayOfWithSpreadOperatorInFunctionFix(
        val wrapper: Name,
        argument: KtExpression
) : KotlinQuickFixAction<KtExpression>(argument) {
    override fun getText() = "Surround with *$wrapper(...)"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val argument = element?.getParentOfType<KtValueArgument>(false) ?: return
        val argumentName = argument.getArgumentName()?.asName ?: return
        val argumentExpression = argument.getArgumentExpression() ?: return

        val factory = KtPsiFactory(argumentExpression)

        val surroundedWithArrayOf = factory.createExpressionByPattern("$wrapper($0)", argumentExpression)
        val newArgument = factory.createArgument(surroundedWithArrayOf, argumentName, isSpread = true)

        argument.replace(newArgument)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val actualDiagnostic = when (diagnostic.factory) {
                Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION ->
                    Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION.cast(diagnostic)

                Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR ->
                    Errors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR.cast(diagnostic)

                else -> error("Non expected diagnostic: $diagnostic")
            }

            val parameterType = actualDiagnostic.a

            val wrapper = PRIMITIVE_TYPE_TO_ARRAY[KotlinBuiltIns.getPrimitiveArrayElementType(parameterType)] ?: ARRAY_OF_FUNCTION
            return SurroundWithArrayOfWithSpreadOperatorInFunctionFix(wrapper, actualDiagnostic.psiElement)
        }
    }
}