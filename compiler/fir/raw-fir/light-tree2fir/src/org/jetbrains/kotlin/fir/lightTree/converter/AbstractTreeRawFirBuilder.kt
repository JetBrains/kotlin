/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.name.Name

abstract class AbstractTreeRawFirBuilder<Node : Any>(
    baseSession: FirSession,
    context: Context<Node>,
) : AbstractRawFirBuilder<Node>(baseSession, context) {
    protected val implicitType: FirImplicitTypeRef = FirImplicitTypeRefImplWithoutSource

    override fun Node.getReferencedNameAsName(): Name {
        return asText.nameAsSafeName()
    }

    protected abstract fun Node.getFirstChildExpression(): Node?

    protected abstract fun Node.getLastChildExpression(): Node?

    override fun Node.getExpressionInParentheses(): Node? = getFirstChildExpression()

    override fun Node.getAnnotatedExpression(): Node? = getFirstChildExpression()

    override fun Node.getLabeledExpression(): Node? = getLastChildExpression()

    override val Node?.arrayExpression: Node?
        get() = this?.getFirstChildExpression()

    fun unquoteIdentifier(quoted: String): String {
        if (quoted.indexOf('`') < 0) {
            return quoted
        }

        if (quoted.startsWith("`") && quoted.endsWith("`") && quoted.length >= 2) {
            return quoted.substring(1, quoted.length - 1)
        } else {
            return quoted
        }
    }
}
