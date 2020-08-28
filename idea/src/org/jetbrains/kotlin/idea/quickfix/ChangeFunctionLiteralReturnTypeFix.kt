/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class ChangeFunctionLiteralReturnTypeFix(
    functionLiteralExpression: KtLambdaExpression,
    type: KotlinType
) : KotlinQuickFixAction<KtLambdaExpression>(functionLiteralExpression) {

    private val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_TYPES_WITH_SHORT_NAMES.renderType(type)
    private val typeSourceCode = IdeDescriptorRenderers.SOURCE_CODE_TYPES.renderType(type)
    private val functionLiteralReturnTypeRef: KtTypeReference?
        get() = element?.functionLiteral?.typeReference
    private val appropriateQuickFix = createAppropriateQuickFix(functionLiteralExpression, type)

    private fun createAppropriateQuickFix(functionLiteralExpression: KtLambdaExpression, type: KotlinType): IntentionAction? {
        val context = functionLiteralExpression.analyze()
        val functionLiteralType = context.getType(functionLiteralExpression) ?: return null

        val builtIns = functionLiteralType.constructor.builtIns
        val functionClass = builtIns.getFunction(functionLiteralType.arguments.size - 1)
        val functionClassTypeParameters = LinkedList<KotlinType>()
        for (typeProjection in functionLiteralType.arguments) {
            functionClassTypeParameters.add(typeProjection.type)
        }
        // Replacing return type:
        functionClassTypeParameters.removeAt(functionClassTypeParameters.size - 1)
        functionClassTypeParameters.add(type)
        val eventualFunctionLiteralType = TypeUtils.substituteParameters(functionClass, functionClassTypeParameters)

        val correspondingProperty = PsiTreeUtil.getParentOfType(functionLiteralExpression, KtProperty::class.java)
        if (correspondingProperty != null &&
            correspondingProperty.delegate == null &&
            correspondingProperty.initializer?.let { QuickFixUtil.canEvaluateTo(it, functionLiteralExpression) } != false
        ) {
            val correspondingPropertyTypeRef = correspondingProperty.typeReference
            val propertyType = context.get(BindingContext.TYPE, correspondingPropertyTypeRef)
            return if (propertyType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, propertyType))
                ChangeVariableTypeFix(correspondingProperty, eventualFunctionLiteralType)
            else
                null
        }

        val resolvedCall = functionLiteralExpression.getParentResolvedCall(context, true)
        if (resolvedCall != null) {
            val valueArgument = resolvedCall.call.getValueArgumentForExpression(functionLiteralExpression)
            val correspondingParameter = QuickFixUtil.getParameterDeclarationForValueArgument(resolvedCall, valueArgument)
            if (correspondingParameter != null) {
                val correspondingParameterTypeRef = correspondingParameter.typeReference
                val parameterType = context.get(BindingContext.TYPE, correspondingParameterTypeRef)
                return if (parameterType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parameterType))
                    ChangeParameterTypeFix(correspondingParameter, eventualFunctionLiteralType)
                else
                    null
            }
        }


        val parentFunction = PsiTreeUtil.getParentOfType(functionLiteralExpression, KtFunction::class.java, true)
        return if (parentFunction != null && QuickFixUtil.canFunctionOrGetterReturnExpression(parentFunction, functionLiteralExpression)) {
            val parentFunctionReturnTypeRef = parentFunction.typeReference
            val parentFunctionReturnType = context.get(BindingContext.TYPE, parentFunctionReturnTypeRef)
            if (parentFunctionReturnType != null && !KotlinTypeChecker.DEFAULT
                    .isSubtypeOf(eventualFunctionLiteralType, parentFunctionReturnType)
            )
                ChangeCallableReturnTypeFix.ForEnclosing(parentFunction, eventualFunctionLiteralType)
            else
                null
        } else
            null
    }

    override fun getText() = appropriateQuickFix?.text ?: KotlinBundle.message("fix.change.return.type.lambda", typePresentation)
    override fun getFamilyName() = KotlinBundle.message("fix.change.return.type.family")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean =
        functionLiteralReturnTypeRef != null || appropriateQuickFix != null && appropriateQuickFix.isAvailable(
            project,
            editor!!,
            file
        )

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        functionLiteralReturnTypeRef?.let {
            val newTypeRef = it.replace(KtPsiFactory(file).createType(typeSourceCode)) as KtTypeReference
            ShortenReferences.DEFAULT.process(newTypeRef)
        }
        if (appropriateQuickFix != null && appropriateQuickFix.isAvailable(project, editor!!, file)) {
            appropriateQuickFix.invoke(project, editor, file)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val functionLiteralExpression = QuickFixUtil.getParentElementOfType(diagnostic, KtLambdaExpression::class.java) ?: return null
            return ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, functionLiteralExpression.builtIns.unitType)
        }
    }
}
