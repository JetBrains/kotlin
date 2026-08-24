/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes

/**
 * A [KtCodeFragment] whose content is a type reference.
 *
 * Use this when the snippet denotes a type rather than a value, for example when specifying a cast target in a refactoring.
 * Its [content element][getContentElement] is a [KtTypeReference], or `null` if the text could not be parsed as one.
 */
@OptIn(KtImplementationDetail::class)
class KtTypeCodeFragment(
    project: Project,
    name: String,
    text: CharSequence,
    context: PsiElement?
) : KtCodeFragment(project, name, text, null, KtNodeTypes.TYPE_CODE_FRAGMENT, context) {
    override fun getContentElement() = findChildByClass(KtTypeReference::class.java)
}
