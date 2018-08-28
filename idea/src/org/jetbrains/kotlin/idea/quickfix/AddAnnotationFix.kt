/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

open class AddAnnotationFix(
    element: KtDeclaration,
    private val annotationFqName: FqName,
    private val suffix: String = "",
    private val argumentClassFqName: FqName? = null
) : KotlinQuickFixAction<KtDeclaration>(element) {
    override fun getText(): String {
        val argumentsAsString = argumentClassFqName?.shortName()?.let { "($it::class)" } ?: ""
        return "Add '@${annotationFqName.shortName()}$argumentsAsString' annotation$suffix"
    }

    override fun getFamilyName(): String = "Add annotation"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val factory = KtPsiFactory(project)
        val annotationInnerText = argumentClassFqName?.let { "$it::class" }
        val annotationText = when (annotationInnerText) {
            null -> "@${annotationFqName.asString()}"
            else -> "@${annotationFqName.asString()}($annotationInnerText)"
        }
        val addedAnnotation = element?.addAnnotationEntry(factory.createAnnotationEntry(annotationText))
        addedAnnotation?.let { ShortenReferences.DEFAULT.process(it) }
    }
}