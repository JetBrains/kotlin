/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.atomic.AtomicReference

internal class LazyAnnotationsBox(
    private val annotationsProvider: AnnotationsProvider,
    private val additionalAnnotationsProvider: AdditionalAnnotationsProvider = EmptyAdditionalAnnotationsProvider,
    private val annotationFilter: AnnotationFilter = AlwaysAllowedAnnotationFilter,
) : AnnotationsBox {
    private val annotationsArray: AtomicReference<Array<PsiAnnotation>?> = AtomicReference()

    override fun annotations(owner: PsiModifierList): Array<PsiAnnotation> {
        annotationsArray.get()?.let { return it }

        val annotations = annotationsProvider.annotationInfos().mapNotNullTo(SmartList<PsiAnnotation>()) { applicationInfo ->
            applicationInfo.classId?.let { _ ->
                SymbolLightLazyAnnotation(annotationsProvider, applicationInfo, owner)
            }
        }

        val foundQualifiers = annotations.mapNotNullTo(hashSetOf()) { it.qualifiedName }
        additionalAnnotationsProvider.addAllAnnotations(annotations, foundQualifiers, owner)

        val resultAnnotations = annotationFilter.filtered(annotations)
        return setAnnotationsArray(
            if (resultAnnotations.isNotEmpty()) resultAnnotations.toTypedArray<PsiAnnotation>() else PsiAnnotation.EMPTY_ARRAY
        )
    }

    private fun setAnnotationsArray(array: Array<PsiAnnotation>): Array<PsiAnnotation> =
        if (annotationsArray.compareAndSet(null, array)) {
            array
        } else {
            annotationsArray.get() ?: error("Unexpected state")
        }

    override fun findAnnotation(
        owner: PsiModifierList,
        qualifiedName: String,
    ): PsiAnnotation? = findAnnotation(owner, qualifiedName, withAdditionalAnnotations = true)

    fun findAnnotation(owner: PsiModifierList, qualifiedName: String, withAdditionalAnnotations: Boolean): PsiAnnotation? {
        if (!annotationFilter.isAllowed(qualifiedName)) return null

        annotationsArray.get()?.let { array ->
            return array.find { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        val specialAnnotation = if (specialAnnotationClassId != null) {
            val annotationApplication = annotationsProvider[specialAnnotationClassId].firstOrNull() ?: return null
            SymbolLightLazyAnnotation(annotationsProvider, annotationApplication, owner)
        } else if (withAdditionalAnnotations) {
            additionalAnnotationsProvider.findAdditionalAnnotation(this, qualifiedName, owner)
        } else {
            null
        }

        return specialAnnotation ?: annotations(owner).find { it.qualifiedName == qualifiedName }
    }

    override fun hasAnnotation(owner: PsiModifierList, qualifiedName: String): Boolean {
        if (!annotationFilter.isAllowed(qualifiedName)) return false

        annotationsArray.get()?.let { array ->
            return array.any { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        return if (specialAnnotationClassId != null) {
            specialAnnotationClassId in annotationsProvider
        } else {
            annotations(owner).any { it.qualifiedName == qualifiedName }
        }
    }

    companion object {
        /**
         * @see org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
         */
        private val specialAnnotationsList: Map<String, ClassId> = listOf(
            StandardClassIds.Annotations.Deprecated,
//        Annotations.Retention,
//        Annotations.Java.Retention,
//        Annotations.Target,
//        Annotations.Java.Target,
//        Annotations.Java.Override,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.JvmRecord,
//        Annotations.Repeatable,
//        Annotations.Java.Repeatable,
        ).associateBy { it.asFqNameString() }
    }
}
