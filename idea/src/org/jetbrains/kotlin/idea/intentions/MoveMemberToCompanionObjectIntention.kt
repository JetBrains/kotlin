/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.move.OuterInstanceReferenceUsageInfo
import org.jetbrains.kotlin.idea.refactoring.move.collectOuterInstanceReferences
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.traverseOuterInstanceReferences
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.util.findCallableMemberBySignature

class MoveMemberToCompanionObjectIntention : SelfTargetingRangeIntention<KtNamedDeclaration>(KtNamedDeclaration::class.java,
                                                                                             "Move to companion object") {
    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty && element !is KtClassOrObject) return null
        if (element is KtEnumEntry) return null
        if (element is KtNamedFunction && element.bodyExpression == null) return null
        if (element is KtNamedFunction && element.valueParameterList == null) return null
        if ((element is KtNamedFunction || element is KtProperty) && element.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return null
        if (element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null
        val containingClass = element.containingClassOrObject as? KtClass ?: return null
        if (containingClass.isLocal || containingClass.isInner()) return null
        return element.nameIdentifier?.textRange
    }

    class JavaUsageInfo(refExpression: PsiReferenceExpression) : UsageInfo(refExpression)

    class ImplicitReceiverUsageInfo(refExpression: KtSimpleNameExpression, val callExpression: KtExpression) : UsageInfo(refExpression)
    class ExplicitReceiverUsageInfo(refExpression: KtSimpleNameExpression, val receiverExpression: KtExpression) : UsageInfo(refExpression)

    private fun getNameSuggestionsForOuterInstance(element: KtNamedDeclaration): List<String> {
        val containingClass = element.containingClassOrObject as KtClass
        val containingClassDescriptor = containingClass.unsafeResolveToDescriptor() as ClassDescriptorWithResolutionScopes
        val companionDescriptor = containingClassDescriptor.companionObjectDescriptor
        val companionMemberScope = (companionDescriptor ?: containingClassDescriptor).scopeForMemberDeclarationResolution
        val validator = CollectingNameValidator(element.getValueParameters().mapNotNull { it.name }) {
            companionMemberScope.getContributedVariables(Name.identifier(it), NoLookupLocation.FROM_IDE).isEmpty()
        }
        return KotlinNameSuggester.suggestNamesByType(containingClassDescriptor.defaultType, validator, "receiver")
    }

    private fun runTemplateForInstanceParam(
            declaration: KtNamedDeclaration,
            nameSuggestions: List<String>,
            editor: Editor?
    ) {
        if (nameSuggestions.isNotEmpty() && editor != null) {
            val restoredElement = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(declaration)
            val restoredParam = restoredElement.getValueParameters().first()

            val paramRefs = ReferencesSearch.search(restoredParam, LocalSearchScope(restoredElement)).toList()

            editor.caretModel.moveToOffset(restoredElement.startOffset)

            val templateBuilder = TemplateBuilderImpl(restoredElement)
            templateBuilder.replaceElement(restoredParam.nameIdentifier!!, "ParamName", ChooseStringExpression(nameSuggestions), true)
            paramRefs.forEach { templateBuilder.replaceElement(it, "ParamName", "ParamRef", false) }
            templateBuilder.run(editor, true)
        }
    }

    private fun moveReceiverToArgumentList(refElement: PsiElement, classFqName: FqName) {
        when (refElement) {
            is PsiReferenceExpression -> {
                val qualifier = refElement.qualifier
                val call = refElement.parent as? PsiMethodCallExpression
                if (call != null && qualifier != null) {
                    val argumentList = call.argumentList
                    argumentList.addBefore(qualifier, argumentList.expressions.firstOrNull())
                }
            }

            is KtSimpleNameExpression -> {
                val call = refElement.parent as? KtCallExpression ?: return
                val psiFactory = KtPsiFactory(refElement)
                val argumentList = call.valueArgumentList
                                   ?: call.addAfter(psiFactory.createCallArguments("()"), call.typeArgumentList ?: refElement) as KtValueArgumentList

                val receiver = call.getQualifiedExpressionForSelector()?.receiverExpression
                val receiverArg = receiver?.let { psiFactory.createArgument(it) }
                                  ?: psiFactory.createArgument(psiFactory.createExpression("this@${classFqName.shortName().asString()}"))
                argumentList.addArgumentBefore(receiverArg, argumentList.arguments.firstOrNull())
            }
        }
    }

    private fun doMove(element: KtNamedDeclaration,
                       externalUsages: SmartList<UsageInfo>,
                       outerInstanceUsages: SmartList<UsageInfo>,
                       editor: Editor?) {
        val project = element.project
        val containingClass = element.containingClassOrObject as KtClass

        val javaCodeStyleManager = JavaCodeStyleManager.getInstance(project)

        val companionObject = containingClass.getOrCreateCompanionObject()
        val companionLightClass = companionObject.toLightClass()!!

        val ktPsiFactory = KtPsiFactory(project)
        val javaPsiFactory = JavaPsiFacade.getInstance(project).elementFactory
        val javaCompanionRef = javaPsiFactory.createReferenceExpression(companionLightClass)
        val ktCompanionRef = ktPsiFactory.createExpression(companionObject.fqName!!.asString())

        val elementsToShorten = SmartList<KtElement>()

        val nameSuggestions: List<String>
        if (outerInstanceUsages.isNotEmpty() && element is KtNamedFunction) {
            val parameterList = element.valueParameterList!!
            val parameters = parameterList.parameters

            val newParamType = (containingClass.unsafeResolveToDescriptor() as ClassDescriptor).defaultType

            nameSuggestions = getNameSuggestionsForOuterInstance(element)

            val newParam = parameterList.addParameterBefore(
                    ktPsiFactory.createParameter("${nameSuggestions.first()}: ${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(newParamType)}"),
                    parameters.firstOrNull()
            )

            val newOuterInstanceRef = ktPsiFactory.createExpression(newParam.name!!)
            for (usage in outerInstanceUsages) {
                when (usage) {
                    is OuterInstanceReferenceUsageInfo.ExplicitThis -> {
                        usage.expression?.replace(newOuterInstanceRef)
                    }

                    is OuterInstanceReferenceUsageInfo.ImplicitReceiver -> {
                        usage.callElement?.let { it.replace(ktPsiFactory.createExpressionByPattern("$0.$1", newOuterInstanceRef, it)) }
                    }
                }
            }
        }
        else {
            nameSuggestions = emptyList()
        }

        val hasInstanceArg = nameSuggestions.isNotEmpty()

        element.removeModifier(KtTokens.OPEN_KEYWORD)
        element.removeModifier(KtTokens.FINAL_KEYWORD)

        val newDeclaration = Mover.Default(element, companionObject)

        for (usage in externalUsages) {
            val usageElement = usage.element ?: continue

            if (hasInstanceArg) {
                moveReceiverToArgumentList(usageElement, containingClass.fqName!!)
            }

            when (usage) {
                is JavaUsageInfo -> {
                    (usageElement as? PsiReferenceExpression)
                            ?.qualifierExpression
                            ?.replace(javaCompanionRef)
                            ?.let { javaCodeStyleManager.shortenClassReferences(it) }
                }

                is ExplicitReceiverUsageInfo -> {
                    elementsToShorten += usage.receiverExpression.replaced(ktCompanionRef)
                }

                is ImplicitReceiverUsageInfo -> {
                    usage.callExpression
                            .let { it.replaced(ktPsiFactory.createExpressionByPattern("$0.$1", ktCompanionRef, it)) }
                            .let {
                                val qualifiedCall = it as KtQualifiedExpression
                                elementsToShorten += qualifiedCall.receiverExpression
                                if (hasInstanceArg) {
                                    elementsToShorten += (qualifiedCall.selectorExpression as KtCallExpression).valueArguments.first()
                                }
                            }
                }
            }
        }

        ShortenReferences { ShortenReferences.Options.ALL_ENABLED }.process(elementsToShorten)

        runTemplateForInstanceParam(newDeclaration, nameSuggestions, editor)
    }

    private fun hasTypeParameterReferences(containingClass: KtClassOrObject, element: KtNamedDeclaration): Boolean {
        val containingClassDescriptor = containingClass.unsafeResolveToDescriptor()
        return element.collectDescendantsOfType<KtTypeReference> {
            val referencedDescriptor = it.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, it]?.constructor?.declarationDescriptor
            referencedDescriptor is TypeParameterDescriptor && referencedDescriptor.containingDeclaration == containingClassDescriptor
        }.isNotEmpty()
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtNamedDeclaration, editor: Editor?) {
        val project = element.project

        val containingClass = element.containingClassOrObject as KtClass

        if (element is KtClassOrObject) {
            val nameSuggestions = if (traverseOuterInstanceReferences(element, true)) getNameSuggestionsForOuterInstance(element) else emptyList()
            val outerInstanceName = nameSuggestions.firstOrNull()
            var movedClass: KtClassOrObject? = null
            val mover = object: Mover {
                override fun invoke(originalElement: KtNamedDeclaration, targetContainer: KtElement): KtNamedDeclaration {
                    return Mover.Default(originalElement, targetContainer).apply { movedClass = this as KtClassOrObject }
                }
            }
            val moveDescriptor = MoveDeclarationsDescriptor(project,
                                                            listOf(element),
                                                            KotlinMoveTargetForCompanion(containingClass),
                                                            MoveDeclarationsDelegate.NestedClass(null, outerInstanceName),
                                                            moveCallback = MoveCallback { runTemplateForInstanceParam(movedClass!!, nameSuggestions, editor) })
            MoveKotlinDeclarationsProcessor(moveDescriptor, mover).run()
            return
        }

        val description = RefactoringUIUtil.getDescription(element, false).capitalize()

        if (HierarchySearchRequest(element, element.useScope, false).searchOverriders().any()) {
            return CommonRefactoringUtil.showErrorHint(project, editor, "$description is overridden by declaration(s) in a subclass", text, null)
        }

        if (hasTypeParameterReferences(containingClass, element)) {
            return CommonRefactoringUtil.showErrorHint(project, editor, "$description references type parameters of the containing class", text, null)
        }

        val externalUsages = SmartList<UsageInfo>()
        val outerInstanceUsages = SmartList<UsageInfo>()
        val conflicts = MultiMap<PsiElement, String>()

        containingClass.companionObjects.firstOrNull()?.let { companion ->
            val companionDescriptor = companion.unsafeResolveToDescriptor() as ClassDescriptor
            val callableDescriptor = element.unsafeResolveToDescriptor() as CallableMemberDescriptor
            companionDescriptor.findCallableMemberBySignature(callableDescriptor)?.let {
                DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)
            }?.let {
                conflicts.putValue(it, "Companion object already contains ${RefactoringUIUtil.getDescription(it, false)}")
            }
        }

        val outerInstanceReferences = collectOuterInstanceReferences(element)
        if (outerInstanceReferences.isNotEmpty()) {
            if (element is KtProperty) {
                conflicts.putValue(element, "Usages of outer class instance inside of property '${element.name}' won't be processed")
            }
            else {
                outerInstanceReferences.filterNotTo(outerInstanceUsages) { it.reportConflictIfAny(conflicts) }
            }
        }

        project.runSynchronouslyWithProgress("Searching for ${element.name}", true) {
            runReadAction {
                ReferencesSearch.search(element).mapNotNullTo(externalUsages) { ref ->
                    when (ref) {
                        is PsiReferenceExpression -> JavaUsageInfo(ref)
                        is KtSimpleNameReference -> {
                            val refExpr = ref.expression
                            if (element.isAncestor(refExpr)) return@mapNotNullTo null
                            val context = refExpr.analyze(BodyResolveMode.PARTIAL)
                            val resolvedCall = refExpr.getResolvedCall(context) ?: return@mapNotNullTo null

                            val callExpression = resolvedCall.call.callElement as? KtExpression ?: return@mapNotNullTo null

                            val extensionReceiver = resolvedCall.extensionReceiver
                            if (extensionReceiver != null && extensionReceiver !is ImplicitReceiver) {
                                conflicts.putValue(callExpression,
                                                   "Calls with explicit extension receiver won't be processed: ${callExpression.text}")
                                return@mapNotNullTo null
                            }

                            val dispatchReceiver = resolvedCall.dispatchReceiver ?: return@mapNotNullTo null
                            if (dispatchReceiver is ExpressionReceiver) {
                                ExplicitReceiverUsageInfo(refExpr, dispatchReceiver.expression)
                            }
                            else {
                                ImplicitReceiverUsageInfo(refExpr, callExpression)
                            }
                        }
                        else -> null
                    }
                }
            }
        }

        project.checkConflictsInteractively(conflicts) {
            runWriteAction { doMove(element, externalUsages, outerInstanceUsages, editor) }
        }
    }
}