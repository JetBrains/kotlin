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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.MayBeConstantInspection.Status.*
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.constants.evaluate.isStandaloneOnlyConstant
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MayBeConstantInspection : AbstractKotlinInspection() {
    enum class Status {
        NONE,
        MIGHT_BE_CONST,
        JVM_FIELD_MIGHT_BE_CONST,
        JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                val status = property.getStatus()
                when (status) {
                    NONE, JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER -> return
                    else -> {
                        holder.registerProblem(
                                property.nameIdentifier ?: property,
                                if (status == JVM_FIELD_MIGHT_BE_CONST) "'const' might be used instead of '@JvmField'" else "Might be 'const'",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                IntentionWrapper(AddConstModifierFix(property), property.containingFile)
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun KtProperty.getStatus(): Status {
            if (isLocal || isVar || hasModifier(KtTokens.CONST_KEYWORD) || containingClassOrObject is KtClass) {
                return NONE
            }
            val initializer = initializer

            // For some reason constant evaluation does not work for property.analyze()
            val context = (initializer ?: this).analyze(BodyResolveMode.PARTIAL)
            val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? VariableDescriptor ?: return NONE
            val withJvmField = propertyDescriptor.hasJvmFieldAnnotation()
            if (annotationEntries.isNotEmpty() && !withJvmField) return NONE

            return when {
                initializer != null -> {
                    ConstantExpressionEvaluator.getConstant(
                            initializer, context
                    )?.toConstantValue(propertyDescriptor.type)?.takeIf { !it.isStandaloneOnlyConstant() } ?: return NONE
                    if (withJvmField) JVM_FIELD_MIGHT_BE_CONST else MIGHT_BE_CONST
                }
                withJvmField -> JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER
                else -> NONE
            }
        }
    }
}
