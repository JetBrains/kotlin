/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.resolveAnnotationsWithArguments
import org.jetbrains.kotlin.fir.symbols.resolveAnnotationsWithClassIds
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForType private constructor(
    val coneType: ConeKotlinType,
    private val builder: KaSymbolByFirBuilder,
) : KaAnnotationList() {
    override val token: KaLifetimeToken get() = builder.token
    private val useSiteSession: FirSession get() = builder.rootSession

    override val annotations: List<KaAnnotationApplication>
        get() = withValidityAssertion {
            coneType.customAnnotationsWithLazyResolve(FirResolvePhase.ANNOTATION_ARGUMENTS).mapIndexed { index, annotation ->
                annotation.toKtAnnotationApplication(builder, index)
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
    ): List<KaAnnotationApplication> = withValidityAssertion {
        coneType.customAnnotationsWithLazyResolve(FirResolvePhase.ANNOTATION_ARGUMENTS).mapIndexedNotNull { index, annotation ->
            if (!useSiteTargetFilter.isAllowed(annotation.useSiteTarget) || annotation.toAnnotationClassId(useSiteSession) != classId) {
                return@mapIndexedNotNull null
            }

            annotation.toKtAnnotationApplication(builder, index)
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            coneType.customAnnotationsWithLazyResolve(FirResolvePhase.TYPES).mapNotNull { it.toAnnotationClassId(useSiteSession) }
        }

    companion object {
        fun create(coneType: ConeKotlinType, builder: KaSymbolByFirBuilder): KaAnnotationList {
            return if (coneType.customAnnotations.isEmpty()) {
                KaEmptyAnnotationList(builder.token)
            } else {
                KaFirAnnotationListForType(coneType, builder)
            }
        }
    }
}

private fun ConeKotlinType.customAnnotationsWithLazyResolve(phase: FirResolvePhase): List<FirAnnotation> {
    val annotations = customAnnotations.ifEmpty { return emptyList() }

    for (annotation in annotations) {
        val containerSymbol = (annotation as? FirAnnotationCall)?.containingDeclarationSymbol ?: continue
        when (phase) {
            FirResolvePhase.TYPES -> resolveAnnotationsWithClassIds(containerSymbol)
            FirResolvePhase.ANNOTATION_ARGUMENTS -> annotations.resolveAnnotationsWithArguments(containerSymbol)
            else -> {}
        }
    }

    return annotations
}
