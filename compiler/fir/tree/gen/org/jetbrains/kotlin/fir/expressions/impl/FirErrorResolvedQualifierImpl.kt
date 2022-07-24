/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirErrorResolvedQualifierImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val packageFqName: FqName,
    override val relativeClassFqName: FqName?,
    override val symbol: FirClassLikeSymbol<*>?,
    override var isNullableLHSForCallableReference: Boolean,
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic>,
    override val typeArguments: MutableList<FirTypeProjection>,
    override val diagnostic: ConeDiagnostic,
) : FirErrorResolvedQualifier() {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val classId: ClassId? get() = relativeClassFqName?.let {
    ClassId(packageFqName, it, false)
}
    override val resolvedToCompanionObject: Boolean get() = false

    override val elementKind get() = FirElementKind.ErrorResolvedQualifier

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean) {
        isNullableLHSForCallableReference = newIsNullableLHSForCallableReference
    }

    override fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean) {}

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }
}
