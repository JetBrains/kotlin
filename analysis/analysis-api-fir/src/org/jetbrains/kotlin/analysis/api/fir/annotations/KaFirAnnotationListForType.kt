/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.toKaAnnotation
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForType private constructor(
    val coneType: ConeKotlinType,
    private val builder: KaSymbolByFirBuilder,
) : AbstractList<KaAnnotation>(), KaAnnotationList {
    private val backingAnnotations: List<KaAnnotation> by lazy {
        coneType.customAnnotations.map { firAnnotation ->
            // Resolve annotation types. Probably this call is redundant as we may have resolved types only after the TYPES phase
            (firAnnotation as? FirAnnotationCall)?.containingDeclarationSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)

            firAnnotation.toKaAnnotation(builder)
        }
    }

    override val token: KaLifetimeToken
        get() = builder.token

    override fun isEmpty(): Boolean = withValidityAssertion {
        coneType.customAnnotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { coneType.customAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = withValidityAssertion {
        backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        backingAnnotations.any { it.classId == classId }
    }

    override fun get(classId: ClassId): List<KaAnnotation> = withValidityAssertion {
        backingAnnotations.filter { it.classId == classId }
    }

    override val classIds: Collection<ClassId>
        get() = withValidityAssertion {
            backingAnnotations.mapNotNull { it.classId }
        }

    companion object {
        fun create(coneType: ConeKotlinType, builder: KaSymbolByFirBuilder): KaAnnotationList {
            return if (coneType.customAnnotations.isEmpty()) {
                KaBaseEmptyAnnotationList(builder.token)
            } else {
                KaFirAnnotationListForType(coneType, builder)
            }
        }
    }
}