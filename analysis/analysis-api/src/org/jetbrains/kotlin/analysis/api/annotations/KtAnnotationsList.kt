/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.name.ClassId

/**
 * A list of annotations applied for some entity.
 *
 * Annotation owners are usually implement [KaAnnotated]
 */
public interface KaAnnotationList : List<KaAnnotationApplication>, KaLifetimeOwner {
    @Deprecated("Use the annotation list as a 'List'.")
    public val annotations: List<KaAnnotationApplication>
        get() = this

    @Deprecated("Use the annotation list as a 'List'.")
    public val annotationInfos: List<KaAnnotationApplication>
        get() = this

    /**
     * Checks if entity contains annotation with specified [classId] and filtered by [useSiteTargetFilter].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.hasAnnotation(classId, useSiteTargetFilter) == annotationsList.annotations.any {
     *   it.classId == classId && useSiteTargetFilter.isAllowed(it.useSiteTarget)
     * }
     * ```
     * @param classId [ClassId] to search
     * @param useSiteTargetFilter specific [AnnotationUseSiteTargetFilter]
     */
    public fun hasAnnotation(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
    ): Boolean

    /**
     * A list of annotations applied with specified [classId] and filtered by [useSiteTargetFilter].
     *
     * To check if annotation is present, please use [hasAnnotation].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.annotationsByClassId(classId) == annotationsList.annotations.filter {
     *   it.classId == classId && useSiteTargetFilter.isAllowed(it.useSiteTarget)
     * }
     * ```
     */
    public fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplication>

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
    public val classIds: Collection<ClassId>

    @Deprecated("Use 'classIds' instead.", replaceWith = ReplaceWith("classIds"))
    public val annotationClassIds: Collection<ClassId>
        get() = classIds
}

public typealias KtAnnotationsList = KaAnnotationList

public typealias KaAnnotationsList = KaAnnotationList