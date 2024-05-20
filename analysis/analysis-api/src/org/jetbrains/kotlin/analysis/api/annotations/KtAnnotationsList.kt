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
public abstract class KaAnnotationsList : KaLifetimeOwner {
    /**
     * A list of annotations applied.
     *
     * To check if annotation is present, please use [hasAnnotation].
     * [annotationInfos] is more preferable if suits your needs as a lightweight.
     *
     * @see KaAnnotationApplication
     */
    public abstract val annotations: List<KaAnnotationApplicationWithArgumentsInfo>

    /**
     * A list of annotation infos.
     *
     * Can be used instead of [annotations] if applicable to reduce resolve.
     *
     * @see KaAnnotationApplicationInfo
     */
    public abstract val annotationInfos: List<KaAnnotationApplicationInfo>

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
    public abstract fun hasAnnotation(
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
     *
     * @see KaAnnotationApplicationWithArgumentsInfo
     */
    public abstract fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplicationWithArgumentsInfo>

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

public typealias KtAnnotationsList = KaAnnotationsList