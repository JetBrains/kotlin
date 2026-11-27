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
import org.jetbrains.kotlin.analysis.api.fir.toKaAnnotation
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * Implementation of the file annotation list for decompiled KLibs.
 * All [fileAnnotations] passed to [create] must be fully resolved (in the decompiled libraries, they always are).
 */
internal class KaKlibDecompiledFileAnnotationList private constructor(
    private val fileAnnotations: List<FirAnnotation>,
    override val analysisSession: KaFirSession,
) : AbstractList<KaAnnotation>(), KaAnnotationList, KaFirSessionBoundOwner {
    private val backingAnnotations by lazy {
        fileAnnotations.map { it.toKaAnnotation(analysisSession = analysisSession) }
    }

    override fun isEmpty(): Boolean = entryPointImplementation {
        backingAnnotations.isEmpty()
    }

    override val size: Int
        get() = entryPointImplementation { backingAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = entryPointImplementation {
        backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = entryPointImplementation {
        backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = entryPointImplementation {
        fileAnnotations.any { it.toAnnotationClassId(analysisSession.firSession) == classId }
    }

    override fun get(classId: ClassId): List<KaAnnotation> = entryPointImplementation {
        backingAnnotations.filter { it.classId == classId }
    }

    override val classIds: Collection<ClassId>
        get() = entryPointImplementation {
            fileAnnotations.mapNotNull { it.toAnnotationClassId(analysisSession.firSession) }
        }

    companion object {
        context(analysisSession: KaFirSession)
        fun create(fileAnnotations: List<FirAnnotation>): KaAnnotationList {
            return when {
                fileAnnotations.isEmpty() -> KaFirEmptyAnnotationList(analysisSession.token)
                else -> KaKlibDecompiledFileAnnotationList(fileAnnotations, analysisSession)
            }
        }
    }
}