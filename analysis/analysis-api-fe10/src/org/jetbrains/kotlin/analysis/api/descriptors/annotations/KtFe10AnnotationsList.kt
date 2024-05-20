/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classIdForAnnotation
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtAnnotationInfo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.useSiteTarget
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFe10AnnotationsList private constructor(
    private val fe10Annotations: Annotations,
    private val annotationsToIgnore: Set<ClassId>,
    private val analysisContext: Fe10AnalysisContext
) : KaAnnotationsList() {
    override val token: KaLifetimeToken
        get() = analysisContext.token

    override val annotations: List<KaAnnotationApplicationWithArgumentsInfo>
        get() = withValidityAssertion {
            mapNotIgnoredAnnotationsWithIndex { index, annotation ->
                annotation.toKtAnnotationApplication(analysisContext, index)
            }
        }

    override val annotationInfos: List<KaAnnotationApplicationInfo>
        get() = withValidityAssertion {
            mapNotIgnoredAnnotationsWithIndex { index, annotation ->
                annotation.toKtAnnotationInfo(analysisContext, index)
            }
        }

    override val annotationClassIds: Collection<ClassId>
        get() {
            withValidityAssertion {
                val result = mutableListOf<ClassId>()
                for (annotation in fe10Annotations) {
                    val classId = annotation.classIdForAnnotation ?: continue
                    if (classId in annotationsToIgnore) continue
                    result += classId
                }

                return result
            }
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        fe10Annotations.hasAnnotation(classId.asSingleFqName())
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplicationWithArgumentsInfo> = withValidityAssertion {
        if (classId in annotationsToIgnore) return@withValidityAssertion emptyList()

        fe10Annotations.mapIndexedNotNull { index, annotation ->
            if (!useSiteTargetFilter.isAllowed(annotation.useSiteTarget) || annotation.classIdForAnnotation != classId) {
                return@mapIndexedNotNull null
            }

            annotation.toKtAnnotationApplication(analysisContext, index)
        }
    }

    private fun <T> mapNotIgnoredAnnotationsWithIndex(transformer: (index: Int, annotation: AnnotationDescriptor) -> T?): List<T> {
        var ignoredAnnotationsCounter = 0
        return fe10Annotations.mapIndexedNotNull { index, annotation ->
            if (annotation.classIdForAnnotation in annotationsToIgnore) {
                ignoredAnnotationsCounter++
                null
            } else {
                transformer(index - ignoredAnnotationsCounter, annotation)
            }
        }
    }

    companion object {
        fun create(
            fe10Annotations: Annotations,
            analysisContext: Fe10AnalysisContext,
            ignoreAnnotations: Set<ClassId> = emptySet(),
        ): KaAnnotationsList {
            return if (!fe10Annotations.isEmpty()) {
                KaFe10AnnotationsList(fe10Annotations, ignoreAnnotations, analysisContext)
            } else {
                KaEmptyAnnotationsList(analysisContext.token)
            }
        }
    }
}