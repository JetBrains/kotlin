/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass

internal class KtFe10AnnotationsList private constructor(
    private val fe10Annotations: Annotations,
    override val token: ValidityToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            fe10Annotations.map { KtFe10DescAnnotationApplication(it, token) }
        }

    override val annotationClassIds: Collection<ClassId>
        get() {
            withValidityAssertion {
                val result = mutableListOf<ClassId>()
                for (annotation in fe10Annotations) {
                    val annotationClass = annotation.annotationClass ?: continue
                    result += annotationClass.maybeLocalClassId
                }
                return result
            }
        }

    override fun containsAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        return fe10Annotations.hasAnnotation(classId.asSingleFqName())
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        fe10Annotations.mapNotNull { annotation ->
            if (annotation.annotationClass?.maybeLocalClassId != classId) return@mapNotNull null
            KtFe10DescAnnotationApplication(annotation, token)
        }
    }

    companion object {
        fun create(
            fe10Annotations: Annotations,
            token: ValidityToken,
        ): KtAnnotationsList {
            return if (!fe10Annotations.isEmpty()) {
                KtFe10AnnotationsList(fe10Annotations, token)
            } else {
                KtEmptyAnnotationsList(token)
            }
        }
    }
}