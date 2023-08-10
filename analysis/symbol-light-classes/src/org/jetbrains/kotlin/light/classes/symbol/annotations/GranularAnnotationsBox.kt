/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
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

    private fun getOrComputeCachedAnnotations(owner: PsiModifierList): Collection<PsiAnnotation> {
        cachedAnnotations?.let { return it }

        val annotations = annotationsProvider.annotationInfos().mapNotNullTo(SmartList<PsiAnnotation>()) { applicationInfo ->
            applicationInfo.classId?.let { _ ->
                SymbolLightLazyAnnotation(annotationsProvider, applicationInfo, owner)
            }
        }

        val foundQualifiers = annotations.mapNotNullTo(hashSetOf()) { it.qualifiedName }
        additionalAnnotationsProvider.addAllAnnotations(annotations, foundQualifiers, owner)

        val resultAnnotations = annotationFilter.filtered(annotations)
        fieldUpdater.compareAndSet(this, null, resultAnnotations)

        return getOrComputeCachedAnnotations(owner)
    }

    override fun annotationsArray(owner: PsiModifierList): Array<PsiAnnotation> {
        return getOrComputeCachedAnnotations(owner).toArrayIfNotEmptyOrDefault(PsiAnnotation.EMPTY_ARRAY)
    }

    override fun findAnnotation(
        owner: PsiModifierList,
        qualifiedName: String,
    ): PsiAnnotation? = findAnnotation(owner, qualifiedName, withAdditionalAnnotations = true)

    fun findAnnotation(owner: PsiModifierList, qualifiedName: String, withAdditionalAnnotations: Boolean): PsiAnnotation? {
        if (!annotationFilter.isAllowed(qualifiedName)) return null

        cachedAnnotations?.let { annotations ->
            return annotations.find { it.qualifiedName == qualifiedName }
        }

        specialAnnotationsListWithSafeArgumentsResolve[qualifiedName]?.let { specialAnnotationClassId ->
            val annotationApplication = annotationsProvider[specialAnnotationClassId].firstOrNull() ?: return null
            return SymbolLightLazyAnnotation(annotationsProvider, annotationApplication, owner)
        }

        if (withAdditionalAnnotations && additionalAnnotationsProvider.isSpecialQualifier(qualifiedName)) {
            return additionalAnnotationsProvider.findSpecialAnnotation(this, qualifiedName, owner)
        }

        return getOrComputeCachedAnnotations(owner).find { it.qualifiedName == qualifiedName }
    }

    override fun hasAnnotation(owner: PsiModifierList, qualifiedName: String): Boolean {
        if (!annotationFilter.isAllowed(qualifiedName)) return false

        cachedAnnotations?.let { annotations ->
            return annotations.any { it.qualifiedName == qualifiedName }
        }

        val specialAnnotationClassId = specialAnnotationsList[qualifiedName]
        return if (specialAnnotationClassId != null) {
            specialAnnotationClassId in annotationsProvider
        } else {
            getOrComputeCachedAnnotations(owner).any { it.qualifiedName == qualifiedName }
        }
    }

    companion object {
        private val fieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularAnnotationsBox::class.java,
            /* vclass = */ Collection::class.java,
            /* fieldName = */ "cachedAnnotations",
        )

        /**
         * We can safety reduce resolve only for annotations without arguments
         *
         * @see org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
         */
        private val specialAnnotationsListWithSafeArgumentsResolve: Map<String, ClassId> = listOf(
            JvmStandardClassIds.Annotations.JvmRecord,
        ).associateBy { it.asFqNameString() }

        /**
         * @see org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
         */
        private val specialAnnotationsList: Map<String, ClassId> = listOf(
            StandardClassIds.Annotations.Deprecated,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.Target,
        ).associateBy { it.asFqNameString() } + specialAnnotationsListWithSafeArgumentsResolve
    }
}
