/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.utils.FirRefWithValidityCheck
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForDeclaration private constructor(
    val firRef: FirRefWithValidityCheck<*>,
    private val useSiteSession: FirSession,
    override val token: ValidityToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = firRef.withFir { fir ->
            fir.annotations.map { annotation ->
                KtFirAnnotationApplicationForDeclaration(firRef, useSiteSession, annotation, token)
            }
        }


    override fun containsAnnotation(classId: ClassId): Boolean {
        return firRef.withFirByType(ResolveType.AnnotationType) { fir ->
            fir.annotations.any { it.fullyExpandedClassId(useSiteSession) == classId }
        }
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = firRef.withFir { fir ->
        fir.annotations.mapNotNull { annotation ->
            if (annotation.fullyExpandedClassId(useSiteSession) != classId) return@mapNotNull null
            KtFirAnnotationApplicationForDeclaration(firRef, useSiteSession, annotation, token)
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = firRef.withFirByType(ResolveType.AnnotationType) { fir ->
            fir.annotations.mapNotNull { it.fullyExpandedClassId(useSiteSession) }
        }

    companion object {
        fun create(
            firRef: FirRefWithValidityCheck<*>,
            useSiteSession: FirSession,
            token: ValidityToken,
        ): KtAnnotationsList {
            return if (firRef.withFir { it.annotations.isEmpty() }) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForDeclaration(firRef, useSiteSession, token)
            }
        }
    }
}
