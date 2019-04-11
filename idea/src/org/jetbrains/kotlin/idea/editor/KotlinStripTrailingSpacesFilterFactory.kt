/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.PsiBasedStripTrailingSpacesFilter
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class KotlinStripTrailingSpacesFilterFactory : PsiBasedStripTrailingSpacesFilter.Factory() {
    override fun isApplicableTo(language: Language): Boolean {
        return language.`is`(KotlinLanguage.INSTANCE)
    }

    override fun createFilter(document: Document): PsiBasedStripTrailingSpacesFilter {
        return KotlinStripTrailingSpacesFilter(document)
    }

    private class KotlinStripTrailingSpacesFilter(document: Document) : PsiBasedStripTrailingSpacesFilter(document) {
        override fun process(psiFile: PsiFile) {
            psiFile.accept(object : KtTreeVisitorVoid() {
                override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
                    disableRange(expression.textRange, false)
                }
            })
        }
    }
}