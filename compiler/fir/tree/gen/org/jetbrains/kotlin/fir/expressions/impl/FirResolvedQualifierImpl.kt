/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

@OptIn(UnresolvedExpressionTypeAccess::class)
internal class FirResolvedQualifierImpl(
    override val source: KtSourceElement?,
    @property:UnresolvedExpressionTypeAccess
    override var coneTypeOrNull: ConeKotlinType?,
    override var annotations: MutableOrEmptyList<FirAnnotation>,
    override var packageFqName: FqName,
    override var relativeClassFqName: FqName?,
    override val symbol: FirClassLikeSymbol<*>?,
    override var isNullableLHSForCallableReference: Boolean,
    override var canBeValue: Boolean,
    override val isFullyQualified: Boolean,
    override var nonFatalDiagnostics: MutableOrEmptyList<ConeDiagnostic>,
    override var typeArguments: MutableOrEmptyList<FirTypeProjection>,
) : FirResolvedQualifier() {
    override val classId: ClassId?
        get() = relativeClassFqName?.let {
    ClassId(packageFqName, it, isLocal = false)
}
    override var resolvedToCompanionObject: Boolean = (symbol?.fir as? FirRegularClass)?.companionObjectSymbol != null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirResolvedQualifierImpl {
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedQualifierImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifierImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean) {
        isNullableLHSForCallableReference = newIsNullableLHSForCallableReference
    }

    override fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean) {
        resolvedToCompanionObject = newResolvedToCompanionObject
    }

    override fun replaceCanBeValue(newCanBeValue: Boolean) {
        canBeValue = newCanBeValue
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments = newTypeArguments.toMutableOrEmpty()
    }
}
