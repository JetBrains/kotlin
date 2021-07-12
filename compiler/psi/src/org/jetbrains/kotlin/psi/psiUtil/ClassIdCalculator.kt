/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*

internal object ClassIdCalculator {
    fun calculateClassId(declaration: KtClassLikeDeclaration): ClassId? {
        var ktFile: KtFile? = null
        var element: PsiElement? = declaration
        val containingClasses = mutableListOf<KtClassLikeDeclaration>()
        while (element != null) {
            when (element) {
                is KtEnumEntry -> {
                    return null
                }
                is KtClassLikeDeclaration -> {
                    containingClasses += element
                }
                is KtObjectLiteralExpression -> {
                    return null
                }
                is KtFile -> {
                    ktFile = element
                    break
                }
                is KtDeclaration -> {
                    return null
                }
            }
            element = element.parent
        }
        if (ktFile == null) return null
        val relativeClassName = FqName.fromSegments(
            containingClasses.reversed().map { containingClass ->
                containingClass.name ?: SpecialNames.NO_NAME_PROVIDED.asString()
            }
        )
        return ClassId(ktFile.packageFqName, relativeClassName, /*local=*/false)
    }
}