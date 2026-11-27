/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.mightHaveAnnotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    override val analysisSession: KaFirSession,
) : AbstractList<KaAnnotation>(), KaAnnotationList, KaFirSessionBoundOwner {
    private val backingAnnotations by lazy { annotations(firSymbol, analysisSession = analysisSession) }

    private val useSiteSession: FirSession
        get() = analysisSession.firSession

    override fun isEmpty(): Boolean = entryPointImplementation {
        // isEmpty check needs to be performed on an analyzed declaration
        // (annotations can move to a nested declaration after code analysis).
        // See 'FirTypeResolveTransformer.moveOrDeleteIrrelevantAnnotations()'
        return backingAnnotations.isEmpty()
    }

    override val size: Int
        get() = entryPointImplementation { backingAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = entryPointImplementation {
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = entryPointImplementation {
        return backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = entryPointImplementation {
        return hasAnnotation(firSymbol, classId, useSiteSession)
    }

    override fun get(classId: ClassId): List<KaAnnotation> = entryPointImplementation {
        return annotationsByClassId(firSymbol, classId)
    }

    override val classIds: Collection<ClassId>
        get() = entryPointImplementation {
            annotationClassIds(firSymbol, useSiteSession)
        }

    companion object {
        context(analysisSession: KaFirSession)
        fun create(firSymbol: FirBasedSymbol<*>): KaAnnotationList = when {
            firSymbol.mightHaveAnnotations -> KaFirAnnotationListForDeclaration(firSymbol, analysisSession)
            else -> KaFirEmptyAnnotationList(analysisSession.token)
        }
    }
}
