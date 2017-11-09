/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RedundantExplicitTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
            object : KtVisitorVoid() {
                override fun visitProperty(property: KtProperty) {
                    super.visitProperty(property)

                    if (!property.isLocal) return
                    val typeReference = property.typeReference ?: return
                    val initializer = property.initializer ?: return

                    val type = property.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference] ?: return
                    when (initializer) {
                        is KtConstantExpression -> {
                            when (initializer.node.elementType) {
                                KtNodeTypes.BOOLEAN_CONSTANT -> {
                                    if (!KotlinBuiltIns.isBoolean(type)) return
                                }
                                KtNodeTypes.INTEGER_CONSTANT -> {
                                    if (initializer.text.endsWith("L")) {
                                        if (!KotlinBuiltIns.isLong(type)) return
                                    }
                                    else {
                                        if (!KotlinBuiltIns.isInt(type)) return
                                    }
                                }
                                KtNodeTypes.FLOAT_CONSTANT -> {
                                    if (initializer.text.endsWith("f") || initializer.text.endsWith("F")) {
                                        if (!KotlinBuiltIns.isFloat(type)) return
                                    }
                                    else {
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
                }
            }
}