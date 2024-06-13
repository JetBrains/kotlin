/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
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
) : AbstractList<KaAnnotation>(), KaAnnotationList {
    private val backingAnnotations by lazy { annotations(firSymbol, builder) }

    override val token: KaLifetimeToken
        get() = builder.token

    private val useSiteSession: FirSession
        get() = builder.rootSession

    override fun isEmpty(): Boolean = withValidityAssertion {
        // isEmpty check needs to be performed on an analyzed declaration
        // (annotations can move to a nested declaration after code analysis).
        // See 'FirTypeResolveTransformer.moveOrDeleteIrrelevantAnnotations()'
        return backingAnnotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = withValidityAssertion {
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = withValidityAssertion {
        return backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        return hasAnnotation(firSymbol, classId, useSiteSession)
    }

    override fun get(classId: ClassId): List<KaAnnotation> = withValidityAssertion {
        return annotationsByClassId(firSymbol, classId, builder)
    }

    override val classIds: Collection<ClassId>
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
