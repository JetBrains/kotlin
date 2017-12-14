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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.ClassUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.core.OptionalParametersHelper
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isCallee
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.constructors

//TODO: different replacements for property accessors

abstract class DeprecatedSymbolUsageFixBase(
        element: KtSimpleNameExpression,
        val replaceWith: ReplaceWith
) : KotlinQuickFixAction<KtSimpleNameExpression>(element) {

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        if (!super.isAvailable(project, editor, file)) return false
        val strategy = buildUsageReplacementStrategy(element, replaceWith, recheckAnnotation = true)
        return strategy?.createReplacer(element) != null
    }

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val strategy = buildUsageReplacementStrategy(element, replaceWith, recheckAnnotation = false, editor = editor) ?: return
        invoke(strategy, project, editor)
    }

    protected abstract fun invoke(
            replacementStrategy: UsageReplacementStrategy,
            project: Project,
            editor: Editor?)

    companion object {
        fun fetchReplaceWithPattern(descriptor: DeclarationDescriptor, project: Project): ReplaceWith? {
            val annotation = descriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated) ?: return null
            val replaceWithValue = annotation.argumentValue(kotlin.Deprecated::replaceWith.name) as? AnnotationDescriptor ?: return null
            val pattern = replaceWithValue.argumentValue(kotlin.ReplaceWith::expression.name) as? String ?: return null
            if (pattern.isEmpty()) return null
            val importValues = replaceWithValue.argumentValue(kotlin.ReplaceWith::imports.name) as? List<*> ?: return null
            if (importValues.any { it !is StringValue }) return null
            val imports = importValues.map { (it as StringValue).value }

            // should not be available for descriptors with optional parameters if we cannot fetch default values for them (currently for library with no sources)
            if (descriptor is CallableDescriptor &&
                descriptor.valueParameters.any { it.hasDefaultValue() && OptionalParametersHelper.defaultParameterValue(it, project) == null }) return null

            return ReplaceWith(pattern, imports)
        }

        data class Data(
                val nameExpression: KtSimpleNameExpression,
                val replaceWith: ReplaceWith,
                val descriptor: DeclarationDescriptor
        )

        fun extractDataFromDiagnostic(deprecatedDiagnostic: Diagnostic): Data? {
            val psiElement = deprecatedDiagnostic.psiElement

            val nameExpression: KtSimpleNameExpression = when (psiElement) {
                is KtSimpleNameExpression -> psiElement
                is KtConstructorCalleeExpression -> psiElement.constructorReferenceExpression
                else -> null
            } ?: return null

            val descriptor = when (deprecatedDiagnostic.factory) {
                Errors.DEPRECATION -> DiagnosticFactory.cast(deprecatedDiagnostic, Errors.DEPRECATION).a
                Errors.DEPRECATION_ERROR -> DiagnosticFactory.cast(deprecatedDiagnostic, Errors.DEPRECATION_ERROR).a
                Errors.TYPEALIAS_EXPANSION_DEPRECATION ->
                    DiagnosticFactory.cast(deprecatedDiagnostic, Errors.TYPEALIAS_EXPANSION_DEPRECATION).b
                Errors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR ->
                    DiagnosticFactory.cast(deprecatedDiagnostic, Errors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR).b
                else -> throw IllegalStateException("Bad QuickFixRegistrar configuration")
            }

            val replacement = DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(descriptor, nameExpression.project) ?: return null
            return Data(nameExpression, replacement, descriptor)
        }

        private fun buildUsageReplacementStrategy(element: KtSimpleNameExpression, replaceWith: ReplaceWith, recheckAnnotation: Boolean, editor: Editor? = null): UsageReplacementStrategy? {
            val resolutionFacade = element.getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)
            var target = element.mainReference.resolveToDescriptors(bindingContext).singleOrNull() ?: return null

            var replacePatternFromSymbol = DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(target, resolutionFacade.project)
            if (replacePatternFromSymbol == null && target is ConstructorDescriptor) {
                target = target.containingDeclaration
                replacePatternFromSymbol = DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(target, resolutionFacade.project)
            }

            // check that ReplaceWith hasn't changed
            if (recheckAnnotation && replacePatternFromSymbol != replaceWith) return null

            when (target) {
                is CallableDescriptor -> {
                    val resolvedCall = element.getResolvedCall(bindingContext) ?: return null
                    if (!resolvedCall.isReallySuccess()) return null
                    val replacement = ReplaceWithAnnotationAnalyzer.analyzeCallableReplacement(replaceWith, target, resolutionFacade) ?: return null
                    return CallableUsageReplacementStrategy(replacement, inlineSetter = false)
                }

                is ClassifierDescriptorWithTypeParameters -> {
                    val replacementType = ReplaceWithAnnotationAnalyzer.analyzeClassifierReplacement(replaceWith, target, resolutionFacade)
                    return when {
                        replacementType != null -> {
                            if (editor != null) {
                                val typeAlias = element
                                        .getStrictParentOfType<KtUserType>()
                                        ?.getStrictParentOfType<KtTypeReference>()
                                        ?.getStrictParentOfType<KtTypeAlias>()
                                if (typeAlias != null) {
                                    val usedConstructorWithOwnReplaceWith = usedConstructorsWithOwnReplaceWith(
                                            element.project, target, typeAlias)

                                    if (usedConstructorWithOwnReplaceWith != null) {
                                        val constructorStr = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(usedConstructorWithOwnReplaceWith)
                                        HintManager.getInstance().showErrorHint(
                                                editor,
                                                "There is own 'ReplaceWith' on '$constructorStr' that is used through this alias. " +
                                                "Please replace usages first.")
                                        return null
                                    }
                                }
                            }

                            //TODO: check that it's really resolved and is not an object otherwise it can be expression as well
                            ClassUsageReplacementStrategy(replacementType, null, element.project)
                        }
                        target is ClassDescriptor -> {
                            val constructor = target.unsubstitutedPrimaryConstructor ?: return null
                            val replacementExpression = ReplaceWithAnnotationAnalyzer.analyzeCallableReplacement(replaceWith, constructor, resolutionFacade) ?: return null
                            ClassUsageReplacementStrategy(null, replacementExpression, element.project)
                        }
                        else -> null
                    }
                }

                else -> return null
            }
        }

        private fun usedConstructorsWithOwnReplaceWith(
                project: Project, classifier: ClassifierDescriptorWithTypeParameters, typeAlias: PsiElement): ConstructorDescriptor? {
            val specialReplaceWithForConstructor = classifier.constructors.filter {
                DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(it, project) != null
            }.toSet()

            if (specialReplaceWithForConstructor.isEmpty()) {
                return null
            }

            val searchAliasConstructorUsagesScope = GlobalSearchScope.allScope(project).restrictToKotlinSources()
            ReferencesSearch.search(typeAlias, searchAliasConstructorUsagesScope).find { reference ->
                val element = reference.element

                if (element is KtSimpleNameExpression && element.isCallee()) {
                    val aliasConstructors = element.resolveMainReferenceToDescriptors().filterIsInstance<TypeAliasConstructorDescriptor>()
                    for (referenceConstructor in aliasConstructors) {
                        if (referenceConstructor.underlyingConstructorDescriptor in specialReplaceWithForConstructor) {
                            return referenceConstructor.underlyingConstructorDescriptor
                        }
                    }
                }

                false
            }

            return null
        }
    }
}
