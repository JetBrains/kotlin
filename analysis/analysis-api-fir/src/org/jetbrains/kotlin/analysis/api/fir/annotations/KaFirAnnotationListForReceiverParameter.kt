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
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForReceiverParameter private constructor(
    private val firCallableSymbol: FirCallableSymbol<*>,
    private val receiverParameter: FirAnnotationContainer,
    private val builder: KaSymbolByFirBuilder,
) : AbstractList<KaAnnotation>(), KaAnnotationList {
    private val backingAnnotations by lazy { annotations(firCallableSymbol, builder, receiverParameter) }

    private val useSiteSession: FirSession
        get() = builder.rootSession

    override val token: KaLifetimeToken
        get() = builder.token

    override fun isEmpty(): Boolean = withValidityAssertion {
        // isEmpty check is possible on a callable symbol directly as annotations cannot be moved from a containing
        // declaration (e.g., a containing property) to the receiver parameter afterwards
        // See 'FirTypeResolveTransformer.moveOrDeleteIrrelevantAnnotations()'
        return receiverParameter.annotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { receiverParameter.annotations.size }

    override fun iterator(): Iterator<KaAnnotation> = withValidityAssertion {
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = withValidityAssertion {
        return backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        return hasAnnotation(firCallableSymbol, classId, useSiteSession, receiverParameter)
    }

    override fun get(classId: ClassId, ): List<KaAnnotation> = withValidityAssertion {
        return annotationsByClassId(firCallableSymbol, classId, builder, receiverParameter)
    }

    override val classIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firCallableSymbol, useSiteSession, receiverParameter)
        }

    companion object {
        fun create(firCallableSymbol: FirCallableSymbol<*>, builder: KaSymbolByFirBuilder): KaAnnotationList {
            val receiverParameter = firCallableSymbol.receiverParameter
            return if (receiverParameter?.annotations?.isEmpty() != false) {
                KaEmptyAnnotationList(builder.token)
            } else {
                KaFirAnnotationListForReceiverParameter(firCallableSymbol, receiverParameter, builder)
            }
        }
    }
}
