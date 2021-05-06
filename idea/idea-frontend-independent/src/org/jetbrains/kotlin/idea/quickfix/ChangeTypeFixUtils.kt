/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

object ChangeTypeFixUtils {
    fun familyName(): String = KotlinBundle.message("fix.change.return.type.family")

    fun functionOrConstructorParameterPresentation(element: KtCallableDeclaration, containerName: String?): String? {
        val name = element.name
        return if (name != null) {
            val fullName = if (containerName != null) "'${containerName}.$name'" else "'$name'"
            when (element) {
                is KtParameter -> KotlinBundle.message("fix.change.return.type.presentation.property", fullName)
                is KtProperty -> KotlinBundle.message("fix.change.return.type.presentation.property", fullName)
                else -> KotlinBundle.message("fix.change.return.type.presentation.function", fullName)
            }
        } else null
    }


    fun baseFunctionOrConstructorParameterPresentation(presentation: String): String =
        KotlinBundle.message("fix.change.return.type.presentation.base", presentation)

    fun baseFunctionOrConstructorParameterPresentation(element: KtCallableDeclaration, containerName: String?): String? {
        val presentation = functionOrConstructorParameterPresentation(element, containerName) ?: return null
        return baseFunctionOrConstructorParameterPresentation(presentation)
    }

    fun getTextForQuickFix(
        element: KtCallableDeclaration,
        presentation: String?,
        isUnitType: Boolean,
        typePresentation: String
    ): String {
        if (isUnitType && element is KtFunction && element.hasBlockBody()) {
            return if (presentation == null)
                KotlinBundle.message("fix.change.return.type.remove.explicit.return.type")
            else
                KotlinBundle.message("fix.change.return.type.remove.explicit.return.type.of", presentation)
        }

        return when (element) {
            is KtFunction -> {
                if (presentation != null)
                    KotlinBundle.message("fix.change.return.type.return.type.text.of", presentation, typePresentation)
                else
                    KotlinBundle.message("fix.change.return.type.return.type.text", typePresentation)
            }
            else -> {
                if (presentation != null)
                    KotlinBundle.message("fix.change.return.type.type.text.of", presentation, typePresentation)
                else
                    KotlinBundle.message("fix.change.return.type.type.text", typePresentation)
            }
        }
    }
}