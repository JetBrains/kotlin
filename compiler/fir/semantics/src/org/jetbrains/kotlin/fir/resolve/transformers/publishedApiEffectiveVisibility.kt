/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.unexpandedClassId
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private object PublishedApiEffectiveVisibilityKey : FirDeclarationDataKey()
private object LazyPublishedApiEffectiveVisibilityKey : FirDeclarationDataKey()

var FirDeclaration.nonLazyPublishedApiEffectiveVisibility: EffectiveVisibility? by FirDeclarationDataRegistry.data(
    PublishedApiEffectiveVisibilityKey
)
var FirDeclaration.lazyPublishedApiEffectiveVisibility: Lazy<EffectiveVisibility?>? by FirDeclarationDataRegistry.data(
    LazyPublishedApiEffectiveVisibilityKey
)

val FirDeclaration.publishedApiEffectiveVisibility: EffectiveVisibility?
    get() = nonLazyPublishedApiEffectiveVisibility ?: lazyPublishedApiEffectiveVisibility?.value

inline val FirBasedSymbol<*>.publishedApiEffectiveVisibility: EffectiveVisibility?
    get() {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        return fir.publishedApiEffectiveVisibility
    }

fun computePublishedApiEffectiveVisibility(
    annotations: List<FirAnnotation>,
    visibility: Visibility,
    selfEffectiveVisibility: EffectiveVisibility,
    containingClassSymbol: FirClassLikeSymbol<*>?,
    parentEffectiveVisibility: EffectiveVisibility,
    parentPublishedEffectiveVisibility: EffectiveVisibility?,
    forClass: Boolean,
    session: FirSession,
): EffectiveVisibility? {
    val hasPublishedApiAnnotation = annotations.any {
        (it.resolvedType as? ConeClassLikeType)?.lookupTag?.classId == StandardClassIds.Annotations.PublishedApi
    }

    return computePublishedApiEffectiveVisibility(
        hasPublishedApiAnnotation,
        visibility,
        selfEffectiveVisibility,
        containingClassSymbol?.toLookupTag(),
        parentEffectiveVisibility,
        parentPublishedEffectiveVisibility,
        forClass,
        session
    )
}

fun computePublishedApiEffectiveVisibility(
    hasPublishedApiAnnotation: Boolean,
    visibility: Visibility,
    selfEffectiveVisibility: EffectiveVisibility,
    containingClassLookupTag: ConeClassLikeLookupTag?,
    parentEffectiveVisibility: EffectiveVisibility,
    parentPublishedEffectiveVisibility: EffectiveVisibility?,
    forClass: Boolean,
    session: FirSession,
): EffectiveVisibility? {
    val selfPublishedEffectiveVisibility = runIf(hasPublishedApiAnnotation) {
        visibility.toEffectiveVisibility(
            containingClassLookupTag, forClass = forClass, ownerIsPublishedApi = true
        )
    }

    if (selfPublishedEffectiveVisibility != null || parentPublishedEffectiveVisibility != null) {
        return (parentPublishedEffectiveVisibility ?: parentEffectiveVisibility).lowerBound(
            (selfPublishedEffectiveVisibility ?: selfEffectiveVisibility),
            session.typeContext
        )
    }

    return null
}

/**
 * Published visibility depends on the published visibility of the containing class.
 * However, the published visibility of deserialized classes can't be eagerly determined because it depends on their annotations, which
 * are loaded later to prevent endless loops.
 * To break up this dependency, the published visibility can be computed lazily when containing classes are fully deserialized.
 */
fun FirMemberDeclaration.setLazyPublishedVisibility(session: FirSession) {
    setLazyPublishedVisibility(annotations, null, session)
}

fun FirMemberDeclaration.setLazyPublishedVisibility(annotations: List<FirAnnotation>, parentProperty: FirProperty?, session: FirSession) {
    setLazyPublishedVisibility(
        hasPublishedApi = annotations.any { it.unexpandedClassId == StandardClassIds.Annotations.PublishedApi },
        parentProperty,
        session
    )
}

fun FirMemberDeclaration.setLazyPublishedVisibility(hasPublishedApi: Boolean, parentProperty: FirProperty?, session: FirSession) {
    if (!hasPublishedApi) return

    lazyPublishedApiEffectiveVisibility = lazy {
        val containingClassLookupTag = (when {
            parentProperty != null -> parentProperty.symbol.callableId.classId
            this is FirClassLikeDeclaration -> classId.parentClassId
            this is FirCallableDeclaration -> symbol.callableId.classId
            else -> null
        })?.toLookupTag()

        val status = status as FirResolvedDeclarationStatus
        val parentSymbol = containingClassLookupTag?.toSymbol(session)
        computePublishedApiEffectiveVisibility(
            hasPublishedApiAnnotation = true,
            visibility = status.visibility,
            selfEffectiveVisibility = status.effectiveVisibility,
            containingClassLookupTag = containingClassLookupTag,
            parentEffectiveVisibility = parentProperty?.effectiveVisibility ?: parentSymbol?.effectiveVisibility
            ?: EffectiveVisibility.Public,
            parentPublishedEffectiveVisibility = parentProperty?.publishedApiEffectiveVisibility
                ?: parentSymbol?.publishedApiEffectiveVisibility,
            forClass = this is FirClass,
            session = session,
        )
    }
}
