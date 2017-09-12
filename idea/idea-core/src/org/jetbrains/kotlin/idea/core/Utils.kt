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

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.analysis.computeTypeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstanceToExpression
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getDispatchReceiverWithSmartCast
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.*

fun Call.mapArgumentsToParameters(targetDescriptor: CallableDescriptor): Map<ValueArgument, ValueParameterDescriptor> {
    val parameters = targetDescriptor.valueParameters
    if (parameters.isEmpty()) return emptyMap()

    val map = HashMap<ValueArgument, ValueParameterDescriptor>()
    val parametersByName = parameters.associateBy { it.name }

    var positionalArgumentIndex: Int? = 0

    for (argument in valueArguments) {
        if (argument is LambdaArgument) {
            map[argument] = parameters.last()
        }
        else {
            val argumentName = argument.getArgumentName()?.asName

            if (argumentName != null) {
                if (targetDescriptor.hasStableParameterNames()) {
                    val parameter = parametersByName[argumentName]
                    if (parameter != null) {
                        map[argument] = parameter
                    }
                }
                positionalArgumentIndex = null
            }
            else {
                if (positionalArgumentIndex != null && positionalArgumentIndex < parameters.size) {
                    val parameter = parameters[positionalArgumentIndex]
                    map[argument] = parameter

                    if (parameter.varargElementType == null) {
                        positionalArgumentIndex++
                    }
                }
            }
        }
    }

    return map
}

fun ImplicitReceiver.asExpression(resolutionScope: LexicalScope, psiFactory: KtPsiFactory): KtExpression? {
    val expressionFactory = resolutionScope.getImplicitReceiversWithInstanceToExpression()
                                    .entries
                                    .firstOrNull { it.key.containingDeclaration == this.declarationDescriptor }
                                    ?.value ?: return null
    return expressionFactory.createExpression(psiFactory)
}

fun KtImportDirective.targetDescriptors(resolutionFacade: ResolutionFacade = this.getResolutionFacade()): Collection<DeclarationDescriptor> {
    // For codeFragments imports are created in dummy file
    if (this.containingKtFile.doNotAnalyze != null) return emptyList()
    val nameExpression = importedReference?.getQualifiedElementSelector() as? KtSimpleNameExpression ?: return emptyList()
    return nameExpression.mainReference.resolveToDescriptors(resolutionFacade.analyze(nameExpression))
}

fun Call.resolveCandidates(
        bindingContext: BindingContext,
        resolutionFacade: ResolutionFacade,
        expectedType: KotlinType = expectedType(this, bindingContext),
        filterOutWrongReceiver: Boolean = true,
        filterOutByVisibility: Boolean = true
): Collection<ResolvedCall<FunctionDescriptor>> {
    val resolutionScope = callElement.getResolutionScope(bindingContext, resolutionFacade)
    val inDescriptor = resolutionScope.ownerDescriptor

    val dataFlowInfo = bindingContext.getDataFlowInfoBefore(callElement)
    val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
    val callResolutionContext = BasicCallResolutionContext.create(
            bindingTrace, resolutionScope, this, expectedType, dataFlowInfo,
            ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            false, callElement.languageVersionSettings
    ).replaceCollectAllCandidates(true)
    val callResolver = resolutionFacade.frontendService<CallResolver>()

    val results = callResolver.resolveFunctionCall(callResolutionContext)

    var candidates = results.allCandidates!!

    if (callElement is KtConstructorDelegationCall) { // for "this(...)" delegation call exclude caller from candidates
        inDescriptor as ConstructorDescriptor
        candidates = candidates.filter { it.resultingDescriptor.original != inDescriptor.original }
    }

    if (filterOutWrongReceiver) {
        candidates = candidates.filter { it.status != ResolutionStatus.RECEIVER_TYPE_ERROR && it.status != ResolutionStatus.RECEIVER_PRESENCE_ERROR }
    }

    if (filterOutByVisibility) {
        candidates = candidates.filter {
            Visibilities.isVisible(it.getDispatchReceiverWithSmartCast(), it.resultingDescriptor, inDescriptor)
        }
    }

    return candidates
}

private fun expectedType(call: Call, bindingContext: BindingContext): KotlinType {
    return (call.callElement as? KtExpression)?.let {
        bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, it.getQualifiedExpressionForSelectorOrThis()]
    } ?: TypeUtils.NO_EXPECTED_TYPE
}

fun KtCallableDeclaration.canOmitDeclaredType(initializerOrBodyExpression: KtExpression, canChangeTypeToSubtype: Boolean): Boolean {
    val declaredType = (unsafeResolveToDescriptor() as? CallableDescriptor)?.returnType ?: return false
    val bindingContext = initializerOrBodyExpression.analyze()
    val scope = initializerOrBodyExpression.getResolutionScope(bindingContext, initializerOrBodyExpression.getResolutionFacade())
    val expressionType = initializerOrBodyExpression.computeTypeInContext(scope) ?: return false
    if (KotlinTypeChecker.DEFAULT.equalTypes(expressionType, declaredType)) return true
    return canChangeTypeToSubtype && expressionType.isSubtypeOf(declaredType)
}

fun String.quoteIfNeeded(): String = if (KotlinNameSuggester.isIdentifier(this)) this else "`$this`"

fun String.unquote(): String = KtPsiUtil.unquoteIdentifier(this)

fun FqName.quoteSegmentsIfNeeded(): String {
    return pathSegments().joinToString(".") { it.asString().quoteIfNeeded() }
}

fun FqName.quoteIfNeeded() = FqName(quoteSegmentsIfNeeded())

fun isEnumCompanionPropertyWithEntryConflict(element: PsiElement, expectedName: String): Boolean {
    if (element !is KtProperty) return false

    val propertyClass = element.containingClassOrObject as? KtObjectDeclaration ?: return false
    if (!propertyClass.isCompanion()) return false

    val outerClass = propertyClass.containingClassOrObject as? KtClass ?: return false
    if (!outerClass.isEnum()) return false

    return outerClass.declarations.any { it is KtEnumEntry && it.name == expectedName }
}