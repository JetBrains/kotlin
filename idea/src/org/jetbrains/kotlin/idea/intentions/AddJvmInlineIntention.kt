/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class AddJvmInlineIntention : SelfTargetingRangeIntention<KtClass>(
    KtClass::class.java,
    KotlinBundle.lazyMessage("add.jvminline.annotation")
), HighPriorityAction {
    override fun applyTo(element: KtClass, editor: Editor?) {
        runWriteAction {
            element.addAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME)
        }
    }

    override fun applicabilityRange(element: KtClass): TextRange? {
        if (!element.isValue()) return null

        val modifier = element.modifierList?.getModifier(KtTokens.VALUE_KEYWORD) ?: return null
        return modifier.textRange
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val valueModifier = ErrorsJvm.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION.cast(diagnostic)
            val modifierListOwner = valueModifier.psiElement.getParentOfType<KtModifierListOwner>(strict = true) ?: return null
            return if (valueModifier != null) AddJvmInlineIntention() else null
        }
    }
}