/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter.utils

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes.DOT_QUALIFIED_EXPRESSION
import org.jetbrains.kotlin.KtNodeTypes.SAFE_ACCESS_EXPRESSION
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.generateNotNullOrOther
import org.jetbrains.kotlin.fir.builder.generateResolvedAccessExpression
import org.jetbrains.kotlin.fir.declarations.impl.FirVariableImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirBlockImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirComponentCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirThrowExpressionImpl
import org.jetbrains.kotlin.fir.lightTree.fir.DestructuringDeclaration
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens.AS_SAFE
import org.jetbrains.kotlin.lexer.KtTokens.IDENTIFIER
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing

val qualifiedAccessTokens = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)

fun String.getOperationSymbol(): IElementType {
    KotlinExpressionParsing.ALL_OPERATIONS.types.forEach {
        if (it is KtSingleValueToken && it.value == this) return it
    }
    if (this == "as?") return AS_SAFE
    return IDENTIFIER
}

fun generateDestructuringBlock(
    session: FirSession,
    multiDeclaration: DestructuringDeclaration,
    container: FirVariable<*>,
    tmpVariable: Boolean
): FirExpression {
    return FirBlockImpl(session, null).apply {
        if (tmpVariable) {
            statements += container
        }
        val isVar = multiDeclaration.isVar
        for ((index, entry) in multiDeclaration.entries.withIndex()) {
            statements += FirVariableImpl(
                session, null, entry.name,
                entry.returnTypeRef, isVar,
                FirComponentCallImpl(session, null, index + 1, generateResolvedAccessExpression(session, null, container)),
                FirVariableSymbol(entry.name) // TODO?
            ).apply {
                annotations += entry.annotations
                symbol.bind(this)
            }
        }
    }
}
