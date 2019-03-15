/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME

class AddJvmDefaultAnnotation(declaration: KtCallableDeclaration) : AddAnnotationFix(declaration, JVM_DEFAULT_FQ_NAME) {
    override fun getFamilyName(): String = text

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val psiElement = diagnostic.psiElement
            if (psiElement is KtNamedFunction || psiElement is KtProperty)
                return AddJvmDefaultAnnotation(psiElement as KtCallableDeclaration)
            return null
        }
    }
}
