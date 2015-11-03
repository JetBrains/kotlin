/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.refactoring.createPrimaryConstructorIfAbsent
import org.jetbrains.kotlin.idea.core.refactoring.replaceListPsiAndKeepDelimiters
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.core.toKeywordToken
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetValVar
import org.jetbrains.kotlin.idea.refactoring.changeSignature.getCallableSubstitutor
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.utils.sure

class JetCallableDefinitionUsage<T : PsiElement>(
        function: T,
        val originalCallableDescriptor: CallableDescriptor,
        baseFunction: JetCallableDefinitionUsage<PsiElement>?,
        private val samCallType: KotlinType?
) : JetUsageInfo<T>(function) {
    val baseFunction: JetCallableDefinitionUsage<*> = baseFunction ?: this

    val hasExpectedType: Boolean = checkIfHasExpectedType(originalCallableDescriptor, isInherited)

    val currentCallableDescriptor: CallableDescriptor? by lazy {
        val element = declaration
        when (element) {
            is KtFunction, is KtProperty, is KtParameter -> (element as KtDeclaration).resolveToDescriptor() as CallableDescriptor
            is KtClass -> (element.resolveToDescriptor() as ClassDescriptor).unsubstitutedPrimaryConstructor
            is PsiMethod -> element.getJavaMethodDescriptor()
            else -> null
        }
    }

    val typeSubstitutor: TypeSubstitutor? by lazy {
        if (!isInherited) return@lazy null

        if (samCallType == null) {
            getCallableSubstitutor(this.baseFunction, this)
        }
        else {
            val currentBaseDescriptor = this.baseFunction.currentCallableDescriptor
            val classDescriptor = currentBaseDescriptor?.containingDeclaration as? ClassDescriptor ?: return@lazy null
            getTypeSubstitutor(classDescriptor.defaultType, samCallType)
        }
    }

    private fun checkIfHasExpectedType(callableDescriptor: CallableDescriptor, isInherited: Boolean): Boolean {
        if (!(callableDescriptor is AnonymousFunctionDescriptor && isInherited)) return false

        val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(callableDescriptor) as KtFunctionLiteral?
        assert(functionLiteral != null) { "No declaration found for " + callableDescriptor }

        val parent = functionLiteral!!.parent as? KtFunctionLiteralExpression ?: return false

        return parent.analyze(BodyResolveMode.PARTIAL)[BindingContext.EXPECTED_EXPRESSION_TYPE, parent] != null
    }

    val declaration: PsiElement
        get() = element!!

    val isInherited: Boolean
        get() = baseFunction !== this

    override fun processUsage(changeInfo: JetChangeInfo, element: T, allUsages: Array<out UsageInfo>): Boolean {
        if (element !is KtNamedDeclaration) return true

        val psiFactory = KtPsiFactory(element.project)

        if (changeInfo.isNameChanged) {
            val identifier = (element as KtCallableDeclaration).nameIdentifier

            identifier?.replace(psiFactory.createIdentifier(changeInfo.newName))
        }

        changeReturnTypeIfNeeded(changeInfo, element)

        val parameterList = element.getValueParameterList()

        if (changeInfo.isParameterSetOrOrderChanged) {
            processParameterListWithStructuralChanges(changeInfo, element, parameterList, psiFactory)
        }
        else if (parameterList != null) {
            var paramIndex = if (originalCallableDescriptor.extensionReceiverParameter != null) 1 else 0

            for (parameter in parameterList.parameters) {
                val parameterInfo = changeInfo.newParameters[paramIndex]
                changeParameter(paramIndex, parameter, parameterInfo)
                paramIndex++
            }

            parameterList.addToShorteningWaitSet(Options.DEFAULT)
        }

        if (element is KtCallableDeclaration && changeInfo.isReceiverTypeChanged()) {
            val receiverTypeText = changeInfo.renderReceiverType(this)
            val receiverTypeRef = if (receiverTypeText != null) psiFactory.createType(receiverTypeText) else null
            val newReceiverTypeRef = element.setReceiverTypeReference(receiverTypeRef)
            newReceiverTypeRef?.addToShorteningWaitSet(ShortenReferences.Options.DEFAULT)
        }

        if (changeInfo.isVisibilityChanged() && !KtPsiUtil.isLocal(element as KtDeclaration)) {
            changeVisibility(changeInfo, element)
        }

        return true
    }

    protected fun changeReturnTypeIfNeeded(changeInfo: JetChangeInfo, element: PsiElement) {
        if (element !is KtCallableDeclaration) return
        if (element is KtConstructor<*>) return

        val returnTypeIsNeeded = if (element is KtFunction) {
            element !is KtFunctionLiteral && (changeInfo.isRefactoringTarget(originalCallableDescriptor) || element.typeReference != null)
        }
        else {
            element is KtProperty || element is KtParameter
        }

        if (changeInfo.isReturnTypeChanged && returnTypeIsNeeded) {
            element.setTypeReference(null)
            val returnTypeText = changeInfo.renderReturnType(this)

            //TODO use ChangeFunctionReturnTypeFix.invoke when JetTypeCodeFragment.getType() is ready
            if (!(returnTypeText == "Unit" || returnTypeText == "kotlin.Unit")) {
                element.setTypeReference(KtPsiFactory(element).createType(returnTypeText))!!.addToShorteningWaitSet(
                        Options.DEFAULT)
            }
        }
    }

    private fun processParameterListWithStructuralChanges(
            changeInfo: JetChangeInfo,
            element: PsiElement,
            originalParameterList: KtParameterList?,
            psiFactory: KtPsiFactory) {
        var parameterList = originalParameterList
        val parametersCount = changeInfo.getNonReceiverParametersCount()
        val isLambda = element is KtFunctionLiteral
        var canReplaceEntireList = false

        var newParameterList: KtParameterList? = null
        if (isLambda) {
            if (parametersCount == 0) {
                if (parameterList != null) {
                    parameterList.delete()
                    val arrow = (element as KtFunctionLiteral).arrow
                    arrow?.delete()
                    parameterList = null
                }
            }
            else {
                newParameterList = psiFactory.createFunctionLiteralParameterList(changeInfo.getNewParametersSignatureWithoutParentheses(this))
                canReplaceEntireList = true
            }
        }
        else if (!(element is KtProperty || element is KtParameter)) {
            newParameterList = psiFactory.createParameterList(changeInfo.getNewParametersSignature(this))
        }

        if (newParameterList == null) return

        if (parameterList != null) {
            if (canReplaceEntireList) {
                newParameterList = parameterList.replace(newParameterList) as KtParameterList
            }
            else {
                newParameterList = replaceListPsiAndKeepDelimiters(parameterList, newParameterList) { parameters }
            }
        }
        else {
            if (element is KtClass) {
                val constructor = element.createPrimaryConstructorIfAbsent()
                val oldParameterList = constructor.valueParameterList.sure { "primary constructor from factory has parameter list" }
                newParameterList = oldParameterList.replace(newParameterList) as KtParameterList
            }
            else if (isLambda) {
                val functionLiteral = element as KtFunctionLiteral
                val anchor = functionLiteral.lBrace
                newParameterList = element.addAfter(newParameterList, anchor) as KtParameterList
                if (functionLiteral.arrow == null) {
                    val whitespaceAndArrow = psiFactory.createWhitespaceAndArrow()
                    element.addRangeAfter(whitespaceAndArrow.first, whitespaceAndArrow.second, newParameterList)
                }
            }
        }

        newParameterList.addToShorteningWaitSet(Options.DEFAULT)
    }

    private fun changeVisibility(changeInfo: JetChangeInfo, element: PsiElement) {
        val newVisibilityToken = changeInfo.newVisibility.toKeywordToken()
        when (element) {
            is KtCallableDeclaration -> element.setVisibility(newVisibilityToken)
            is KtClass -> element.createPrimaryConstructorIfAbsent().setVisibility(newVisibilityToken)
            else -> throw AssertionError("Invalid element: " + element.getElementTextWithContext())
        }
    }

    private fun changeParameter(parameterIndex: Int, parameter: KtParameter, parameterInfo: JetParameterInfo) {
        val valOrVarKeyword = parameter.valOrVarKeyword
        val valOrVar = parameterInfo.valOrVar

        val psiFactory = KtPsiFactory(project)
        val newKeyword = valOrVar.createKeyword(psiFactory)
        if (valOrVarKeyword != null) {
            if (newKeyword != null) {
                valOrVarKeyword.replace(newKeyword)
            }
            else {
                valOrVarKeyword.delete()
            }
        }
        else if (valOrVar != JetValVar.None && newKeyword != null) {
            val firstChild = parameter.firstChild
            parameter.addBefore(newKeyword, firstChild)
            parameter.addBefore(psiFactory.createWhiteSpace(), firstChild)
        }

        if (parameterInfo.isTypeChanged && parameter.typeReference != null) {
            val renderedType = parameterInfo.renderType(parameterIndex, this)
            parameter.setTypeReference(psiFactory.createType(renderedType))
        }

        val newIdentifier = psiFactory.createIdentifier(parameterInfo.getInheritedName(this))
        parameter.nameIdentifier?.replace(newIdentifier)
    }
}
