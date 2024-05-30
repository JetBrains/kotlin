/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classIdForAnnotation
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.useSiteTarget
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId

internal class KaFe10AnnotationList private constructor(
    private val fe10Annotations: Annotations,
    private val analysisContext: Fe10AnalysisContext,
    private val ignoredAnnotations: Set<ClassId> = emptySet()
) : AbstractList<KaAnnotationApplication>(), KaAnnotationList {
    private val backingAnnotations: List<KaAnnotationApplication> by lazy {
        buildList {
            fe10Annotations.forEachIndexed { index, annotationDescriptor ->
                if (annotationDescriptor.classIdForAnnotation !in ignoredAnnotations) {
                    add(annotationDescriptor.toKtAnnotationApplication(analysisContext, index))
                }
            }
        }
    }

    override val token: KaLifetimeToken
        get() = analysisContext.token

    override fun isEmpty(): Boolean = withValidityAssertion {
        return if (ignoredAnnotations.isEmpty()) {
            fe10Annotations.isEmpty()
        } else {
            backingAnnotations.isEmpty()
        }
    }

    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun iterator(): Iterator<KaAnnotationApplication> = withValidityAssertion {
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotationApplication = withValidityAssertion {
        return backingAnnotations[index]
    }

    override val annotationClassIds: Set<ClassId>
        get() = withValidityAssertion {
            buildSet {
                for (annotation in fe10Annotations) {
                    val classId = annotation.classIdForAnnotation ?: continue
                    if (classId in ignoredAnnotations) continue
                    add(classId)
                }
            }
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        fe10Annotations.hasAnnotation(classId.asSingleFqName())
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplication> = withValidityAssertion {
        if (classId in ignoredAnnotations) return@withValidityAssertion emptyList()

        fe10Annotations.mapIndexedNotNull { index, annotation ->
            if (!useSiteTargetFilter.isAllowed(annotation.useSiteTarget) || annotation.classIdForAnnotation != classId) {
                return@mapIndexedNotNull null
            }

            annotation.toKtAnnotationApplication(analysisContext, index)
        }
    }

    companion object {
        fun create(
            fe10Annotations: Annotations,
            analysisContext: Fe10AnalysisContext,
            ignoredAnnotations: Set<ClassId> = emptySet(),
        ): KaAnnotationList {
            return if (!fe10Annotations.isEmpty()) {
                KaFe10AnnotationList(fe10Annotations, analysisContext, ignoredAnnotations)
            } else {
                KaEmptyAnnotationList(analysisContext.token)
            }
        }
    }
}