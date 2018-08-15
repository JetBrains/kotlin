// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.collections

import com.intellij.debugger.streams.psi.ChainTransformer
import com.intellij.debugger.streams.wrapper.QualifierExpression
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.impl.StreamChainImpl
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.debugger.sequence.psi.impl.KotlinChainTransformerImpl
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType

class CollectionChainTransformer : ChainTransformer<KtCallExpression> {
    private val transformer = KotlinChainTransformerImpl(KotlinCollectionsTypeExtractor())

    override fun transform(chainCalls: List<KtCallExpression>, context: PsiElement): StreamChain {
        val chain = transformer.transform(chainCalls, context)

        if (chainCalls.first().resolveType().isArray) {
            val qualifier = WrappedQualifier(chain.qualifierExpression)
            return StreamChainImpl(qualifier, chain.intermediateCalls, chain.terminationCall, chain.context)
        }

        return chain
    }

    /**
     * Kotlin arrays have not {@code onEach} extension. But current implementation uses onEach to increment a time counter.
     * We use asIterable to avoid further issues with the transformed expression evaluation
     * TODO: Avoid showing "asIterable()" in the tab name in trace window
     */
    private class WrappedQualifier(private val qualifierExpression: QualifierExpression) : QualifierExpression by qualifierExpression {
        override val text: String
            get() = qualifierExpression.text + ".asIterable()"
    }

    private val KotlinType.isArray: Boolean
        get() = KotlinBuiltIns.isArray(this) || KotlinBuiltIns.isPrimitiveArray(this)
}