/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

private val JAVA_LANG_CLASS_FQ_NAME = FqName("java.lang.Class")

private fun KotlinType.isJClass(): Boolean {
    val expressionTypeFqName = constructor.declarationDescriptor?.fqNameSafe ?: return false
    return expressionTypeFqName == JAVA_LANG_CLASS_FQ_NAME
}

class ConvertClassToKClassFix(element: KtDotQualifiedExpression, type: KotlinType) :
    KotlinQuickFixAction<KtDotQualifiedExpression>(element) {
    private val isApplicable: Boolean = run {
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        val expressionType = bindingContext.getType(element) ?: return@run false
        if (!expressionType.isJClass()) return@run false

        val children = element.children
        if (children.size != 2) return@run false

        val firstChild = children.first() as? KtExpression ?: return@run false
        val firstChildType = bindingContext.getType(firstChild) ?: return@run false

        return@run firstChildType.isSubtypeOf(type)
    }

    override fun getText() = element?.let { KotlinBundle.message("remove.0", it.children.lastOrNull()?.text.toString()) } ?: ""
    override fun getFamilyName() = KotlinBundle.message("remove.conversion.from.kclass.to.class")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile) = isApplicable

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        element.replace(element.firstChild)
    }

    companion object Factory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val casted = Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.cast(diagnostic)

            val expectedClassDescriptor = casted.a.constructor.declarationDescriptor as? ClassDescriptor ?: return emptyList()
            if (!KotlinBuiltIns.isKClass(expectedClassDescriptor)) return emptyList()

            val element = casted.psiElement.parent as? KtDotQualifiedExpression ?: return emptyList()
            return listOf(ConvertClassToKClassFix(element, casted.a))
        }
    }
}