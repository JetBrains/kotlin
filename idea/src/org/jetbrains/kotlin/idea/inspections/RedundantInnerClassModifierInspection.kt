/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class RedundantInnerClassModifierInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classVisitor(fun(targetClass) {
        val innerModifier = targetClass.modifierList?.getModifier(KtTokens.INNER_KEYWORD) ?: return
        val outerClasses = targetClass.parentsOfType<KtClass>().dropWhile { it == targetClass }.toSet()
        if (outerClasses.isEmpty() || outerClasses.any { it.isLocal || it.isInner() }) return
        if (targetClass.hasOuterClassMemberReference(outerClasses)) return
        holder.registerProblem(
            innerModifier,
            KotlinBundle.message("inspection.redundant.inner.class.modifier.descriptor"),
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            IntentionWrapper(
                RemoveModifierFix(targetClass, KtTokens.INNER_KEYWORD, isRedundant = true),
                targetClass.containingFile
            )
        )
    })

    private fun KtClass.hasOuterClassMemberReference(outerClasses: Set<KtClass>): Boolean {
        val targetClass = this
        val outerClassDescriptors by lazy {
            val context = targetClass.analyze(BodyResolveMode.PARTIAL)
            outerClasses.mapNotNull { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as? ClassDescriptor }
        }
        val hasSuperType = outerClasses.any { it.getSuperTypeList() != null }
        return anyDescendantOfType<KtExpression> { expression ->
            when (expression) {
                is KtNameReferenceExpression -> {
                    val reference = expression.mainReference.resolve()
                    val referenceClass = reference?.getStrictParentOfType<KtClass>()
                    if (expression.getStrictParentOfType<KtSuperTypeCallEntry>() != null) {
                        return@anyDescendantOfType reference is KtClass && reference.isInner()
                                || reference is KtPrimaryConstructor && referenceClass?.isInner() == true
                    }
                    if (referenceClass != null) {
                        if (referenceClass == targetClass) return@anyDescendantOfType false
                        if (referenceClass in outerClasses) return@anyDescendantOfType true
                    }
                    if (!hasSuperType) return@anyDescendantOfType false
                    val referenceClassDescriptor = referenceClass?.descriptor as? ClassDescriptor
                        ?: reference?.getStrictParentOfType<PsiClass>()?.getJavaClassDescriptor()
                        ?: (expression.resolveToCall()?.resultingDescriptor as? SyntheticJavaPropertyDescriptor)
                            ?.getMethod?.containingDeclaration as? ClassDescriptor
                        ?: return@anyDescendantOfType false
                    outerClassDescriptors.any { outer -> outer.isSubclassOf(referenceClassDescriptor) }
                }
                is KtThisExpression -> {
                    expression.resolveToCall()?.resultingDescriptor?.returnType?.constructor?.declarationDescriptor in outerClassDescriptors
                }
                else -> false
            }
        }
    }
}
