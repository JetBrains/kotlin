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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression: KtLambdaExpression, private val type: KotlinType) : KotlinQuickFixAction<KtLambdaExpression>(functionLiteralExpression) {
    private val functionLiteralReturnTypeRef: KtTypeReference?
    private var appropriateQuickFix: IntentionAction? = null

    init {
        functionLiteralReturnTypeRef = functionLiteralExpression.functionLiteral.typeReference

        doInit(functionLiteralExpression)
    }

    private fun doInit(functionLiteralExpression: KtLambdaExpression) {
        val analysisResult = functionLiteralExpression.getContainingKtFile().analyzeFullyAndGetResult()
        val context = analysisResult.bindingContext
        val functionLiteralType = context.getType(functionLiteralExpression) ?: error("Type of function literal not available in binding context")

        val builtIns = analysisResult.moduleDescriptor.builtIns
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
        if (correspondingProperty != null && QuickFixUtil.canEvaluateTo(correspondingProperty.initializer!!, functionLiteralExpression)) {
            val correspondingPropertyTypeRef = correspondingProperty.typeReference
            val propertyType = context.get(BindingContext.TYPE, correspondingPropertyTypeRef)
            if (propertyType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, propertyType)) {
                appropriateQuickFix = ChangeVariableTypeFix(correspondingProperty, eventualFunctionLiteralType)
            }
            return
        }

        val resolvedCall = functionLiteralExpression.getParentResolvedCall(context, true)
        if (resolvedCall != null) {
            val valueArgument = resolvedCall.call.getValueArgumentForExpression(functionLiteralExpression)
            val correspondingParameter = QuickFixUtil.getParameterDeclarationForValueArgument(resolvedCall, valueArgument)
            if (correspondingParameter != null) {
                val correspondingParameterTypeRef = correspondingParameter.typeReference
                val parameterType = context.get(BindingContext.TYPE, correspondingParameterTypeRef)
                if (parameterType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parameterType)) {
                    appropriateQuickFix = ChangeParameterTypeFix(correspondingParameter, eventualFunctionLiteralType)
                }
                return
            }
        }


        val parentFunction = PsiTreeUtil.getParentOfType(functionLiteralExpression, KtFunction::class.java, true)
        if (parentFunction != null && QuickFixUtil.canFunctionOrGetterReturnExpression(parentFunction, functionLiteralExpression)) {
            val parentFunctionReturnTypeRef = parentFunction.typeReference
            val parentFunctionReturnType = context.get(BindingContext.TYPE, parentFunctionReturnTypeRef)
            if (parentFunctionReturnType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parentFunctionReturnType)) {
                appropriateQuickFix = ChangeFunctionReturnTypeFix(parentFunction, eventualFunctionLiteralType)
            }
        }
    }

    override fun getText(): String {
        if (appropriateQuickFix != null) {
            return appropriateQuickFix!!.text
        }
        return String.format("Change lambda expression return type to '%s'",
                             IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type))
    }

    override fun getFamilyName(): String {
        return KotlinBundle.message("change.type.family")
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) && (functionLiteralReturnTypeRef != null || appropriateQuickFix != null && appropriateQuickFix!!.isAvailable(project, editor!!, file))
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (functionLiteralReturnTypeRef != null) {
            var newTypeRef = KtPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
            newTypeRef = functionLiteralReturnTypeRef.replace(newTypeRef) as KtTypeReference
            ShortenReferences.DEFAULT.process(newTypeRef)
        }
        if (appropriateQuickFix != null && appropriateQuickFix!!.isAvailable(project, editor!!, file)) {
            appropriateQuickFix!!.invoke(project, editor, file)
        }
    }

    companion object {
        fun createFactoryForExpectedOrAssignmentTypeMismatch(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val functionLiteralExpression = QuickFixUtil.getParentElementOfType(diagnostic, KtLambdaExpression::class.java) ?: return null
                    return ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, functionLiteralExpression.platform.builtIns.getUnitType())
                }
            }
        }
    }
}
