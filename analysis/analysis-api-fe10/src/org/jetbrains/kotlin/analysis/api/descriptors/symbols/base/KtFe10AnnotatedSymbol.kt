/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.base

import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescAnnotationCall
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass

internal interface KtFe10AnnotatedSymbol : KtAnnotatedSymbol, KtFe10Symbol {
    val annotationsObject: Annotations

    override val annotations: List<KtAnnotationCall>
        get() = withValidityAssertion {
            annotationsObject.map { KtFe10DescAnnotationCall(it, token) }
        }

    override val annotationClassIds: Collection<ClassId>
        get() {
            withValidityAssertion {
                val result = mutableListOf<ClassId>()
                for (annotation in annotationsObject) {
                    val annotationClass = annotation.annotationClass ?: continue
                    result += annotationClass.maybeLocalClassId
                }
                return result
            }
        }

    override fun containsAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        return annotationsObject.hasAnnotation(classId.asSingleFqName())
    }
}