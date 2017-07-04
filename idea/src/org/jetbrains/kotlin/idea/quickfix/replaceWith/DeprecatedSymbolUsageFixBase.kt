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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
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
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

//TODO: different replacements for property accessors

abstract class DeprecatedSymbolUsageFixBase(
        element: KtSimpleNameExpression,
        val replaceWith: ReplaceWith
) : KotlinQuickFixAction<KtSimpleNameExpression>(element) {

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        if (!super.isAvailable(project, editor, file)) return false
        val strategy = buildUsageReplacementStrategy(element, replaceWith, recheckAnnotation = true)
        return strategy != null && strategy.createReplacer(element) != null
    }

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val strategy = buildUsageReplacementStrategy(element, replaceWith, recheckAnnotation = false)!!
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

            //TODO: compiler crash here
            /*
                        val nameExpression: JetSimpleNameExpression = when (psiElement) {
                            is JetSimpleNameExpression -> psiElement
                            is JetConstructorCalleeExpression -> psiElement.constructorReferenceExpression
                            else -> null
                        } ?: return null
            */
            val nameExpression: KtSimpleNameExpression = when (psiElement) {
                is KtSimpleNameExpression -> psiElement
                is KtConstructorCalleeExpression -> psiElement.constructorReferenceExpression
                else -> null
            } ?: return null

            val descriptor = DiagnosticFactory.cast(deprecatedDiagnostic, Errors.DEPRECATION, Errors.DEPRECATION_ERROR).a
            val replacement = DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(descriptor, nameExpression.project) ?: return null
            return Data(nameExpression, replacement, descriptor)
        }

        private fun buildUsageReplacementStrategy(element: KtSimpleNameExpression, replaceWith: ReplaceWith, recheckAnnotation: Boolean): UsageReplacementStrategy? {
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
                    return CallableUsageReplacementStrategy(replacement)
                }

                is ClassDescriptor -> {
                    val replacementType = ReplaceWithAnnotationAnalyzer.analyzeClassReplacement(replaceWith, target, resolutionFacade)
                    if (replacementType != null) { //TODO: check that it's really resolved and is not an object otherwise it can be expression as well
                        return ClassUsageReplacementStrategy(replacementType, null, element.project)
                    }
                    else {
                        val constructor = target.unsubstitutedPrimaryConstructor ?: return null
                        val replacementExpression = ReplaceWithAnnotationAnalyzer.analyzeCallableReplacement(replaceWith, constructor, resolutionFacade) ?: return null
                        return ClassUsageReplacementStrategy(null, replacementExpression, element.project)
                    }
                }

                else -> return null
            }
        }
    }
}
