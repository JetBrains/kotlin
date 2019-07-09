/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.jdk2k

import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis

interface Transformation {
    operator fun invoke(callExpression: KtCallExpression, replacement: Replacement)
    fun isApplicable(call: KtCallExpression): Boolean = true
}

object WithoutAdditionalTransformation : Transformation {
    override fun invoke(callExpression: KtCallExpression, replacement: Replacement) {
        val psiFactory = KtPsiFactory(callExpression)
        val calleeLength = callExpression.calleeExpression?.textLength ?: return
        val replaced = callExpression.getQualifiedExpressionForSelectorOrThis().replaced(
            psiFactory.createExpression("${replacement.kotlinFunctionFqName}${callExpression.text.substring(calleeLength)}")
        )
        ShortenReferences.DEFAULT.process(replaced)
    }
}

object ToExtensionFunction : Transformation {
    override fun invoke(callExpression: KtCallExpression, replacement: Replacement) {
        val file = callExpression.containingKtFile
        val psiFactory = KtPsiFactory(callExpression)
        val valueArguments = callExpression.valueArguments
        val typeArguments = callExpression.typeArgumentList?.text ?: ""
        val receiverText = valueArguments.first().getArgumentExpression()
            ?.run { if (this is KtOperationExpression) "($text)" else text }
            ?: valueArguments.first().text
        val argumentsText = valueArguments.drop(1).joinToString(separator = ", ") { it.text }
        callExpression.getQualifiedExpressionForSelectorOrThis().replaced(
            psiFactory.createExpression("$receiverText.${replacement.kotlinFunctionShortName}$typeArguments($argumentsText)")
        )
        file.resolveImportReference(FqName(replacement.kotlinFunctionFqName)).firstOrNull()?.let {
            ImportInsertHelper.getInstance(callExpression.project).importDescriptor(file, it)
        }
    }

    override fun isApplicable(call: KtCallExpression): Boolean = call.valueArguments.isNotEmpty()
}