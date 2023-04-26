/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationInfo
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.resolveAnnotationsWithArguments
import org.jetbrains.kotlin.fir.symbols.resolveAnnotationsWithClassIds
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.custom
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForType private constructor(
    val coneType: ConeKotlinType,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplicationWithArgumentsInfo>
        get() = withValidityAssertion {
            coneType.customAnnotationsWithLazyResolve(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING).mapIndexed { index, annotation ->
                annotation.toKtAnnotationApplication(useSiteSession, index)
            }
        }

    override val annotationInfos: List<KtAnnotationApplicationInfo>
        get() = withValidityAssertion {
            coneType.customAnnotationsWithLazyResolve(FirResolvePhase.TYPES).mapIndexed { index, annotation ->
                annotation.toKtAnnotationInfo(useSiteSession, index)
            }
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        coneType.customAnnotationsWithLazyResolve(FirResolvePhase.TYPES).any {
            useSiteTargetFilter.isAllowed(it.useSiteTarget) && it.toAnnotationClassId(useSiteSession) == classId
        }
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KtAnnotationApplicationWithArgumentsInfo> = withValidityAssertion {
        coneType.customAnnotationsWithLazyResolve(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING).mapIndexedNotNull { index, annotation ->
            if (!useSiteTargetFilter.isAllowed(annotation.useSiteTarget) || annotation.toAnnotationClassId(useSiteSession) != classId) {
                return@mapIndexedNotNull null
            }

            annotation.toKtAnnotationApplication(useSiteSession, index)
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            coneType.customAnnotationsWithLazyResolve(FirResolvePhase.TYPES).mapNotNull { it.toAnnotationClassId(useSiteSession) }
        }

    companion object {
        fun create(
            coneType: ConeKotlinType,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            return if (coneType.customAnnotations.isEmpty()) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForType(coneType, useSiteSession, token)
            }
        }
    }
}

private fun ConeKotlinType.customAnnotationsWithLazyResolve(phase: FirResolvePhase): List<FirAnnotation> {
    val custom = attributes.custom ?: return emptyList()
    val annotations = custom.annotations.ifEmpty { return emptyList() }

    for (containerSymbol in custom.containerSymbols) {
        when (phase) {
            FirResolvePhase.TYPES -> resolveAnnotationsWithClassIds(containerSymbol)
            FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING -> annotations.resolveAnnotationsWithArguments(containerSymbol)
            else -> {}
        }
    }

    return annotations
}
