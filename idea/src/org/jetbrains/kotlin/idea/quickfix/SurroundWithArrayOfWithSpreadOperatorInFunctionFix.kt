/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.ArrayFqNames

class SurroundWithArrayOfWithSpreadOperatorInFunctionFix(
    val wrapper: Name,
    argument: KtExpression
) : KotlinQuickFixAction<KtExpression>(argument) {
    override fun getText() = KotlinBundle.message("surround.with.star.0", wrapper)

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

            val wrapper = ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY[KotlinBuiltIns.getPrimitiveArrayElementType(parameterType)] ?: ArrayFqNames.ARRAY_OF_FUNCTION
            return SurroundWithArrayOfWithSpreadOperatorInFunctionFix(wrapper, actualDiagnostic.psiElement)
        }
    }
}