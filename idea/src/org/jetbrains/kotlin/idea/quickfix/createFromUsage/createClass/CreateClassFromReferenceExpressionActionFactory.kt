/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

object CreateClassFromReferenceExpressionActionFactory : CreateClassFromUsageFactory<KtSimpleNameExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtSimpleNameExpression? {
        val refExpr = diagnostic.psiElement as? KtSimpleNameExpression ?: return null
        if (refExpr.getNonStrictParentOfType<KtTypeReference>() != null) return null
        return refExpr
    }

    private fun getFullCallExpression(element: KtSimpleNameExpression): KtExpression? {
        return element.parent.let {
            when {
                it is KtCallExpression && it.calleeExpression == element -> return null
                it is KtQualifiedExpression && it.selectorExpression == element -> it
                else -> element
            }
        }
    }

    private fun isQualifierExpected(element: KtSimpleNameExpression) =
        element.isDotReceiver() || ((element.parent as? KtDotQualifiedExpression)?.isDotReceiver() ?: false)

    override fun getPossibleClassKinds(element: KtSimpleNameExpression, diagnostic: Diagnostic): List<ClassKind> {
        fun isEnum(element: PsiElement): Boolean {
            return when (element) {
                is KtClass -> element.isEnum()
                is PsiClass -> element.isEnum
                else -> false
            }
        }

        val name = element.getReferencedName()

        val (context, moduleDescriptor) = element.analyzeAndGetResult()

        val fullCallExpr = getFullCallExpression(element) ?: return Collections.emptyList()

        val inImport = element.getNonStrictParentOfType<KtImportDirective>() != null
        if (inImport || isQualifierExpected(element)) {
            val receiverSelector =
                (fullCallExpr as? KtQualifiedExpression)?.receiverExpression?.getQualifiedElementSelector() as? KtReferenceExpression
            val qualifierDescriptor = receiverSelector?.let { context[BindingContext.REFERENCE_TARGET, it] }

            val targetParents = getTargetParentsByQualifier(element, receiverSelector != null, qualifierDescriptor)
                .ifEmpty { return emptyList() }

            targetParents.forEach {
                if (element.getCreatePackageFixIfApplicable(it) != null) return emptyList()
            }

            if (!name.checkClassName()) return emptyList()

            return ClassKind.values().filter {
                when (it) {
                    ClassKind.ANNOTATION_CLASS -> inImport
                    ClassKind.ENUM_ENTRY -> inImport && targetParents.any { isEnum(it) }
                    else -> true
                }
            }
        }
        val parent = element.parent
        if (parent is KtClassLiteralExpression && parent.receiverExpression == element) {
            return listOf(ClassKind.PLAIN_CLASS, ClassKind.ENUM_CLASS, ClassKind.INTERFACE, ClassKind.ANNOTATION_CLASS, ClassKind.OBJECT)
        }

        if (fullCallExpr.getAssignmentByLHS() != null) return Collections.emptyList()

        val call = element.getCall(context) ?: return Collections.emptyList()
        val targetParents = getTargetParentsByCall(call, context).ifEmpty { return emptyList() }
        if (isInnerClassExpected(call)) return Collections.emptyList()

        val allKinds = listOf(ClassKind.OBJECT, ClassKind.ENUM_ENTRY)

        val expectedType = fullCallExpr.guessTypeForClass(context, moduleDescriptor)

        return allKinds.filter { classKind ->
            targetParents.any { targetParent ->
                (expectedType == null || getClassKindFilter(expectedType, targetParent)(classKind)) && when (classKind) {
                    ClassKind.OBJECT -> !isEnum(targetParent)
                    ClassKind.ENUM_ENTRY -> isEnum(targetParent)
                    else -> false
                }
            }
        }
    }

    override fun extractFixData(element: KtSimpleNameExpression, diagnostic: Diagnostic): ClassInfo? {
        val name = element.getReferencedName()

        val (context, moduleDescriptor) = element.analyzeAndGetResult()

        val fullCallExpr = getFullCallExpression(element) ?: return null

        if (element.isInImportDirective() || isQualifierExpected(element)) {
            val receiverSelector =
                (fullCallExpr as? KtQualifiedExpression)?.receiverExpression?.getQualifiedElementSelector() as? KtReferenceExpression
            val qualifierDescriptor = receiverSelector?.let { context[BindingContext.REFERENCE_TARGET, it] }

            val targetParents = getTargetParentsByQualifier(element, receiverSelector != null, qualifierDescriptor)
                .ifEmpty { return null }

            return ClassInfo(
                name = name,
                targetParents = targetParents,
                expectedTypeInfo = TypeInfo.Empty
            )
        }

        val call = element.getCall(context) ?: return null
        val targetParents = getTargetParentsByCall(call, context).ifEmpty { return null }

        val expectedTypeInfo = fullCallExpr.guessTypeForClass(context, moduleDescriptor)?.toClassTypeInfo() ?: TypeInfo.Empty

        return ClassInfo(
            name = name,
            targetParents = targetParents,
            expectedTypeInfo = expectedTypeInfo
        )
    }
}
