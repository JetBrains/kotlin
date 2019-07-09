/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

internal fun PsiElement.isUsageAsAnnotationOrImport(): Boolean {
    val parent = parent

    if (parent is KtUserType) {
        return parent.parent is KtTypeReference &&
                parent.parent.parent is KtConstructorCalleeExpression &&
                parent.parent.parent.parent is KtAnnotationEntry
    }

    return parent is KtDotQualifiedExpression && parent.parent is KtImportDirective
}
