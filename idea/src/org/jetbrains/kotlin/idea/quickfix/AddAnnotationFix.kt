/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

open class AddAnnotationFix(
    element: KtDeclaration,
    private val annotationFqName: FqName,
    private val suffix: String = ""
) : KotlinQuickFixAction<KtDeclaration>(element) {
    override fun getText(): String = "Add '@${annotationFqName.shortName()}' annotation$suffix"

    override fun getFamilyName(): String = "Add annotation"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.addAnnotation(annotationFqName)
    }
}