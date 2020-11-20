/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorResolvedQualifier : FirResolvedQualifier(), FirDiagnosticHolder {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val packageFqName: FqName
    abstract override val relativeClassFqName: FqName?
    abstract override val classId: ClassId?
    abstract override val symbol: FirClassLikeSymbol<*>?
    abstract override val isNullableLHSForCallableReference: Boolean
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val diagnostic: ConeDiagnostic

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorResolvedQualifier(this, data)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorResolvedQualifier

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirErrorResolvedQualifier
}
