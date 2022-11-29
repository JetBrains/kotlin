/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

internal class KtFe10AnnotationsList private constructor(
    private val fe10Annotations: Annotations,
    private val annotationsToIgnore: Set<ClassId>,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            fe10Annotations.mapNotNull { annotation ->
                if (annotation.annotationClass.classId in annotationsToIgnore) null
                else annotation.toKtAnnotationApplication()
            }
        }

    override val annotationClassIds: Collection<ClassId>
        get() {
            withValidityAssertion {
                val result = mutableListOf<ClassId>()
                for (annotation in fe10Annotations) {
                    val annotationClass = annotation.annotationClass ?: continue
                    val classId = annotationClass.maybeLocalClassId
                    if (classId in annotationsToIgnore) continue
                    result += classId
                }
                return result
            }
        }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        return fe10Annotations.hasAnnotation(classId.asSingleFqName())
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        if (classId in annotationsToIgnore) return@withValidityAssertion emptyList()
        fe10Annotations.mapNotNull { annotation ->
            if (annotation.annotationClass?.maybeLocalClassId != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication()
        }
    }

    companion object {
        fun create(
            fe10Annotations: Annotations,
            token: KtLifetimeToken,
            ignoreAnnotations: Set<ClassId> = emptySet(),
        ): KtAnnotationsList {
            return if (!fe10Annotations.isEmpty()) {
                KtFe10AnnotationsList(fe10Annotations, ignoreAnnotations, token)
            } else {
                KtEmptyAnnotationsList(token)
            }
        }
    }
}