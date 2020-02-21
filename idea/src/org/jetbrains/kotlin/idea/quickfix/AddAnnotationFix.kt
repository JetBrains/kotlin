/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

open class AddAnnotationFix(
    element: KtDeclaration,
    private val annotationFqName: FqName,
    private val kind: Kind = Kind.Self,
    private val argumentClassFqName: FqName? = null
) : KotlinQuickFixAction<KtDeclaration>(element) {
    override fun getText(): String {
        val annotationArguments = (argumentClassFqName?.shortName()?.let { "($it::class)" } ?: "")
        val annotationCall = annotationFqName.shortName().asString() + annotationArguments
        return when (kind) {
            Kind.Self -> KotlinBundle.message("fix.add.annotation.text.self", annotationCall)
            is Kind.Declaration -> KotlinBundle.message("fix.add.annotation.text.declaration", annotationCall, kind.name ?: "?")
            is Kind.ContainingClass -> KotlinBundle.message("fix.add.annotation.text.containing.class", annotationCall, kind.name ?: "?")
        }
    }

    override fun getFamilyName(): String = KotlinBundle.message("fix.add.annotation.family")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.addAnnotation(annotationFqName, annotationInnerText = argumentClassFqName?.let { "$it::class" })
    }

    sealed class Kind {
        object Self : Kind()
        class Declaration(val name: String?) : Kind()
        class ContainingClass(val name: String?) : Kind()
    }
}