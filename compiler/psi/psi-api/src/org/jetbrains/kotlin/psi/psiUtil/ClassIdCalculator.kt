/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.utils.*

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
        val elementType =
            @Suppress("DEPRECATION")
            expression.elementType

        val convertedText: Any? = when (elementType) {
            KtNodeTypes.INTEGER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT -> {
                val isFloatingPoint = elementType == KtNodeTypes.FLOAT_CONSTANT
                if (hasIllegallyPositionedUnderscore(expression.text, isFloatingPoint)) return null
                parseNumericLiteral(expression.text, isFloatingPoint)
            }

            KtNodeTypes.BOOLEAN_CONSTANT -> parseBooleanLiteral(expression.text)
            else -> null
        }

        return when (elementType) {
            KtNodeTypes.INTEGER_CONSTANT -> when {
                convertedText !is Long -> null
                hasUnsignedLongNumericLiteralSuffix(expression.text) -> StandardClassIds.ULong
                hasLongNumericLiteralSuffix(expression.text) -> StandardClassIds.Long
                hasUnsignedNumericLiteralSuffix(expression.text) -> {
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
