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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddAnnotationTargetFix(annotationEntry: KtAnnotationEntry) : KotlinQuickFixAction<KtAnnotationEntry>(annotationEntry) {

    override fun getText() = "Add annotation target"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val annotationEntry = element ?: return

        val annotationClass = annotationEntry.toAnnotationClass() ?: return

        val requiredAnnotationTargets = annotationEntry.getRequiredAnnotationTargets(annotationClass, project)
        if (requiredAnnotationTargets.isEmpty()) return

        val psiFactory = KtPsiFactory(annotationEntry)
        annotationClass.addAnnotationTargets(requiredAnnotationTargets, psiFactory)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        private fun KtAnnotationEntry.toAnnotationClass(): KtClass? {
            val context = analyze(BodyResolveMode.PARTIAL)
            val annotationDescriptor = context[BindingContext.ANNOTATION, this] ?: return null
            val annotationTypeDescriptor = annotationDescriptor.type.constructor.declarationDescriptor ?: return null
            return (DescriptorToSourceUtils.descriptorToDeclaration(annotationTypeDescriptor) as? KtClass)?.takeIf {
                it.isAnnotation() && it.isWritable
            }
        }

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtAnnotationEntry>? {
            val entry = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            if (entry.toAnnotationClass() == null) return null

            return AddAnnotationTargetFix(entry)
        }
    }
}

private fun KtAnnotationEntry.getRequiredAnnotationTargets(annotationClass: KtClass, project: Project): List<KotlinTarget> {
    val requiredTargets = getActualTargetList()
    if (requiredTargets.isEmpty()) return emptyList()

    val searchScope = GlobalSearchScope.allScope(project).restrictToKotlinSources()
    val otherReferenceRequiredTargets = ReferencesSearch.search(annotationClass, searchScope).mapNotNull { reference ->
        reference.element.getNonStrictParentOfType<KtAnnotationEntry>()?.takeIf { it != this }?.getActualTargetList()
    }.flatten().toSet()

    val annotationTargetValueNames = AnnotationTarget.values().map { it.name }
    return (requiredTargets + otherReferenceRequiredTargets).filter { it.name in annotationTargetValueNames }
}

private fun KtAnnotationEntry.getActualTargetList(): List<KotlinTarget> {
    val annotatedElement = getStrictParentOfType<KtModifierList>()?.owner as? KtElement
            ?: getStrictParentOfType<KtAnnotatedExpression>()?.baseExpression
            ?: return emptyList()
    return AnnotationChecker.getDeclarationSiteActualTargetList(annotatedElement, null, BindingTraceContext())
}

private fun KtClass.addAnnotationTargets(annotationTargets: List<KotlinTarget>, psiFactory: KtPsiFactory) {
    val targetAnnotationName = KotlinBuiltIns.FQ_NAMES.target.shortName().asString()

    val targetAnnotationEntry = annotationEntries.find { it.typeReference?.text == targetAnnotationName } ?: run {
        val text = "@$targetAnnotationName${annotationTargets.toArgumentListString()}"
        addAnnotationEntry(psiFactory.createAnnotationEntry(text))
        return
    }
    val valueArgumentList = targetAnnotationEntry.valueArgumentList
    if (valueArgumentList == null) {
        val text = annotationTargets.toArgumentListString()
        targetAnnotationEntry.add(psiFactory.createCallArguments(text))
    } else {
        val arguments = targetAnnotationEntry.valueArguments.mapNotNull { it.getArgumentExpression()?.text }
        for (target in annotationTargets) {
            val text = target.asNameString()
            if (text !in arguments) valueArgumentList.addArgument(psiFactory.createArgument(text))
        }
    }
}

private fun List<KotlinTarget>.toArgumentListString() = joinToString(separator = ", ", prefix = "(", postfix = ")") { it.asNameString() }

private fun KotlinTarget.asNameString() = "${KotlinBuiltIns.FQ_NAMES.annotationTarget.shortName().asString()}.$name"
