/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.AbbreviatedType

class RedundantExplicitTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        propertyVisitor(fun(property) {
            if (!property.isLocal) return
            val typeReference = property.typeReference ?: return
            val initializer = property.initializer ?: return

            val type = property.resolveToDescriptorIfAny()?.type ?: return
            if (type is AbbreviatedType) return
            when (initializer) {
                is KtConstantExpression -> {
                    when (initializer.node.elementType) {
                        KtNodeTypes.BOOLEAN_CONSTANT -> {
                            if (!KotlinBuiltIns.isBoolean(type)) return
                        }
                        KtNodeTypes.INTEGER_CONSTANT -> {
                            if (initializer.text.endsWith("L")) {
                                if (!KotlinBuiltIns.isLong(type)) return
                            } else {
                                if (!KotlinBuiltIns.isInt(type)) return
                            }
                        }
                        KtNodeTypes.FLOAT_CONSTANT -> {
                            if (initializer.text.endsWith("f") || initializer.text.endsWith("F")) {
                                if (!KotlinBuiltIns.isFloat(type)) return
                            } else {
                                if (!KotlinBuiltIns.isDouble(type)) return
                            }
                        }
                        KtNodeTypes.CHARACTER_CONSTANT -> {
                            if (!KotlinBuiltIns.isChar(type)) return
                        }
                        else -> return
                    }
                }
                is KtStringTemplateExpression -> {
                    if (!KotlinBuiltIns.isString(type)) return
                }
                is KtNameReferenceExpression -> {
                    if (typeReference.text != initializer.getReferencedName()) return
                }
                is KtCallExpression -> {
                    if (typeReference.text != initializer.calleeExpression?.text) return
                }
                else -> return
            }

            holder.registerProblem(
                typeReference,
                "Explicitly given type is redundant here",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                IntentionWrapper(RemoveExplicitTypeIntention(), property.containingKtFile)
            )
        })
}