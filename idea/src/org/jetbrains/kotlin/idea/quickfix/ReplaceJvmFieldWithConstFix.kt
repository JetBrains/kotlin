/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.checkers.ConstModifierChecker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class ReplaceJvmFieldWithConstFix(annotation: KtAnnotationEntry) : KotlinQuickFixAction<KtAnnotationEntry>(annotation) {
    override fun getText(): String = KotlinBundle.message("replace.jvmfield.with.const")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val property = element?.getParentOfType<KtProperty>(false) ?: return
        element?.delete()
        property.addModifier(KtTokens.CONST_KEYWORD)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val annotation = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            val property = annotation.getParentOfType<KtProperty>(false) ?: return null
            val propertyDescriptor = property.descriptor as? PropertyDescriptor ?: return null
            if (!ConstModifierChecker.canBeConst(property, property, propertyDescriptor)) {
                return null
            }

            val initializer = property.initializer ?: return null
            if (!initializer.isConstantExpression()) {
                return null
            }

            return ReplaceJvmFieldWithConstFix(annotation)
        }

        private fun KtExpression.isConstantExpression() =
            ConstantExpressionEvaluator.getConstant(this, analyze(BodyResolveMode.PARTIAL))?.let { !it.usesNonConstValAsConstant } ?: false
    }
}