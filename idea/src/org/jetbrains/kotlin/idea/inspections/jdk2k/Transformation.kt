/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.jdk2k

import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.isNullable

interface Transformation {
    operator fun invoke(callExpression: KtCallExpression, replacement: Replacement)
    fun isApplicable(callExpression: KtCallExpression): Boolean = true
    fun isApplicableInContext(callExpression: KtCallExpression, context: BindingContext): Boolean = true
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

object ToExtensionFunctionWithNonNullableReceiver : Transformation {
    override fun invoke(callExpression: KtCallExpression, replacement: Replacement) {
        val file = callExpression.containingKtFile
        val psiFactory = KtPsiFactory(callExpression)
        val valueArguments = callExpression.valueArguments
        val typeArguments = callExpression.typeArgumentList?.text ?: ""
        val receiverText = valueArguments.first().getArgumentExpression()
            ?.run { if (this is KtOperationExpression) "($text)" else text }
            ?: valueArguments.first().text
        val argumentsText = valueArguments.drop(1).joinToString(separator = ", ") { it.text }

        val oldExpression = callExpression.getQualifiedExpressionForSelectorOrThis()
        val commentSaver = CommentSaver(oldExpression)

        val replaced = oldExpression.replaced(
            psiFactory.createExpression("$receiverText.${replacement.kotlinFunctionShortName}$typeArguments($argumentsText)")
        )
        commentSaver.restore(replaced)

        file.resolveImportReference(FqName(replacement.kotlinFunctionFqName)).firstOrNull()?.let {
            ImportInsertHelper.getInstance(callExpression.project).importDescriptor(file, it)
        }
    }

    override fun isApplicable(callExpression: KtCallExpression): Boolean = callExpression.valueArguments.isNotEmpty()

    override fun isApplicableInContext(callExpression: KtCallExpression, context: BindingContext): Boolean = callExpression
        .valueArguments.firstOrNull()
        ?.getArgumentExpression()
        ?.getType(context)
        ?.isNullable() == false
}

object ToExtensionFunctionWithNonNullableArguments : Transformation {
    override fun invoke(callExpression: KtCallExpression, replacement: Replacement) = ToExtensionFunctionWithNonNullableReceiver(
        callExpression,
        replacement
    )

    override fun isApplicable(callExpression: KtCallExpression): Boolean = ToExtensionFunctionWithNonNullableReceiver.isApplicable(
        callExpression
    )

    override fun isApplicableInContext(callExpression: KtCallExpression, context: BindingContext): Boolean = callExpression
        .valueArguments.all {
        it.getArgumentExpression()?.getType(context)?.isNullable() == false
    }
}

object ToExtensionFunctionWithNullableReceiver : Transformation {
    override fun invoke(callExpression: KtCallExpression, replacement: Replacement) = ToExtensionFunctionWithNonNullableReceiver(
        callExpression,
        replacement
    )

    override fun isApplicable(callExpression: KtCallExpression): Boolean = ToExtensionFunctionWithNonNullableReceiver.isApplicable(
        callExpression
    )
}