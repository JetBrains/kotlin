/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.parsing.hasIllegalUnderscore
import org.jetbrains.kotlin.parsing.hasLongSuffix
import org.jetbrains.kotlin.parsing.hasUnsignedLongSuffix
import org.jetbrains.kotlin.parsing.hasUnsignedSuffix
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.parsing.parseNumericLiteral
import org.jetbrains.kotlin.psi.*

internal object ClassIdCalculator {
    fun calculateClassId(declaration: KtClassLikeDeclaration): ClassId? {
        var ktFile: KtFile? = null
        val containingClasses = mutableListOf<KtClassLikeDeclaration>()

        for (element in declaration.parentsWithSelf) {
            when (element) {
                is KtEnumEntry,
                is KtCallElement,
                is KtObjectLiteralExpression,
                is KtCodeFragment,
                is PsiErrorElement,
                -> {
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
                is KtDeclaration -> {
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

    /**
     * A best-effort way to get the class id of expression's type without resolve.
     */
    fun inferConstantExpressionClassIdByPsi(expression: KtConstantExpression): ClassId? {
        val convertedText: Any? = when (expression.elementType) {
            KtNodeTypes.INTEGER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT -> {
                if (hasIllegalUnderscore(expression.text, expression.elementType)) return null
                parseNumericLiteral(expression.text, expression.elementType)
            }

            KtNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(expression.text)
            else -> null
        }

        return when (expression.elementType) {
            KtNodeTypes.INTEGER_CONSTANT -> when {
                convertedText !is Long -> null
                hasUnsignedLongSuffix(expression.text) -> StandardClassIds.ULong
                hasLongSuffix(expression.text) -> StandardClassIds.Long
                hasUnsignedSuffix(expression.text) -> {
                    if (convertedText.toULong() > UInt.MAX_VALUE || convertedText.toULong() < UInt.MIN_VALUE) {
                        StandardClassIds.ULong
                    } else {
                        StandardClassIds.UInt
                    }
                }

                else -> if (convertedText > Int.MAX_VALUE || convertedText < Int.MIN_VALUE) {
                    StandardClassIds.Long
                } else {
                    StandardClassIds.Int
                }
            }

            KtNodeTypes.FLOAT_CONSTANT -> if (convertedText is Float) StandardClassIds.Float else StandardClassIds.Double
            KtNodeTypes.CHARACTER_CONSTANT -> StandardClassIds.Char
            KtNodeTypes.BOOLEAN_CONSTANT -> StandardClassIds.Boolean
            else -> null
        }
    }
}
