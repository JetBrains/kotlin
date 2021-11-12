/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.annotations.KtFirAnnotationCall
import org.jetbrains.kotlin.analysis.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.transformers.resolveSupertypesInTheAir
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType

internal class KtFirTypeAndAnnotations<T : FirDeclaration>(
    private val containingDeclaration: FirRefWithValidityCheck<T>,
    @Suppress("UNUSED_PARAMETER") typeResolvePhase: FirResolvePhase,
    private val builder: KtSymbolByFirBuilder,
    private val typeRef: (T) -> FirTypeRef,
) : KtTypeAndAnnotations() {

    override val token: ValidityToken get() = containingDeclaration.token

    override val type: KtType by containingDeclaration.withFirAndCache(ResolveType.CallableReturnType) { fir ->
        builder.typeBuilder.buildKtType(typeRef(fir))
    }

    override val annotations: List<KtAnnotationCall> by containingDeclaration.withFirAndCache { fir ->
        typeRef(fir).annotations.map {
            KtFirAnnotationCall(containingDeclaration, it)
        }
    }
}

internal class KtSimpleFirTypeAndAnnotations(
    private val coneType: ConeKotlinType,
    private val _annotations: List<KtAnnotationCall>,
    private val builder: KtSymbolByFirBuilder,
    override val token: ValidityToken
) : KtTypeAndAnnotations() {
    override val type: KtType by cached {
        builder.typeBuilder.buildKtType(coneType)
    }

    override val annotations: List<KtAnnotationCall> get() = withValidityAssertion { _annotations }
}

internal fun FirRefWithValidityCheck<FirClass>.superTypesAndAnnotationsList(builder: KtSymbolByFirBuilder): List<KtTypeAndAnnotations> =
    withFir(FirResolvePhase.SUPER_TYPES) { fir ->
        fir.superTypeRefs.mapToTypeAndAnnotations(this, builder)
    }

internal fun FirRefWithValidityCheck<FirRegularClass>.superTypesAndAnnotationsListForRegularClass(builder: KtSymbolByFirBuilder): List<KtTypeAndAnnotations> {
    return withFir { fir ->
        if (fir.resolvePhase >= FirResolvePhase.SUPER_TYPES) {
            fir.superTypeRefs.mapToTypeAndAnnotations(this, builder)
        } else null
    } ?: withFirByType(ResolveType.NoResolve) { fir ->
        fir.resolveSupertypesInTheAir(builder.rootSession).mapToTypeAndAnnotations(this, builder)
    }
}

private fun List<FirTypeRef>.mapToTypeAndAnnotations(
    containingDeclaration: FirRefWithValidityCheck<FirClass>,
    builder: KtSymbolByFirBuilder,
) = map { typeRef ->
    val annotations = typeRef.annotations.map { annotation ->
        KtFirAnnotationCall(containingDeclaration, annotation)
    }
    KtSimpleFirTypeAndAnnotations(typeRef.coneType, annotations, builder, containingDeclaration.token)
}

internal fun FirRefWithValidityCheck<FirTypedDeclaration>.returnTypeAndAnnotations(
    typeResolvePhase: FirResolvePhase,
    builder: KtSymbolByFirBuilder
) = KtFirTypeAndAnnotations(this, typeResolvePhase, builder) { it.returnTypeRef }

internal fun FirRefWithValidityCheck<FirCallableDeclaration>.receiverTypeAndAnnotations(builder: KtSymbolByFirBuilder) = withFir { fir ->
    fir.receiverTypeRef?.let { _ ->
        KtFirTypeAndAnnotations(this, FirResolvePhase.TYPES, builder) {
            it.receiverTypeRef ?: error { "Receiver expected for callable declaration but it is null" }
        }
    }
}
