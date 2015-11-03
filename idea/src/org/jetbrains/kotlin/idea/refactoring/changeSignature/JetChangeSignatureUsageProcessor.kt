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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.changeSignature.*
import com.intellij.refactoring.rename.ResolveSnapshotProvider
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.HashSet
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.KotlinFileReferencesResolver
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.refactoring.createTempCopy
import org.jetbrains.kotlin.idea.core.refactoring.isTrueJavaMethod
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.*
import org.jetbrains.kotlin.idea.refactoring.getBodyScope
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class JetChangeSignatureUsageProcessor : ChangeSignatureUsageProcessor {
    // This is special 'PsiElement' whose purpose is to wrap JetMethodDescriptor so that it can be kept in the usage list
    private class OriginalJavaMethodDescriptorWrapper(element: PsiElement) : UsageInfo(element) {
        internal var originalJavaMethodDescriptor: JetMethodDescriptor? = null
    }

    private class DummyJetChangeInfo(
            method: PsiElement,
            methodDescriptor: JetMethodDescriptor
    ) : JetChangeInfo(methodDescriptor = methodDescriptor,
                      name = "",
                      newReturnType = null,
                      newReturnTypeText = "",
                      newVisibility = Visibilities.DEFAULT_VISIBILITY,
                      parameterInfos = emptyList<JetParameterInfo>(),
                      receiver = null,
                      context = method)

    // It's here to prevent O(usage_count^2) performance
    private var initializedOriginalDescriptor: Boolean = false

    override fun findUsages(info: ChangeInfo): Array<UsageInfo> {
        initializedOriginalDescriptor = false

        val result = HashSet<UsageInfo>()

        result.add(OriginalJavaMethodDescriptorWrapper(info.method))

        if (info is JetChangeInfo) {
            findAllMethodUsages(info, result)
        }
        else {
            findSAMUsages(info, result)
            //findConstructorDelegationUsages(info, result)
            findKotlinOverrides(info, result)
            if (info is JavaChangeInfo) {
                findKotlinCallers(info, result)
            }
        }

        return result.toTypedArray()
    }

    private fun findAllMethodUsages(changeInfo: JetChangeInfo, result: MutableSet<UsageInfo>) {
        loop@ for (functionUsageInfo in changeInfo.getAffectedCallables()) {
            when (functionUsageInfo) {
                is JetCallableDefinitionUsage<*> -> findOneMethodUsages(functionUsageInfo, changeInfo, result)
                is KotlinCallerUsage -> findCallerUsages(functionUsageInfo, changeInfo, result)
                else -> {
                    result.add(functionUsageInfo)

                    val callee = functionUsageInfo.element ?: continue@loop

                    val propagationTarget = functionUsageInfo is CallerUsageInfo
                                            || (functionUsageInfo is OverriderUsageInfo && !functionUsageInfo.isOriginalOverrider)


                    for (reference in ReferencesSearch.search(callee, callee.useScope.restrictToKotlinSources())) {
                        val callElement = reference.element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: continue
                        val usage = if (propagationTarget) {
                            KotlinCallerCallUsage(callElement)
                        }
                        else {
                            JetFunctionCallUsage(callElement, changeInfo.methodDescriptor.originalPrimaryCallable)
                        }
                        result.add(usage)
                    }
                }
            }
        }
    }

    private fun findCallerUsages(callerUsage: KotlinCallerUsage, changeInfo: JetChangeInfo, result: MutableSet<UsageInfo>) {
        result.add(callerUsage)

        val element = callerUsage.element ?: return

        for (ref in ReferencesSearch.search(element, element.useScope)) {
            val callElement = ref.element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: continue
            result.add(KotlinCallerCallUsage(callElement))
        }

        val body = element.getDeclarationBody() ?: return
        val callerDescriptor = element.resolveToDescriptorIfAny() ?: return
        val context = body.analyze()
        val newParameterNames = changeInfo.getNonReceiverParameters().mapTo(HashSet<String>()) { it.name }
        body.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                        val currentName = expression.getReferencedName()
                        if (currentName !in newParameterNames) return

                        val resolvedCall = expression.getResolvedCall(context) ?: return

                        if (resolvedCall.explicitReceiverKind != ExplicitReceiverKind.NO_EXPLICIT_RECEIVER) return

                        val resultingDescriptor = resolvedCall.resultingDescriptor
                        if (resultingDescriptor !is VariableDescriptor) return

                        // Do not report usages of duplicated parameter
                        if (resultingDescriptor is ValueParameterDescriptor
                            && resultingDescriptor.containingDeclaration === callerDescriptor) return

                        val callElement = resolvedCall.call.callElement

                        var receiver = resolvedCall.extensionReceiver
                        if (receiver !is ThisReceiver) {
                            receiver = resolvedCall.dispatchReceiver
                        }

                        if (receiver is ThisReceiver) {
                            result.add(JetImplicitThisUsage(callElement, receiver.declarationDescriptor))
                        }
                        else if (!receiver.exists()) {
                            result.add(
                                    object : UnresolvableCollisionUsageInfo(callElement, null) {
                                        override fun getDescription(): String {
                                            val signature = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(callerDescriptor)
                                            return "There is already a variable '$currentName' in $signature. It will conflict with the new parameter."
                                        }
                                    }
                            )
                        }
                    }
                })
    }

    private fun findReferences(functionPsi: PsiElement): Set<PsiReference> {
        val result = LinkedHashSet<PsiReference>()

        val searchScope = functionPsi.useScope
        val options = KotlinReferencesSearchOptions(true, false, false, false)
        val parameters = KotlinReferencesSearchParameters(functionPsi, searchScope, false, null, options)
        result.addAll(ReferencesSearch.search(parameters).findAll())
        if (functionPsi is KtProperty || functionPsi is KtParameter) {
            functionPsi.toLightMethods().flatMapTo(result) { MethodReferencesSearch.search(it, searchScope, true).findAll() }
        }

        return result
    }

    private fun findOneMethodUsages(
            functionUsageInfo: JetCallableDefinitionUsage<*>,
            changeInfo: JetChangeInfo,
            result: MutableSet<UsageInfo>
    ) {
        val isInherited = functionUsageInfo.isInherited

        if (isInherited) {
            result.add(functionUsageInfo)
        }

        val functionPsi = functionUsageInfo.element ?: return

        for (reference in findReferences(functionPsi)) {
            val element = reference.element

            if (functionPsi is KtClass && reference.resolve() !== functionPsi) continue

            if (element is KtReferenceExpression) {
                var parent = element.parent

                when {
                    parent is KtCallExpression ->
                        result.add(JetFunctionCallUsage(parent, functionUsageInfo))

                    parent is KtUserType && parent.parent is KtTypeReference -> {
                        parent = parent.parent.parent

                        if (parent is KtConstructorCalleeExpression && parent.parent is KtDelegatorToSuperCall)
                            result.add(JetFunctionCallUsage(parent.parent as KtDelegatorToSuperCall, functionUsageInfo))
                    }

                    element is KtSimpleNameExpression && (functionPsi is KtProperty || functionPsi is KtParameter) ->
                        result.add(JetPropertyCallUsage(element))
                }
            }
        }

        val oldName = changeInfo.oldName

        if (oldName != null) {
            TextOccurrencesUtil.findNonCodeUsages(functionPsi, oldName, true, true, changeInfo.newName, result)
        }

        val oldParameters = (functionPsi as KtNamedDeclaration).getValueParameters()

        val newReceiverInfo = changeInfo.receiverParameterInfo

        for (parameterInfo in changeInfo.newParameters) {
            if (parameterInfo.oldIndex >= 0 && parameterInfo.oldIndex < oldParameters.size) {
                val oldParam = oldParameters[parameterInfo.oldIndex]
                val oldParamName = oldParam.name

                if (parameterInfo == newReceiverInfo || (oldParamName != null && oldParamName != parameterInfo.name)) {
                    for (reference in ReferencesSearch.search(oldParam, oldParam.useScope)) {
                        val element = reference.element

                        // Usages in named arguments of the calls usage will be changed when the function call is changed
                        if ((element is KtSimpleNameExpression || element is KDocName) && element.parent !is KtValueArgumentName) {
                            result.add(JetParameterUsage(element as KtElement, parameterInfo, functionUsageInfo))
                        }
                    }
                }
            }
        }

        if (functionPsi is KtFunction && newReceiverInfo != changeInfo.methodDescriptor.receiver) {
            findOriginalReceiversUsages(functionUsageInfo, result, changeInfo)
        }

        if (functionPsi is KtClass && functionPsi.isEnum()) {
            for (declaration in functionPsi.declarations) {
                if (declaration is KtEnumEntry && declaration.getDelegationSpecifiers().isEmpty()) {
                    result.add(JetEnumEntryWithoutSuperCallUsage(declaration))
                }
            }
        }

        functionPsi.processDelegationCallConstructorUsages(functionPsi.useScope) {
            when (it) {
                is KtConstructorDelegationCall -> result.add(JetConstructorDelegationCallUsage(it, changeInfo))
                is KtDelegatorToSuperCall -> result.add(JetFunctionCallUsage(it, functionUsageInfo))
            }
            true
        }
    }

    private fun processInternalReferences(functionUsageInfo: JetCallableDefinitionUsage<*>, visitor: KtTreeVisitor<BindingContext>) {
        val ktFunction = functionUsageInfo.declaration as KtFunction

        val body = ktFunction.bodyExpression
        body?.accept(visitor, body.analyze(BodyResolveMode.FULL))

        for (parameter in ktFunction.valueParameters) {
            val defaultValue = parameter.defaultValue
            defaultValue?.accept(visitor, defaultValue.analyze(BodyResolveMode.FULL))
        }
    }

    private fun findOriginalReceiversUsages(
            functionUsageInfo: JetCallableDefinitionUsage<*>,
            result: MutableSet<UsageInfo>,
            changeInfo: JetChangeInfo
    ) {
        val originalReceiverInfo = changeInfo.methodDescriptor.receiver
        val callableDescriptor = functionUsageInfo.originalCallableDescriptor
        processInternalReferences(
                functionUsageInfo,
                object : KtTreeVisitor<BindingContext>() {
                    private fun processExplicitThis(
                            expression: KtSimpleNameExpression,
                            receiverDescriptor: ReceiverParameterDescriptor) {
                        if (originalReceiverInfo != null && !changeInfo.hasParameter(originalReceiverInfo)) return
                        if (expression.parent !is KtThisExpression) return

                        if (receiverDescriptor === callableDescriptor.extensionReceiverParameter) {
                            assert(originalReceiverInfo != null) { "No original receiver info provided: " + functionUsageInfo.declaration.text }
                            result.add(JetParameterUsage(expression, originalReceiverInfo!!, functionUsageInfo))
                        }
                        else {
                            val targetDescriptor = receiverDescriptor.type.constructor.declarationDescriptor
                            assert(targetDescriptor != null) { "Receiver type has no descriptor: " + functionUsageInfo.declaration.text }
                            result.add(JetNonQualifiedOuterThisUsage(expression.parent as KtThisExpression, targetDescriptor!!))
                        }
                    }

                    private fun processImplicitThis(
                            callElement: KtElement,
                            receiverValue: ThisReceiver) {
                        val targetDescriptor = receiverValue.declarationDescriptor
                        if (compareDescriptors(callElement.project, targetDescriptor, callableDescriptor)) {
                            assert(originalReceiverInfo != null) { "No original receiver info provided: " + functionUsageInfo.declaration.text }
                            result.add(JetImplicitThisToParameterUsage(callElement, originalReceiverInfo!!, functionUsageInfo))
                        }
                        else {
                            result.add(JetImplicitThisUsage(callElement, targetDescriptor))
                        }
                    }

                    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, context: BindingContext): Void? {
                        val resolvedCall = expression.getResolvedCall(context) ?: return null

                        val resultingDescriptor = resolvedCall.resultingDescriptor
                        if (resultingDescriptor is ReceiverParameterDescriptor) {
                            processExplicitThis(expression, resultingDescriptor)
                            return null
                        }

                        var receiverValue = resolvedCall.extensionReceiver
                        if (!receiverValue.exists()) {
                            receiverValue = resolvedCall.dispatchReceiver
                        }
                        if (receiverValue is ThisReceiver) {
                            processImplicitThis(resolvedCall.call.callElement, receiverValue)
                        }

                        return null
                    }
                })
    }

    private fun findSAMUsages(changeInfo: ChangeInfo, result: MutableSet<UsageInfo>) {
        val method = changeInfo.method
        if (!method.isTrueJavaMethod()) return
        method as PsiMethod

        if (method.containingClass == null) return

        val containingDescriptor = method.getJavaMethodDescriptor()?.containingDeclaration as? JavaClassDescriptor ?: return
        if (containingDescriptor.functionTypeForSamInterface == null) return
        val samClass = method.containingClass ?: return

        for (ref in ReferencesSearch.search(samClass)) {
            if (ref !is KtSimpleNameReference) continue

            val callee = ref.expression
            val callExpression = callee.getNonStrictParentOfType<KtCallExpression>() ?: continue
            if (callExpression.calleeExpression !== callee) continue

            val arguments = callExpression.valueArguments
            if (arguments.size != 1) continue

            val argExpression = arguments[0].getArgumentExpression()
            if (argExpression !is KtFunctionLiteralExpression) continue

            val context = callExpression.analyze(BodyResolveMode.FULL)

            val functionLiteral = argExpression.functionLiteral
            val functionDescriptor = context.get(BindingContext.FUNCTION, functionLiteral)
            assert(functionDescriptor != null) { "No descriptor for " + functionLiteral.text }

            val samCallType = context.getType(callExpression) ?: continue

            result.add(DeferredJavaMethodOverrideOrSAMUsage(functionLiteral, functionDescriptor!!, samCallType))
        }
    }

    private fun findKotlinOverrides(changeInfo: ChangeInfo, result: MutableSet<UsageInfo>) {
        val method = changeInfo.method
        if (!method.isTrueJavaMethod()) return

        for (overridingMethod in OverridingMethodsSearch.search(method as PsiMethod)) {
            val unwrappedElement = overridingMethod.namedUnwrappedElement as? KtNamedFunction ?: continue
            val functionDescriptor = unwrappedElement.resolveToDescriptorIfAny() as? FunctionDescriptor ?: continue
            result.add(DeferredJavaMethodOverrideOrSAMUsage(unwrappedElement, functionDescriptor, null))
            findDeferredUsagesOfParameters(changeInfo, result, unwrappedElement, functionDescriptor)
        }
    }

    private fun findKotlinCallers(changeInfo: JavaChangeInfo, result: MutableSet<UsageInfo>) {
        val method = changeInfo.method
        if (!method.isTrueJavaMethod()) return

        for (primaryCaller in changeInfo.methodsToPropagateParameters) {
            addDeferredCallerIfPossible(result, primaryCaller)
            for (overridingCaller in OverridingMethodsSearch.search(primaryCaller)) {
                addDeferredCallerIfPossible(result, overridingCaller)
            }
        }
    }

    private fun addDeferredCallerIfPossible(result: MutableSet<UsageInfo>, overridingCaller: PsiMethod) {
        val unwrappedElement = overridingCaller.namedUnwrappedElement
        if (unwrappedElement is KtFunction || unwrappedElement is KtClass) {
            result.add(DeferredJavaMethodKotlinCallerUsage(unwrappedElement as KtNamedDeclaration))
        }
    }

    private fun findDeferredUsagesOfParameters(
            changeInfo: ChangeInfo,
            result: MutableSet<UsageInfo>,
            function: KtNamedFunction,
            functionDescriptor: FunctionDescriptor) {
        val functionInfoForParameters = JetCallableDefinitionUsage<PsiElement>(function, functionDescriptor, null, null)
        val oldParameters = function.valueParameters
        val parameters = changeInfo.newParameters
        for ((paramIndex, parameterInfo) in parameters.withIndex()) {
            if (!(parameterInfo.oldIndex >= 0 && parameterInfo.oldIndex < oldParameters.size)) continue

            val oldParam = oldParameters[parameterInfo.oldIndex]
            val oldParamName = oldParam.name

            if (!(oldParamName != null && oldParamName != parameterInfo.name)) continue

            for (reference in ReferencesSearch.search(oldParam, oldParam.useScope)) {
                val element = reference.element
                // Usages in named arguments of the calls usage will be changed when the function call is changed
                if (!((element is KtSimpleNameExpression || element is KDocName) && element.parent !is KtValueArgumentName)) continue

                result.add(
                        object : JavaMethodDeferredKotlinUsage<KtElement>(element as KtElement) {
                            override fun resolve(javaMethodChangeInfo: JetChangeInfo): JavaMethodKotlinUsageWithDelegate<KtElement> {
                                return object : JavaMethodKotlinUsageWithDelegate<KtElement>(element as KtElement, javaMethodChangeInfo) {
                                    override val delegateUsage: JetUsageInfo<KtElement>
                                        get() = JetParameterUsage(element as KtElement,
                                                                  this.javaMethodChangeInfo.newParameters[paramIndex],
                                                                  functionInfoForParameters)
                                }
                            }
                        }
                )
            }
        }
    }

    override fun findConflicts(info: ChangeInfo, refUsages: Ref<Array<UsageInfo>>): MultiMap<PsiElement, String> {
        val result = MultiMap<PsiElement, String>()

        // Delete OverriderUsageInfo and CallerUsageInfo for Kotlin declarations since they can't be processed correctly
        // TODO (OverriderUsageInfo only): Drop when OverriderUsageInfo.getElement() gets deleted
        val usageInfos = refUsages.get()
        val adjustedUsages = usageInfos.filterNot { getOverriderOrCaller(it) is KtLightMethod }
        if (adjustedUsages.size < usageInfos.size) {
            refUsages.set(adjustedUsages.toTypedArray())
        }

        if (info !is JetChangeInfo) return result

        val parameterNames = HashSet<String>()
        val function = info.method
        val element = function
        val bindingContext = (element as KtElement).analyze(BodyResolveMode.FULL)
        val oldDescriptor = info.originalBaseFunctionDescriptor
        val containingDeclaration = oldDescriptor.containingDeclaration

        val parametersScope = when {
            oldDescriptor is ConstructorDescriptor && containingDeclaration is ClassDescriptorWithResolutionScopes ->
                containingDeclaration.scopeForInitializerResolution
            function is KtFunction ->
                function.getBodyScope(bindingContext)
            else ->
                null
        }

        val callableScope = oldDescriptor.getContainingScope()

        val kind = info.kind
        if (!kind.isConstructor && callableScope != null && !info.newName.isEmpty()) {
            val newName = Name.identifier(info.newName)
            val conflicts = if (oldDescriptor is FunctionDescriptor)
                callableScope.getAllAccessibleFunctions(newName)
            else
                callableScope.getAllAccessibleVariables(newName)
            for (conflict in conflicts) {
                if (conflict === oldDescriptor) continue

                val conflictElement = DescriptorToSourceUtils.descriptorToDeclaration(conflict)
                if (conflictElement === info.method) continue

                if (conflict.valueParameters.map { it.type } == oldDescriptor.valueParameters.map { it.type }) {
                    result.putValue(conflictElement, "Function already exists: '" + DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(conflict) + "'")
                    break
                }
            }
        }

        for (parameter in info.getNonReceiverParameters()) {
            val valOrVar = parameter.valOrVar
            val parameterName = parameter.name

            if (!parameterNames.add(parameterName)) {
                result.putValue(element, "Duplicating parameter '$parameterName'")
            }

            if (parametersScope != null) {
                if (kind === JetMethodDescriptor.Kind.PRIMARY_CONSTRUCTOR && valOrVar !== JetValVar.None) {
                    for (property in parametersScope.getVariablesFromImplicitReceivers(Name.identifier(parameterName))) {
                        val propertyDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(property) ?: continue
                        if (propertyDeclaration.parent !is KtParameterList) {
                            result.putValue(propertyDeclaration, "Duplicating property '$parameterName'")
                            break
                        }
                    }
                }
                else if (function is KtFunction) {
                    for (variable in parametersScope.getContributedVariables(Name.identifier(parameterName), NoLookupLocation.FROM_IDE)) {
                        if (variable is ValueParameterDescriptor) continue
                        val conflictElement = DescriptorToSourceUtils.descriptorToDeclaration(variable)
                        result.putValue(conflictElement, "Duplicating local variable '$parameterName'")
                    }
                }
            }
        }

        val newReceiverInfo = info.receiverParameterInfo
        val originalReceiverInfo = info.methodDescriptor.receiver
        if (function is KtCallableDeclaration && newReceiverInfo != originalReceiverInfo) {
            findReceiverIntroducingConflicts(result, function, newReceiverInfo)
            findInternalExplicitReceiverConflicts(refUsages.get(), result, originalReceiverInfo)
            findThisLabelConflicts(refUsages, result, info, function)
        }

        for (usageInfo in usageInfos) {
            if (usageInfo !is KotlinCallerUsage) continue
            val callerDescriptor = usageInfo.element?.resolveToDescriptorIfAny() ?: continue
            findParameterDuplicationInCaller(result, info, usageInfo.element!!, callerDescriptor)
        }

        return result
    }

    private fun findParameterDuplicationInCaller(
            result: MultiMap<PsiElement, String>,
            changeInfo: JetChangeInfo,
            caller: KtNamedDeclaration,
            callerDescriptor: DeclarationDescriptor) {
        val valueParameters = caller.getValueParameters()
        val existingParameters = valueParameters.toMapBy { it.name }
        val signature = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(callerDescriptor)
        for (parameterInfo in changeInfo.getNonReceiverParameters()) {
            if (!(parameterInfo.isNewParameter)) continue

            val name = parameterInfo.name
            val parameter = existingParameters[name] ?: continue

            result.putValue(parameter, "There is already a parameter '$name' in $signature. It will conflict with the new parameter.")
        }
    }

    private fun findThisLabelConflicts(
            refUsages: Ref<Array<UsageInfo>>,
            result: MultiMap<PsiElement, String>,
            changeInfo: JetChangeInfo,
            callable: KtCallableDeclaration) {
        val psiFactory = KtPsiFactory(callable.project)
        for (usageInfo in refUsages.get()) {
            if (usageInfo !is JetParameterUsage) continue

            val newExprText = usageInfo.getReplacementText(changeInfo)
            if (!newExprText.startsWith("this")) continue

            if (usageInfo.element is KDocName) continue // TODO support converting parameter to receiver in KDoc

            val originalExpr = usageInfo.element as? KtExpression ?: continue
            val bindingContext = originalExpr.analyze(BodyResolveMode.FULL)
            val scope = originalExpr.getResolutionScope(bindingContext, originalExpr.getResolutionFacade())

            val newExpr = psiFactory.createExpression(newExprText) as KtThisExpression

            val newContext = newExpr.analyzeInContext(scope, originalExpr)

            val labelExpr = newExpr.getTargetLabel()
            if (labelExpr != null && newContext.get(BindingContext.AMBIGUOUS_LABEL_TARGET, labelExpr) != null) {
                result.putValue(
                        originalExpr,
                        "Parameter reference can't be safely replaced with " + newExprText + " since " + labelExpr.text + " is ambiguous in this context")
                continue
            }

            val thisTarget = newContext.get(BindingContext.REFERENCE_TARGET, newExpr.instanceReference)
            val thisTargetPsi = (thisTarget as? DeclarationDescriptorWithSource)?.source?.getPsi()
            if (thisTargetPsi != null && callable.isAncestor(thisTargetPsi, true)) {
                result.putValue(
                        originalExpr,
                        "Parameter reference can't be safely replaced with $newExprText since target function can't be referenced in this context")
            }
        }
    }

    private fun findInternalExplicitReceiverConflicts(
            usages: Array<UsageInfo>,
            result: MultiMap<PsiElement, String>,
            originalReceiverInfo: JetParameterInfo?
    ) {
        if (originalReceiverInfo != null) return

        for (usageInfo in usages) {
            if (!(usageInfo is JetFunctionCallUsage || usageInfo is JetPropertyCallUsage)) continue

            val callElement = usageInfo.element as? KtElement ?: continue

            val parent = callElement.parent
            if (parent is KtQualifiedExpression && parent.selectorExpression === callElement) {
                val message = "Explicit receiver is already present in call element: " + CommonRefactoringUtil.htmlEmphasize(parent.text)
                result.putValue(callElement, message)
            }
        }
    }

    private fun findReceiverIntroducingConflicts(
            result: MultiMap<PsiElement, String>,
            callable: PsiElement,
            newReceiverInfo: JetParameterInfo?) {
        if (newReceiverInfo != null && (callable is KtNamedFunction) && callable.bodyExpression != null) {
            val noReceiverRefToContext = KotlinFileReferencesResolver.resolve(callable, true, true).filter {
                val resolvedCall = it.key.getResolvedCall(it.value)
                resolvedCall != null && !resolvedCall.dispatchReceiver.exists() && !resolvedCall.extensionReceiver.exists()
            }

            val psiFactory = KtPsiFactory(callable.project)
            val tempFile = (callable.containingFile as KtFile).createTempCopy { it }
            val functionWithReceiver = tempFile.findElementAt(callable.textOffset)?.getNonStrictParentOfType<KtNamedFunction>() ?: return
            val receiverTypeRef = psiFactory.createType(newReceiverInfo.currentTypeText)
            functionWithReceiver.setReceiverTypeReference(receiverTypeRef)
            val newContext = functionWithReceiver.bodyExpression!!.analyze(BodyResolveMode.FULL)

            val originalOffset = callable.bodyExpression!!.textOffset
            val newBody = functionWithReceiver.bodyExpression ?: return
            for ((originalRef, originalContext) in noReceiverRefToContext.entries) {
                val newRef = newBody
                        .findElementAt(originalRef.textOffset - originalOffset)
                        ?.getNonStrictParentOfType<KtReferenceExpression>()
                val newResolvedCall = newRef.getResolvedCall(newContext)
                if (newResolvedCall == null || newResolvedCall.extensionReceiver.exists() || newResolvedCall.dispatchReceiver.exists()) {
                    val descriptor = originalRef.getResolvedCall(originalContext)!!.candidateDescriptor
                    val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(callable.project, descriptor)
                    val prefix = if (declaration != null) RefactoringUIUtil.getDescription(declaration, true) else originalRef.text
                    result.putValue(originalRef, prefix.capitalize() + " will no longer be accessible after signature change")
                }
            }
        }
    }

    private fun isJavaMethodUsage(usageInfo: UsageInfo): Boolean {
        // MoveRenameUsageInfo corresponds to non-Java usage of Java method
        return usageInfo is JavaMethodDeferredKotlinUsage<*> || usageInfo is MoveRenameUsageInfo
    }

    private fun createReplacementUsage(
            originalUsageInfo: UsageInfo,
            javaMethodChangeInfo: JetChangeInfo,
            allUsages: Array<UsageInfo>
    ): UsageInfo? {
        if (originalUsageInfo is JavaMethodDeferredKotlinUsage<*>) return originalUsageInfo.resolve(javaMethodChangeInfo)

        val callElement = PsiTreeUtil.getParentOfType(originalUsageInfo.element, KtCallElement::class.java) ?: return null
        val refTarget = originalUsageInfo.reference?.resolve()
        return JavaMethodKotlinCallUsage(callElement, javaMethodChangeInfo, refTarget != null && refTarget.isCaller(allUsages))
    }

    private class NullabilityPropagator(baseMethod: PsiMethod) {
        private val nullManager: NullableNotNullManager
        private val javaPsiFacade: JavaPsiFacade
        private val javaCodeStyleManager: JavaCodeStyleManager
        private val methodAnnotation: PsiAnnotation?
        private val parameterAnnotations: Array<PsiAnnotation?>

        init {
            val project = baseMethod.project
            this.nullManager = NullableNotNullManager.getInstance(project)
            this.javaPsiFacade = JavaPsiFacade.getInstance(project)
            this.javaCodeStyleManager = JavaCodeStyleManager.getInstance(project)

            this.methodAnnotation = getNullabilityAnnotation(baseMethod)
            this.parameterAnnotations = baseMethod.parameterList.parameters.map { getNullabilityAnnotation(it) }.toTypedArray()
        }

        private fun getNullabilityAnnotation(element: PsiModifierListOwner): PsiAnnotation? {
            val nullAnnotation = nullManager.getNullableAnnotation(element, false)
            val notNullAnnotation = nullManager.getNotNullAnnotation(element, false)
            if ((nullAnnotation == null) == (notNullAnnotation == null)) return null
            return nullAnnotation ?: notNullAnnotation
        }

        private fun addNullabilityAnnotationIfApplicable(element: PsiModifierListOwner, annotation: PsiAnnotation?) {
            val nullableAnnotation = nullManager.getNullableAnnotation(element, false)
            val notNullAnnotation = nullManager.getNotNullAnnotation(element, false)

            if (notNullAnnotation != null && nullableAnnotation == null && element is PsiMethod) return

            val annotationQualifiedName = annotation?.qualifiedName
            if (annotationQualifiedName != null
                && javaPsiFacade.findClass(annotationQualifiedName, element.resolveScope) == null) return

            notNullAnnotation?.delete()
            nullableAnnotation?.delete()

            if (annotationQualifiedName == null) return

            val modifierList = element.modifierList
            if (modifierList != null) {
                modifierList.addAnnotation(annotationQualifiedName)
                javaCodeStyleManager.shortenClassReferences(element)
            }
        }

        fun processMethod(currentMethod: PsiMethod) {
            val currentParameters = currentMethod.parameterList.parameters
            addNullabilityAnnotationIfApplicable(currentMethod, methodAnnotation)
            for (i in parameterAnnotations.indices) {
                addNullabilityAnnotationIfApplicable(currentParameters[i], parameterAnnotations[i])
            }
        }
    }

    private fun isOverriderOrCaller(usage: UsageInfo) = usage is OverriderUsageInfo || usage is CallerUsageInfo

    private fun getOverriderOrCaller(usage: UsageInfo): PsiMethod? {
        if (usage is OverriderUsageInfo) return usage.overridingMethod
        if (usage is CallerUsageInfo) {
            val element = usage.element
            return if (element is PsiMethod) element else null
        }
        return null
    }

    override fun processUsage(changeInfo: ChangeInfo, usageInfo: UsageInfo, beforeMethodChange: Boolean, usages: Array<UsageInfo>): Boolean {
        val method = changeInfo.method
        val isJavaMethodUsage = isJavaMethodUsage(usageInfo)

        if (usageInfo is KotlinWrapperForJavaUsageInfos) {
            val javaChangeInfos = (changeInfo as JetChangeInfo).getOrCreateJavaChangeInfos()
            assert(javaChangeInfos != null) { "JavaChangeInfo not found: " + method.text }

            javaChangeInfos!!.firstOrNull {
                changeInfo.originalToCurrentMethods[usageInfo.javaChangeInfo.method] == it.method
            }?.let { javaChangeInfo ->
                val nullabilityPropagator = NullabilityPropagator(javaChangeInfo.method)
                val javaUsageInfos = usageInfo.javaUsageInfos
                val processors = ChangeSignatureUsageProcessor.EP_NAME.extensions

                for (usage in javaUsageInfos) {
                    if (isOverriderOrCaller(usage) && beforeMethodChange) continue
                    for (processor in processors) {
                        if (processor is JetChangeSignatureUsageProcessor) continue
                        if (isOverriderOrCaller(usage)) {
                            processor.processUsage(javaChangeInfo, usage, true, javaUsageInfos)
                        }
                        if (processor.processUsage(javaChangeInfo, usage, beforeMethodChange, javaUsageInfos)) break
                    }
                    if (usage is OverriderUsageInfo && usage.isOriginalOverrider) {
                        val overridingMethod = usage.overridingMethod
                        if (overridingMethod != null && overridingMethod !is KtLightMethod) {
                            nullabilityPropagator.processMethod(overridingMethod)
                        }
                    }
                }
            }
        }

        if (beforeMethodChange) {
            if (method !is PsiMethod || initializedOriginalDescriptor) return true

            val descriptorWrapper = usages.firstIsInstanceOrNull<OriginalJavaMethodDescriptorWrapper>()
            if (descriptorWrapper == null || descriptorWrapper.originalJavaMethodDescriptor != null) return true

            val methodDescriptor = method.getJavaMethodDescriptor()
            assert(methodDescriptor != null)
            descriptorWrapper.originalJavaMethodDescriptor = JetChangeSignatureData(methodDescriptor!!, method, listOf(methodDescriptor))

            // This change info is used as a placeholder before primary method update
            // It gets replaced with real change info afterwards
            val dummyChangeInfo = DummyJetChangeInfo(changeInfo.method, descriptorWrapper.originalJavaMethodDescriptor!!)
            for (i in usages.indices) {
                val oldUsageInfo = usages[i]
                if (!isJavaMethodUsage(oldUsageInfo)) continue

                val newUsageInfo = createReplacementUsage(oldUsageInfo, dummyChangeInfo, usages)
                if (newUsageInfo != null) {
                    usages[i] = newUsageInfo
                }
            }

            initializedOriginalDescriptor = true

            return true
        }

        val element = usageInfo.element ?: return false

        if (usageInfo is JavaMethodKotlinUsageWithDelegate<*>) {
            // Do not call getOriginalJavaMethodDescriptorWrapper() on each usage to avoid O(usage_count^2) performance
            if (usageInfo.javaMethodChangeInfo is DummyJetChangeInfo) {
                val descriptorWrapper = usages.firstIsInstanceOrNull<OriginalJavaMethodDescriptorWrapper>()
                val methodDescriptor = (descriptorWrapper?.originalJavaMethodDescriptor) ?: return true

                val javaMethodChangeInfo = changeInfo.toJetChangeInfo(methodDescriptor)
                for (info in usages) {
                    (info as? JavaMethodKotlinUsageWithDelegate<*>)?.javaMethodChangeInfo = javaMethodChangeInfo
                }
            }

            return usageInfo.processUsage(usages)
        }

        if (usageInfo is MoveRenameUsageInfo && isJavaMethodUsage) {
            val callee = PsiTreeUtil.getParentOfType(usageInfo.element, KtSimpleNameExpression::class.java, false)
            val ref = callee?.mainReference
            if (ref is KtSimpleNameReference) {
                ref.handleElementRename((method as PsiMethod).name)
                return true
            }

            return false
        }

        @Suppress("UNCHECKED_CAST")
        return (usageInfo as? JetUsageInfo<PsiElement>)?.processUsage(changeInfo as JetChangeInfo, element, usages) ?: false
    }

    override fun processPrimaryMethod(changeInfo: ChangeInfo): Boolean {
        if (changeInfo !is JetChangeInfo) return false

        for (primaryFunction in changeInfo.methodDescriptor.primaryCallables) {
            primaryFunction.processUsage(changeInfo, primaryFunction.declaration, UsageInfo.EMPTY_ARRAY)
        }
        changeInfo.primaryMethodUpdated()
        return true
    }

    override fun shouldPreviewUsages(changeInfo: ChangeInfo, usages: Array<UsageInfo>) = false

    override fun setupDefaultValues(changeInfo: ChangeInfo, refUsages: Ref<Array<UsageInfo>>, project: Project) = true

    override fun registerConflictResolvers(
            snapshots: List<ResolveSnapshotProvider.ResolveSnapshot>,
            resolveSnapshotProvider: ResolveSnapshotProvider,
            usages: Array<UsageInfo>, changeInfo: ChangeInfo) {
    }
}
