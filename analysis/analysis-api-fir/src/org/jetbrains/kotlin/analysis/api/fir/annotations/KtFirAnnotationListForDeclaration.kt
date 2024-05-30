/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    private val builder: KaSymbolByFirBuilder,
) : AbstractList<KaAnnotationApplication>(), KaAnnotationList {
    private val backingAnnotations by lazy { annotations(firSymbol, builder) }

    override val token: KaLifetimeToken
        get() = builder.token

    private val useSiteSession: FirSession
        get() = builder.rootSession

    override fun isEmpty(): Boolean = withValidityAssertion {
        return firSymbol.annotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { firSymbol.annotations.size }

    override fun iterator(): Iterator<KaAnnotationApplication> = withValidityAssertion {
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotationApplication = withValidityAssertion {
        return backingAnnotations[index]
    }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        return hasAnnotation(firSymbol, classId, useSiteTargetFilter, useSiteSession)
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplication> = withValidityAssertion {
        return annotationsByClassId(firSymbol, classId, useSiteTargetFilter, builder)
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firSymbol, useSiteSession)
        }

    companion object {
        fun create(firSymbol: FirBasedSymbol<*>, builder: KaSymbolByFirBuilder): KaAnnotationList {
            return when {
                firSymbol is FirBackingFieldSymbol && firSymbol.propertySymbol.annotations.any { it.useSiteTarget == null } ->
                    KaFirAnnotationListForDeclaration(firSymbol, builder)
                firSymbol.annotations.isEmpty() ->
                    KaEmptyAnnotationList(builder.token)
                else ->
                    KaFirAnnotationListForDeclaration(firSymbol, builder)
            }
        }
    }
}
