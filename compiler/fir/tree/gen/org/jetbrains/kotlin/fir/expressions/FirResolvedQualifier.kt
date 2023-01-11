/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedQualifier : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val packageFqName: FqName
    abstract val relativeClassFqName: FqName?
    abstract val classId: ClassId?
    abstract val symbol: FirClassLikeSymbol<*>?
    abstract val isNullableLHSForCallableReference: Boolean
    abstract val resolvedToCompanionObject: Boolean
    abstract val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract val typeArguments: List<FirTypeProjection>

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitResolvedQualifier(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformResolvedQualifier(this, data) as E

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean)

    abstract fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedQualifier

    abstract fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifier
}
