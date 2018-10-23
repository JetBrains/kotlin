/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.tower.WrongResolutionToClassifier
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ConvertToAnonymousObjectFix(
    callExpression: KtCallExpression,
    private val interfaceName: String,
    private val functionName: String,
    private val functionParameters: String,
    private val functionReturnType: String?
) : KotlinQuickFixAction<KtCallExpression>(callExpression) {
    override fun getFamilyName() = "Convert to anonymous object"

    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val callExpression = element ?: return
        val lambda = callExpression.lambdaArguments.singleOrNull()?.getLambdaExpression() ?: return

        val psiFactory = KtPsiFactory(project)
        val body = lambda.bodyExpression
        if (body != null) {
            body.forEachDescendantOfType<KtReturnExpression> {
                if (it.getLabelName() == interfaceName) it.labeledExpression?.delete()
            }
            val last = body.statements.lastOrNull()
            if (last !is KtReturnExpression) last?.replace(psiFactory.createExpressionByPattern("return $0", last))
        }

        val anonymousObject = psiFactory.buildExpression {
            appendFixedText("object : $interfaceName {")
            appendFixedText("override fun $functionName$functionParameters")
            if (functionReturnType != null) appendFixedText(": $functionReturnType")
            appendFixedText("{")
            if (body != null) appendExpression(body)
            appendFixedText("}")
            appendFixedText("}")
        }
        callExpression.replace(anonymousObject)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtCallExpression>? {
            val casted = Errors.RESOLUTION_TO_CLASSIFIER.cast(diagnostic)
            if (casted.b != WrongResolutionToClassifier.INTERFACE_AS_FUNCTION) return null

            val callExpression = casted.psiElement.parent as? KtCallExpression ?: return null
            if (callExpression.lambdaArguments.singleOrNull()?.getLambdaExpression() == null) return null
            val interfaceName = callExpression.calleeExpression?.text ?: return null

            val classDescriptor = casted.a as? LazyClassDescriptor ?: return null
            val function = classDescriptor.declaredCallableMembers.singleOrNull() as? SimpleFunctionDescriptor ?: return null
            val functionDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(function) as? KtNamedFunction ?: return null
            val functionName = functionDeclaration.name ?: return null
            val functionParameters = functionDeclaration.valueParameterList?.text ?: return null
            val functionReturnType = function.returnType?.takeIf { !it.isUnit() }?.toString()

            return ConvertToAnonymousObjectFix(callExpression, interfaceName, functionName, functionParameters, functionReturnType)
        }
    }
}