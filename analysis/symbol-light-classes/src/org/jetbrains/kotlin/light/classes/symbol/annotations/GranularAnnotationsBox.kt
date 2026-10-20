/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.light.classes.symbol.utils.toArrayIfNotEmptyOrDefault
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

internal class GranularAnnotationsBox(
    private val annotationsProvider: AnnotationsProvider,
    private val additionalAnnotationsProvider: AdditionalAnnotationsProvider = EmptyAdditionalAnnotationsProvider,
    private val annotationFilter: AnnotationFilter = AlwaysAllowedAnnotationFilter,
) : AnnotationsBox {
    @Volatile
    private var cachedAnnotations: Collection<PsiAnnotation>? = null

    private fun getOrComputeCachedAnnotations(owner: PsiElement): Collection<PsiAnnotation> {
        cachedAnnotations?.let { return it }

        val annotations = annotationsProvider.annotationInfos().mapTo(SmartList<PsiAnnotation>()) { applicationInfo ->
            SymbolLightLazyAnnotation(annotationsProvider, applicationInfo, owner)
        }

        val foundQualifiers = annotations.mapNotNullTo(hashSetOf()) { it.qualifiedName }
        additionalAnnotationsProvider.addAllAnnotations(annotations, foundQualifiers, owner)

        val resultAnnotations = annotationFilter.filtered(annotations)
        fieldUpdater.compareAndSet(this, null, resultAnnotations)

        return getOrComputeCachedAnnotations(owner)
    }

    override fun annotationsArray(owner: PsiElement): Array<PsiAnnotation> {
        return getOrComputeCachedAnnotations(owner).toArrayIfNotEmptyOrDefault(PsiAnnotation.EMPTY_ARRAY)
    }

    override fun findAnnotation(owner: PsiElement, qualifiedName: String): PsiAnnotation? {
        if (!annotationFilter.isAllowed(qualifiedName)) return null

        cachedAnnotations?.let { annotations ->
            return annotations.find { it.qualifiedName == qualifiedName }
        }

        specialAnnotationsListWithSafeArgumentsResolve[qualifiedName]?.let { specialAnnotationClassId ->
            annotationsProvider[specialAnnotationClassId].firstOrNull()?.let { annotationApplication ->
                return SymbolLightLazyAnnotation(annotationsProvider, annotationApplication, owner)
            }

            return additionalAnnotationsProvider.findSpecialAnnotation(qualifiedName, owner)
        }

        return getOrComputeCachedAnnotations(owner).find { it.qualifiedName == qualifiedName }
    }

    override fun hasAnnotation(owner: PsiElement, qualifiedName: String): Boolean {
        if (!annotationFilter.isAllowed(qualifiedName)) return false

        cachedAnnotations?.let { annotations ->
            return annotations.any { it.qualifiedName == qualifiedName }
        }

        specialAnnotationsList[qualifiedName]?.let { specialAnnotationClassId ->
            return specialAnnotationClassId in annotationsProvider ||
                    additionalAnnotationsProvider.findSpecialAnnotation(qualifiedName, owner) != null
        }

        return getOrComputeCachedAnnotations(owner).any { it.qualifiedName == qualifiedName }
    }

    companion object {
        private val fieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularAnnotationsBox::class.java,
            /* vclass = */ Collection::class.java,
            /* fieldName = */ GranularAnnotationsBox::cachedAnnotations.name,
        )

        /**
         * Special annotations without parameters, which [GranularAnnotationsBox.findAnnotation] looks up among the declared
         * annotations only.
         *
         * Unlike [GranularAnnotationsBox.hasAnnotation], [GranularAnnotationsBox.findAnnotation] converts the arguments of the found
         * annotation, which would resolve them for an annotation with parameters.
         */
        private val specialAnnotationsListWithSafeArgumentsResolve: Map<String, ClassId> = listOf(
            JvmStandardClassIds.Annotations.JvmRecord,
            JvmStandardClassIds.Annotations.JvmInline,
        ).associateBy { it.asFqNameString() }

        /**
         * Special annotations, which [GranularAnnotationsBox.hasAnnotation] looks up among the declared annotations only, without
         * computing all annotations.
         *
         * Each of them must be an annotation which the compiler resolves in the `COMPILER_REQUIRED_ANNOTATIONS` phase instead of
         * `TYPES` (see `FirAnnotationsPlatformSpecificSupportComponent.requiredAnnotations`). An [AdditionalAnnotationsProvider]
         * which adds one of them must override [AdditionalAnnotationsProvider.findSpecialAnnotation].
         */
        private val specialAnnotationsList: Map<String, ClassId> = listOf(
            StandardClassIds.Annotations.Deprecated,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.SinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.Target,
            StandardClassIds.Annotations.EqualityBound,
            StandardClassIds.Annotations.NumericClass,
            JvmStandardClassIds.Annotations.JvmName,
        ).associateBy { it.asFqNameString() } + specialAnnotationsListWithSafeArgumentsResolve
    }
}
