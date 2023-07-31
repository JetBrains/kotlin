/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType

internal object ExpectActualAnnotationIncompatibilityDiagnosticRenderers {
    private val descriptorRender = DescriptorRenderer.withOptions {
        annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
        modifiers = emptySet()
        withDefinedIn = true
        classifierNamePolicy = ClassifierNamePolicy.SHORT
    }

    @JvmField
    val DESCRIPTOR_RENDERER = descriptorRender.asRenderer()

    @JvmField
    val INCOMPATIBILITY = Renderer { incompatibilityType: ExpectActualAnnotationsIncompatibilityType<AnnotationDescriptor> ->
        val sb = StringBuilder("Annotation `")
            .append(descriptorRender.renderAnnotation(incompatibilityType.expectAnnotation))
            .append("` ")
        when (incompatibilityType) {
            is ExpectActualAnnotationsIncompatibilityType.MissingOnActual -> {
                sb.append("is missing on actual declaration")
            }
            is ExpectActualAnnotationsIncompatibilityType.DifferentOnActual -> {
                sb.append("has different arguments on actual declaration: `")
                    .append(descriptorRender.renderAnnotation(incompatibilityType.actualAnnotation))
                    .append("`")
            }
        }
        sb.toString()
    }
}
