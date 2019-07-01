/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ConvertCollectionConstructorToFunction : SelfTargetingIntention<KtCallExpression>(
    KtCallExpression::class.java, "Convert Collection constructor to function"
) {

    private val functionMap = hashMapOf(
        "java.util.ArrayList.<init>" to "arrayListOf",
        "kotlin.collections.ArrayList.<init>" to "arrayListOf",
        "java.util.HashMap.<init>" to "hashMapOf",
        "kotlin.collections.HashMap.<init>" to "hashMapOf",
        "java.util.HashSet.<init>" to "hashSetOf",
        "kotlin.collections.HashSet.<init>" to "hashSetOf",
        "java.util.LinkedHashMap.<init>" to "linkedMapOf",
        "kotlin.collections.LinkedHashMap.<init>" to "linkedMapOf",
        "java.util.LinkedHashSet.<init>" to "linkedSetOf",
        "kotlin.collections.LinkedHashSet.<init>" to "linkedSetOf"
    )

    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        val fq = element.resolveToCall()?.resultingDescriptor?.fqNameSafe?.asString() ?: return false
        return functionMap.containsKey(fq) && element.valueArguments.size == 0
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val fq = element.resolveToCall()?.resultingDescriptor?.fqNameSafe?.asString() ?: return
        val toCall = functionMap[fq] ?: return
        val callee = element.calleeExpression ?: return
        callee.replace(KtPsiFactory(element).createIdentifier(toCall))
        element.getQualifiedExpressionForSelector()?.replace(element)
    }
}
