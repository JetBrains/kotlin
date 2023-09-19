/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*

internal object ClassIdCalculator {
    fun calculateClassId(declaration: KtClassLikeDeclaration): ClassId? {
        var ktFile: KtFile? = null
        val containingClasses = mutableListOf<KtClassLikeDeclaration>()

        for (element in declaration.parentsWithSelf) {
            when (element) {
                is KtEnumEntry -> {
                    return null
                }
                is KtClassLikeDeclaration -> {
                    containingClasses += element
                }
                is KtFile -> {
                    ktFile = element
                    break
                }
                is KtScript -> {
                    // Skip script parent
                }
                is KtDeclaration, is KtObjectLiteralExpression -> {
                    // Local declarations don't have a 'ClassId'
                    return null
                }
            }
        }

        if (ktFile == null) return null
        val relativeClassName = FqName.fromSegments(
            containingClasses.asReversed().map { containingClass ->
                containingClass.name ?: SpecialNames.NO_NAME_PROVIDED.asString()
            }
        )

        return ClassId(ktFile.packageFqName, relativeClassName, isLocal = false)
    }
}
