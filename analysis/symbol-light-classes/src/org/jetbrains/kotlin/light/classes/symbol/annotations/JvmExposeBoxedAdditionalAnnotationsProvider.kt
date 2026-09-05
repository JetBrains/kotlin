/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.JvmStandardClassIds.Annotations.ParameterNames

/**
 * Ensures that every boxed light method exposes [JvmExposeBoxed] in the same form as the corresponding JVM declaration.
 *
 * The provider synthesizes or adjusts the annotation when it cannot be copied from source unchanged:
 *
 * * In [implicit mode][org.jetbrains.kotlin.light.classes.symbol.methods.JvmExposeBoxedMode.IMPLICIT], the source declaration
 * has no [JvmExposeBoxed] annotation, so the provider synthesizes one.
 * * When [JvmName] is present and [JvmExposeBoxed] does not specify a name, the provider copies the [JvmName] argument to
 * [JvmExposeBoxed.jvmName]. An explicitly specified [JvmExposeBoxed] name is preserved.
 *
 * @see org.jetbrains.kotlin.light.classes.symbol.methods.JvmExposeBoxedMode
 */
internal object JvmExposeBoxedAdditionalAnnotationsProvider : AdditionalAnnotationsProvider {
    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement,
    ) {
        if (!owner.parent.isJvmExposeBoxed()) return

        val exposeBoxedQualifier = JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME.asString()
        val jvmNameQualifier = JvmStandardClassIds.JVM_NAME.asString()

        var exposeBoxedAnnotation: SymbolLightLazyAnnotation? = null
        var exposeBoxedAnnotationIndex = -1
        var jvmNameAnnotation: SymbolLightLazyAnnotation? = null
        for (index in currentRawAnnotations.indices) {
            val annotation = currentRawAnnotations[index] as? SymbolLightLazyAnnotation ?: continue
            when (annotation.qualifiedName) {
                exposeBoxedQualifier -> {
                    exposeBoxedAnnotation = annotation
                    exposeBoxedAnnotationIndex = index
                }

                jvmNameQualifier -> jvmNameAnnotation = annotation
            }
        }

        if (exposeBoxedAnnotation?.firstArgument() != null) {
            // Existing @JvmExposeBoxed annotation with explicit name - preserve it
            return
        }

        // Use the name from @JvmName annotation, if present
        val exposedName = jvmNameAnnotation?.firstArgument()?.let { AnnotationArgument(ParameterNames.jvmExposeBoxedName, it.value) }
        if (exposeBoxedAnnotation == null) {
            addSimpleAnnotationIfMissing(
                qualifier = exposeBoxedQualifier,
                currentRawAnnotations = currentRawAnnotations,
                foundQualifiers = foundQualifiers,
                owner = owner,
                arguments = listOfNotNull(exposedName),
            )
        } else if (exposedName != null) {
            currentRawAnnotations[exposeBoxedAnnotationIndex] = SymbolLightSimpleAnnotation(
                fqName = exposeBoxedQualifier,
                parent = owner,
                arguments = listOf(exposedName),
                kotlinOrigin = exposeBoxedAnnotation.kotlinOrigin,
            )
        }
    }

    /**
     * [JvmExposeBoxed] cannot be resolved through the special-qualifier path because its arguments may depend on the owner's
     * other annotations. It is therefore handled in [addAllAnnotations].
     */
    override fun findSpecialAnnotation(
        annotationsBox: GranularAnnotationsBox,
        qualifiedName: String,
        owner: PsiElement,
    ): PsiAnnotation? = null

    override fun isSpecialQualifier(qualifiedName: String): Boolean = false
}

private fun PsiElement.isJvmExposeBoxed(): Boolean = this is SymbolLightMethodBase && isJvmExposeBoxed

private fun SymbolLightLazyAnnotation.firstArgument(): AnnotationArgument? =
    annotationApplicationWithArgumentsInfo.value.annotation.arguments.firstOrNull()
