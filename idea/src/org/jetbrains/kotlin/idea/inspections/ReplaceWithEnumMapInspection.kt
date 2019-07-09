/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.types.typeUtil.isEnum

private val hashMapCreationFqNames = setOf(
    "java.util.HashMap.<init>",
    "kotlin.collections.HashMap.<init>",
    "kotlin.collections.hashMapOf"
)

class ReplaceWithEnumMapInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor(fun(element: KtCallExpression) {
            if (!element.platform.isJvm()) return
            val context = element.analyze()
            val fqName = element.getResolvedCall(context)?.resultingDescriptor?.fqNameUnsafe?.asString() ?: return
            if (!hashMapCreationFqNames.contains(fqName)) return
            if (element.valueArguments.isNotEmpty()) return

            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, element] ?: return
            val firstArgType = expectedType.arguments.firstOrNull()?.type ?: return
            if (!firstArgType.isEnum()) return
            val enumClassName = firstArgType.constructor.declarationDescriptor?.fqNameUnsafe?.asString() ?: return
            holder.registerProblem(element, "Replaceable with EnumMap", ReplaceWithEnumMapFix(enumClassName))
        })
    }

    private class ReplaceWithEnumMapFix(
        private val enumClassName: String
    ) : LocalQuickFix {
        override fun getFamilyName() = name

        override fun getName() = "Replace with EnumMap"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val call = descriptor.psiElement as? KtCallExpression ?: return
            val factory = KtPsiFactory(call)
            val file = call.containingKtFile
            val enumMapDescriptor = file.resolveImportReference(FqName("java.util.EnumMap")).firstOrNull() ?: return
            ImportInsertHelper.getInstance(project).importDescriptor(call.containingKtFile, enumMapDescriptor)
            call.replace(factory.createExpressionByPattern("EnumMap($0::class.java)", enumClassName))
        }
    }
}