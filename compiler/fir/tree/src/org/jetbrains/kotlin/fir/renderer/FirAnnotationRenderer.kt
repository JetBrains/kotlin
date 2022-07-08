/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference

open class FirAnnotationRenderer {

    internal lateinit var components: FirRendererComponents
    protected val visitor get() = components.visitor
    protected val printer get() = components.printer
    protected val callArgumentsRenderer get() = components.callArgumentsRenderer

    fun render(annotationContainer: FirAnnotationContainer) {
        renderAnnotations(annotationContainer.annotations)
    }

    internal fun renderAnnotations(annotations: List<FirAnnotation>) {
        for (annotation in annotations) {
            renderAnnotation(annotation)
        }
    }

    internal fun renderAnnotation(annotation: FirAnnotation) {
        printer.print("@")
        annotation.useSiteTarget?.let {
            printer.print(it.name)
            printer.print(":")
        }
        annotation.annotationTypeRef.accept(visitor)
        when (annotation) {
            is FirAnnotationCall -> if (annotation.calleeReference.let { it is FirResolvedNamedReference || it is FirErrorNamedReference }) {
                callArgumentsRenderer.renderArgumentMapping(annotation.argumentMapping)
            } else {
                visitor.visitCall(annotation)
            }
            else -> callArgumentsRenderer.renderArgumentMapping(annotation.argumentMapping)

        }
        if (annotation.useSiteTarget == AnnotationUseSiteTarget.FILE) {
            printer.println()
        } else {
            printer.print(" ")
        }
    }
}
