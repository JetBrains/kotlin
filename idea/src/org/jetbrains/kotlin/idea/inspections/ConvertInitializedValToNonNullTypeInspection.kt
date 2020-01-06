/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.propertyVisitor
import org.jetbrains.kotlin.types.TypeUtils

class ConvertInitializedValToNonNullTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        propertyVisitor(fun(property) {
            val typeReference = property.typeReference ?: return
            if (property.initializer == null) {
                return
            }
            val type = property.resolveToDescriptorIfAny()?.type ?: return
            if (TypeUtils.isNullableType(type)) {
                holder.registerProblem(
                    typeReference,
                    "Initialized 'val' should be converted to non-null type",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveRedundantNullableTypeQuickfix()
                )
            }

        })
}

class RemoveRedundantNullableTypeQuickfix : LocalQuickFix {
    override fun getName() = "Convert initialized 'val' to non-null type"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val factory = KtPsiFactory(project)
        element.replace(factory.createIdentifier(element.text.removeSuffix("?")))
    }
}
