/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.WRONG_ANNOTATION_TARGET
import org.jetbrains.kotlin.diagnostics.Errors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.EMPTY
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_CLASSIFIER
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_CONSTRUCTOR
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_EXPRESSION
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_LOCAL_VARIABLE
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_MEMBER_FUNCTION
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_MEMBER_PROPERTY
import org.jetbrains.kotlin.resolve.AnnotationChecker.Companion.TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
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
            if (diagnostic.factory != WRONG_ANNOTATION_TARGET && diagnostic.factory != WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET) {
                return null
            }

            val entry = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            val annotationClass = entry.toAnnotationClass() ?: return null
            if (entry.useSiteTarget != null && entry.getRequiredAnnotationTargets(annotationClass, entry.project).isEmpty()) return null

            return AddAnnotationTargetFix(entry)
        }
    }
}

private fun KtAnnotationEntry.getRequiredAnnotationTargets(annotationClass: KtClass, project: Project): List<KotlinTarget> {
    val requiredTargets = getActualTargetList()
    if (requiredTargets.isEmpty()) return emptyList()

    val searchScope = GlobalSearchScope.allScope(project)
    val otherReferenceRequiredTargets = ReferencesSearch.search(annotationClass, searchScope).mapNotNull { reference ->
        if (reference.element is KtNameReferenceExpression) {
            // Kotlin annotation
            reference.element.getNonStrictParentOfType<KtAnnotationEntry>()?.takeIf { it != this }?.getActualTargetList()
        } else {
            // Java annotation
            (reference.element.parent as? PsiAnnotation)?.getActualTargetList()
        }
    }.flatten().toSet()
    val annotationTargetValueNames = AnnotationTarget.values().map { it.name }
    return (requiredTargets + otherReferenceRequiredTargets).distinct().filter { it.name in annotationTargetValueNames }
}

private fun getActualTargetList(annotated: PsiTarget): AnnotationChecker.Companion.TargetList {
    return when (annotated) {
        is PsiClass -> T_CLASSIFIER
        is PsiMethod ->
            when {
                annotated.isConstructor -> T_CONSTRUCTOR
                else -> T_MEMBER_FUNCTION
            }
        is PsiExpression -> T_EXPRESSION
        is PsiField -> T_MEMBER_PROPERTY(true, false)
        is PsiLocalVariable -> T_LOCAL_VARIABLE
        is PsiParameter -> T_VALUE_PARAMETER_WITHOUT_VAL
        else -> EMPTY
    }
}

private fun PsiAnnotation.getActualTargetList(): List<KotlinTarget> {
    val annotated = parent.parent as? PsiTarget ?: return emptyList()
    return getActualTargetList(annotated).defaultTargets
}

private fun KtAnnotationEntry.getActualTargetList(): List<KotlinTarget> {
    val annotatedElement = getStrictParentOfType<KtModifierList>()?.owner as? KtElement
            ?: getStrictParentOfType<KtAnnotatedExpression>()?.baseExpression
            ?: getStrictParentOfType<KtFile>()
            ?: return emptyList()

    val targetList = AnnotationChecker.getActualTargetList(annotatedElement, null, BindingTraceContext().bindingContext)

    val useSiteTarget = this.useSiteTarget ?: return targetList.defaultTargets
    val annotationUseSiteTarget = useSiteTarget.getAnnotationUseSiteTarget()
    val target = KotlinTarget.USE_SITE_MAPPING[annotationUseSiteTarget] ?: return emptyList()

    if (annotationUseSiteTarget == AnnotationUseSiteTarget.FIELD) {
        if (KotlinTarget.MEMBER_PROPERTY !in targetList.defaultTargets && KotlinTarget.TOP_LEVEL_PROPERTY !in targetList.defaultTargets) {
            return emptyList()
        }
        val property = annotatedElement as? KtProperty
        if (property != null && (LightClassUtil.getLightClassPropertyMethods(property).backingField == null || property.hasDelegate())) {
            return emptyList()
        }
    } else {
        if (target !in with(targetList) { defaultTargets + canBeSubstituted + onlyWithUseSiteTarget }) {
            return emptyList()
        }
    }

    return listOf(target)
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
