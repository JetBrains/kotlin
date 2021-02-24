/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isBoolean

class ExpandBooleanExpressionIntention : SelfTargetingRangeIntention<KtExpression>(
    KtExpression::class.java,
    KotlinBundle.lazyMessage("expand.boolean.expression.to.if.else")
) {
    override fun applicabilityRange(element: KtExpression): TextRange? {
        val target = element.parents.takeWhile {
            it is KtCallExpression || it is KtQualifiedExpression || it is KtOperationExpression || it is KtParenthesizedExpression
        }.lastOrNull() ?: element
        if (element != target) return null
        if (element.deparenthesize() is KtConstantExpression) return null
        val parent = element.parent
        if (parent is KtValueArgument || parent is KtParameter || parent is KtStringTemplateEntry) return null
        val context = element.analyze(BodyResolveMode.PARTIAL)
        if (context[BindingContext.EXPRESSION_TYPE_INFO, element]?.type?.isBoolean() != true) return null
        return element.textRange
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val ifExpression = KtPsiFactory(element).createExpressionByPattern("if ($0) {\ntrue\n} else {\nfalse\n}", element)
        val replaced = element.replace(ifExpression)
        if (replaced != null) {
            editor?.caretModel?.moveToOffset(replaced.startOffset)
        }
    }
}
