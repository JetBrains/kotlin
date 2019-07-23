/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirVariableImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeCallWithArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirUnknownTypeExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

interface DummyElement

class DummyFirStatement(
    psi: PsiElement? = null,
    session: FirSession = object : FirSessionBase(null) {}
) : FirUnknownTypeExpression(session, psi), DummyElement

class DummyFirVariable<F: FirVariable<F>>(
    override val psi: PsiElement? = null,
    override val session: FirSession = object : FirSessionBase(null) {},
    override val annotations: List<FirAnnotationCall> = mutableListOf(),
    override val receiverTypeRef: FirTypeRef? = null,
    override val name: Name = SpecialNames.NO_NAME_PROVIDED,
    override var returnTypeRef: FirTypeRef = FirImplicitTypeRefImpl(session, psi),
    override val isVar: Boolean = false,
    override var initializer: FirExpression? = null,
    override val symbol: FirVariableSymbol<F> = FirVariableSymbol(name),
    override var delegate: FirExpression? = null
) : FirVariable<F>, DummyElement {
    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }
}

class DummyFirAnonymousFunction(
    psi: PsiElement? = null,
    session: FirSession = object : FirSessionBase(null) {},
    override val valueParameters: List<FirValueParameter> = listOf(),
    override val body: FirBlock? = null,
    override val receiverTypeRef: FirTypeRef? = null,
    override var returnTypeRef: FirTypeRef = FirImplicitTypeRefImpl(session, psi)
) : FirAnonymousFunction(session, psi),
    DummyElement {
    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }
}

class DummyFirDeclaration(
    override val psi: PsiElement? = null,
    override val session: FirSession = object : FirSessionBase(null) {},
    override val annotations: List<FirAnnotationCall> = listOf()
) : FirDeclaration, FirStatement, DummyElement {
    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDeclaration(this, data)
}