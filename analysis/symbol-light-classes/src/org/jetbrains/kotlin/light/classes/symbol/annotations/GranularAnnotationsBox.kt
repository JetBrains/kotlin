/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
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
    var cachedAnnotations: Collection<PsiAnnotation>? = null

    val trace = Throwable()

    @get:Synchronized
    val annotationInfo: MutableList<String> = mutableListOf()

    @Volatile
    var cachePopulatedTrace: Throwable? = null

    private fun getOrComputeCachedAnnotations(owner: PsiElement): Collection<PsiAnnotation> {
        annotationInfo.add("Owner: ${owner.text}")
        cachedAnnotations?.let {
            annotationInfo.add("Cached: ${it.size} annotations. ${it.joinToString(System.lineSeparator()) { it.text }}")
            return it
        }

        annotationInfo.add("Annotation provider ${annotationsProvider::class.java.canonicalName} has ${annotationsProvider.annotationInfos().size} annotations")

        if (annotationsProvider is SymbolAnnotationsProvider<*>) {
            annotationInfo.add(
                annotationsProvider.lastAccessedAnnotationIds
                    ?.joinToString(prefix = "Annotation provider annotation IDs: ", separator = System.lineSeparator()).orEmpty()
            )
            annotationInfo.add(
                annotationsProvider.lastAnnotatedSymbolClassAsString.let { "Last annotated symbol class: $it" }
            )

            annotationInfo.add(
                annotationsProvider.lastSymbolAnnotationClass.let { "Last annotated symbol's annotations class: $it" }
            )

            annotationInfo.add(
                annotationsProvider.lastExtraSymbolInfo.let { "Last info from the depths of FIR: ${it?.joinToString(prefix = "[\n", postfix = "]\n", separator = System.lineSeparator())}" }
            )

            annotationInfo.add(
                annotationsProvider.owningPropertyInfoForBackingField.let { "Owning property info for backing field: ${it?.joinToString(prefix = "[\n", postfix = "]\n", separator = System.lineSeparator())}" }
            )
        }

        val annotations = annotationsProvider.annotationInfos().mapNotNullTo(SmartList<PsiAnnotation>()) { applicationInfo ->
            applicationInfo.annotation.classId?.let { _ ->
                SymbolLightLazyAnnotation(annotationsProvider, applicationInfo, owner)
            }
        }

        val foundQualifiers = annotations.mapNotNullTo(hashSetOf()) { it.qualifiedName }
        additionalAnnotationsProvider.addAllAnnotations(annotations, foundQualifiers, owner)

        val resultAnnotations = annotationFilter.filtered(annotations)
        fieldUpdater.compareAndSet(this, null, resultAnnotations)
        cachePopulatedTrace = Throwable()

        return getOrComputeCachedAnnotations(owner)
    }

    override fun annotationsArray(owner: PsiElement): Array<PsiAnnotation> {
        return getOrComputeCachedAnnotations(owner).toArrayIfNotEmptyOrDefault(PsiAnnotation.EMPTY_ARRAY)
    }

    override fun findAnnotation(
        owner: PsiElement,
        qualifiedName: String,
    ): PsiAnnotation? = findAnnotation(owner, qualifiedName, withAdditionalAnnotations = true)

    fun findAnnotation(owner: PsiElement, qualifiedName: String, withAdditionalAnnotations: Boolean): PsiAnnotation? {
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

    override fun hasAnnotation(owner: PsiElement, qualifiedName: String): Boolean {
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
            /* fieldName = */ GranularAnnotationsBox::cachedAnnotations.name,
        )

        /**
         * We can safety reduce resolve only for annotations without arguments
         *
         * @see org.jetbrains.kotlin.fir.declarations.FirAnnotationsPlatformSpecificSupportComponent
         */
        private val specialAnnotationsListWithSafeArgumentsResolve: Map<String, ClassId> = listOf(
            JvmStandardClassIds.Annotations.JvmRecord,
        ).associateBy { it.asFqNameString() }

        /**
         * @see org.jetbrains.kotlin.fir.declarations.FirAnnotationsPlatformSpecificSupportComponent
         */
        private val specialAnnotationsList: Map<String, ClassId> = listOf(
            StandardClassIds.Annotations.Deprecated,
            StandardClassIds.Annotations.DeprecatedSinceKotlin,
            StandardClassIds.Annotations.WasExperimental,
            StandardClassIds.Annotations.Target,
            StandardClassIds.Annotations.IntroducedAt,
        ).associateBy { it.asFqNameString() } + specialAnnotationsListWithSafeArgumentsResolve
    }
}
