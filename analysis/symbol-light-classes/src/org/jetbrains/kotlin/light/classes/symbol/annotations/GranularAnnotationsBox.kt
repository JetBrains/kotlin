/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
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

    /**
     * The special annotations found by [findAnnotation] before all the annotations are computed, or [NoAnnotation] for an absent one.
     * Each lookup restores the annotated symbol, so the results are kept for the lifetime of the box, as [cachedAnnotations] are.
     */
    @Volatile
    private var specialAnnotations: PersistentMap<ClassId, Any> = persistentHashMapOf()

    /**
     * The results of [hasAnnotation] for the special annotations before all the annotations are computed.
     *
     * @see specialAnnotations
     */
    @Volatile
    private var specialAnnotationPresence: PersistentMap<ClassId, Boolean> = persistentHashMapOf()

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
            return findSpecialAnnotation(specialAnnotationClassId, owner)
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
            hasSpecialAnnotation(specialAnnotationClassId)
        } else {
            getOrComputeCachedAnnotations(owner).any { it.qualifiedName == qualifiedName }
        }
    }

    private fun findSpecialAnnotation(classId: ClassId, owner: PsiElement): PsiAnnotation? {
        specialAnnotations[classId]?.let { return it as? PsiAnnotation }

        val annotation = annotationsProvider[classId].firstOrNull()?.let { SymbolLightLazyAnnotation(annotationsProvider, it, owner) }
        while (true) {
            val current = specialAnnotations
            // Another thread might have stored its annotation, which is returned to keep the result stable
            current[classId]?.let { return it as? PsiAnnotation }
            if (specialAnnotationsUpdater.compareAndSet(this, current, current.putting(classId, annotation ?: NoAnnotation))) {
                return annotation
            }
        }
    }

    private fun hasSpecialAnnotation(classId: ClassId): Boolean {
        specialAnnotationPresence[classId]?.let { return it }

        val isPresent = classId in annotationsProvider
        while (true) {
            val current = specialAnnotationPresence
            if (current.containsKey(classId)) return current.getValue(classId)
            if (specialAnnotationPresenceUpdater.compareAndSet(this, current, current.putting(classId, isPresent))) {
                return isPresent
            }
        }
    }

    private object NoAnnotation

    companion object {
        private val fieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularAnnotationsBox::class.java,
            /* vclass = */ Collection::class.java,
            /* fieldName = */ GranularAnnotationsBox::cachedAnnotations.name,
        )

        private val specialAnnotationsUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularAnnotationsBox::class.java,
            /* vclass = */ PersistentMap::class.java,
            /* fieldName = */ GranularAnnotationsBox::specialAnnotations.name,
        )

        private val specialAnnotationPresenceUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularAnnotationsBox::class.java,
            /* vclass = */ PersistentMap::class.java,
            /* fieldName = */ GranularAnnotationsBox::specialAnnotationPresence.name,
        )

        /**
         * We can safety reduce resolve only for annotations without arguments
         *
         * @see org.jetbrains.kotlin.fir.declarations.FirAnnotationsPlatformSpecificSupportComponent
         */
        private val specialAnnotationsListWithSafeArgumentsResolve: Map<String, ClassId> = listOf(
            JvmStandardClassIds.Annotations.JvmRecord,
            JvmStandardClassIds.Annotations.JvmInline,
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
            StandardClassIds.Annotations.EqualityBound,
            StandardClassIds.Annotations.NumericClass,
        ).associateBy { it.asFqNameString() } + specialAnnotationsListWithSafeArgumentsResolve
    }
}
