/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId

/**
 * A list of annotations applied for some entity.
 *
 * Annotation owners are usually implement [KtAnnotated]
 */
public abstract class KtAnnotationsList : KtLifetimeOwner {
    /**
     * A list of annotations applied.
     *
     * To check if annotation is present, please use [hasAnnotation].
     *
     * @see KtAnnotationApplication
     */
    public abstract val annotations: List<KtAnnotationApplication>

    /**
     * Checks if entity contains annotation with specified [classId].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.hasAnnotation(classId) == annotationsList.annotations.any { it.classId == classId }
     * ```
     */
    public abstract fun hasAnnotation(classId: ClassId): Boolean

    /**
     * Checks if entity contains annotation with specified [classId] and [useSiteTarget].
     * If [useSiteTarget] is **null** [acceptAnnotationsWithoutUseSite] effectively unused.
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.hasAnnotation(classId, useSiteTarget, strictUseSite) == annotationsList.annotations.any {
     *      (it.useSiteTarget == useSiteTarget || acceptAnnotationsWithoutUseSite && it.useSiteTarget == null) && it.classId == classId
     * }
     * ```
     *
     * @param classId [ClassId] to search
     * @param useSiteTarget specific [AnnotationUseSiteTarget]
     * @param acceptAnnotationsWithoutUseSite add [ClassId] without specified [AnnotationUseSiteTarget] to search
     */
    public abstract fun hasAnnotation(
        classId: ClassId,
        useSiteTarget: AnnotationUseSiteTarget?,
        acceptAnnotationsWithoutUseSite: Boolean = false,
    ): Boolean

    /**
     * A list of annotations applied with specified [classId].
     *
     * To check if annotation is present, please use [hasAnnotation].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.annotationsByClassId(classId) == annotationsList.annotations.filter { it.classId == classId }
     * ```
     *
     * @see KtAnnotationApplication
     */
    public abstract fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication>

    /**
     * A list of annotations [ClassId].
     *
     * To check if annotation is present, please use [hasAnnotation].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.annotationClassIds == annotationsList.annotations.map { it.classId }
     * ```
     */
    public abstract val annotationClassIds: Collection<ClassId>
}

