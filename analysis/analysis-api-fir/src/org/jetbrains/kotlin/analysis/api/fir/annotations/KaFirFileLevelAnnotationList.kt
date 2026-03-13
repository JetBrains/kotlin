/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId

internal class KaFirFileLevelAnnotationList private constructor(
    private val fileAnnotations: List<FirAnnotation>,
    private val builder: KaSymbolByFirBuilder,
) : AbstractList<KaAnnotation>(), KaAnnotationList {
    private val backingAnnotations by lazy {
        fileAnnotations.map { it.toKaAnnotation(builder) }
    }
    override val token: KaLifetimeToken
        get() = builder.token

    override fun isEmpty(): Boolean = withValidityAssertion {
        backingAnnotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = withValidityAssertion {
        backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        fileAnnotations.any { it.toAnnotationClassId(builder.rootSession) == classId }
    }

    override fun get(classId: ClassId): List<KaAnnotation> = withValidityAssertion {
        backingAnnotations.filter { it.classId == classId }
    }

    override val classIds: Collection<ClassId>
        get() = withValidityAssertion {
            fileAnnotations.mapNotNull { it.toAnnotationClassId(builder.rootSession) }
        }

    companion object {
        fun create(fileAnnotations: List<FirAnnotation>, builder: KaSymbolByFirBuilder): KaAnnotationList {
            return when {
                fileAnnotations.isEmpty() -> KaBaseEmptyAnnotationList(builder.token)
                else -> KaFirFileLevelAnnotationList(fileAnnotations, builder)
            }
        }
    }
}