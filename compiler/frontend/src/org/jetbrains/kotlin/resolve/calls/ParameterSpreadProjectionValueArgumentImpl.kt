/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.ParameterSpreadProjectionValueArgument
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.ValueArgumentName

class ParameterSpreadProjectionValueArgumentImpl(
    override val originalSpreadArgument: ValueArgument,
    override val spreadReceiverExpression: KtExpression,
    override val parameterName: Name,
) : ParameterSpreadProjectionValueArgument {
    private val projectedArgument: KtValueArgument by lazy(LazyThreadSafetyMode.NONE) {
        KtPsiFactory(spreadReceiverExpression.project, false).createArgument(
            "${renderIdentifier(parameterName)} = ${spreadReceiverExpression.text}.${renderIdentifier(parameterName)}"
        )
    }

    override val cacheKey: String by lazy(LazyThreadSafetyMode.NONE) {
        val receiver = spreadReceiverExpression.textRange
        "${receiver.startOffset}:${receiver.endOffset}"
    }

    override fun getArgumentExpression(): KtExpression? = projectedArgument.getArgumentExpression()

    override fun getArgumentName(): ValueArgumentName? = projectedArgument.getArgumentName()

    override fun isNamed(): Boolean = true

    override fun asElement(): KtElement = originalSpreadArgument.asElement()

    override fun getSpreadElement(): LeafPsiElement? = null

    override fun isExternal(): Boolean = originalSpreadArgument.isExternal()

    private fun renderIdentifier(name: Name): String {
        return if (name.isSpecial) {
            "`${name.asString()}`"
        } else {
            name.asString()
        }
    }
}
