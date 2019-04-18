/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.MayBeConstantInspection.Status.*
import org.jetbrains.kotlin.idea.quickfix.AddConstModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.propertyVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.constants.evaluate.isStandaloneOnlyConstant
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MayBeConstantInspection : AbstractKotlinInspection() {
    enum class Status {
        NONE,
        MIGHT_BE_CONST,
        MIGHT_BE_CONST_ERRONEOUS,
        JVM_FIELD_MIGHT_BE_CONST,
        JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER,
        JVM_FIELD_MIGHT_BE_CONST_ERRONEOUS
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return propertyVisitor { property ->
            val status = property.getStatus()
            when (status) {
                NONE, JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER,
                MIGHT_BE_CONST_ERRONEOUS, JVM_FIELD_MIGHT_BE_CONST_ERRONEOUS -> return@propertyVisitor
                MIGHT_BE_CONST, JVM_FIELD_MIGHT_BE_CONST -> {
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

    companion object {
        fun KtProperty.getStatus(): Status {
            if (isLocal || isVar || getter != null ||
                hasModifier(KtTokens.CONST_KEYWORD) || hasModifier(KtTokens.OVERRIDE_KEYWORD)
            ) {
                return NONE
            }
            val containingClassOrObject = this.containingClassOrObject
            if (!isTopLevel && containingClassOrObject !is KtObjectDeclaration) return NONE
            if (containingClassOrObject?.isObjectLiteral() == true) return NONE

            val initializer = initializer
            // For some reason constant evaluation does not work for property.analyze()
            val context = (initializer ?: this).analyze(BodyResolveMode.PARTIAL)
            val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? VariableDescriptor ?: return NONE
            val type = propertyDescriptor.type
            if (!KotlinBuiltIns.isPrimitiveType(type) && !KotlinBuiltIns.isString(type)) return NONE
            val withJvmField = propertyDescriptor.hasJvmFieldAnnotation()
            if (annotationEntries.isNotEmpty() && !withJvmField) return NONE

            return when {
                initializer != null -> {
                    val compileTimeConstant = ConstantExpressionEvaluator.getConstant(
                        initializer, context
                    ) ?: return NONE
                    val erroneousConstant = compileTimeConstant.usesNonConstValAsConstant
                    compileTimeConstant.toConstantValue(propertyDescriptor.type).takeIf {
                        !it.isStandaloneOnlyConstant() && it !is NullValue && it !is ErrorValue
                    } ?: return NONE
                    when {
                        withJvmField ->
                            if (erroneousConstant) JVM_FIELD_MIGHT_BE_CONST_ERRONEOUS
                            else JVM_FIELD_MIGHT_BE_CONST
                        else ->
                            if (erroneousConstant) MIGHT_BE_CONST_ERRONEOUS
                            else MIGHT_BE_CONST
                    }
                }
                withJvmField -> JVM_FIELD_MIGHT_BE_CONST_NO_INITIALIZER
                else -> NONE
            }
        }
    }
}
