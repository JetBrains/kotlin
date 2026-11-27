/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.toKaAnnotation
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeAnnotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForType private constructor(
    val coneType: ConeKotlinType,
    override val analysisSession: KaFirSession,
) : AbstractList<KaAnnotation>(), KaAnnotationList, KaFirSessionBoundOwner {
    private val backingAnnotations: List<KaAnnotation> by lazy {
        coneType.typeAnnotations.map { firAnnotation ->
            // Resolve annotation types. Probably this call is redundant as we may have resolved types only after the TYPES phase
            (firAnnotation as? FirAnnotationCall)?.containingDeclarationSymbol?.lazyResolveToPhase(FirResolvePhase.TYPES)

            firAnnotation.toKaAnnotation(analysisSession = analysisSession)
        }
    }

    override fun isEmpty(): Boolean = entryPointImplementation {
        coneType.typeAnnotations.isEmpty()
    }

    override val size: Int
        get() = entryPointImplementation { coneType.typeAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = entryPointImplementation {
        backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = entryPointImplementation {
        backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = entryPointImplementation {
        backingAnnotations.any { it.classId == classId }
    }

    override fun get(classId: ClassId): List<KaAnnotation> = entryPointImplementation {
        backingAnnotations.filter { it.classId == classId }
    }

    override val classIds: Collection<ClassId>
        get() = entryPointImplementation {
            backingAnnotations.mapNotNull { it.classId }
        }

    companion object {
        context(analysisSession: KaFirSession)
        fun create(coneType: ConeKotlinType): KaAnnotationList {
            return if (coneType.typeAnnotations.isEmpty()) {
                KaFirEmptyAnnotationList(analysisSession.token)
            } else {
                KaFirAnnotationListForType(coneType, analysisSession)
            }
        }
    }
}