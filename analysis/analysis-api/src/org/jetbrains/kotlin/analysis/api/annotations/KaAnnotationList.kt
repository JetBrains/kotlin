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
public interface KaAnnotationList : List<KaAnnotation>, KaLifetimeOwner {
    @Deprecated("Use the annotation list as a 'List'.")
    public val annotations: List<KaAnnotation>
        get() = this

    @Deprecated("Use the annotation list as a 'List'.")
    public val annotationInfos: List<KaAnnotation>
        get() = this

    /**
     * Checks if entity contains annotation with specified [classId].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.hasAnnotation(classId) == annotationsList.annotations.any { it.classId == classId }
     * ```
     * @param classId [ClassId] to search
     */
    public operator fun contains(classId: ClassId): Boolean

    @Deprecated("Use 'contains' instead.", replaceWith = ReplaceWith("contains(classId)"))
    public fun hasAnnotation(classId: ClassId): Boolean = contains(classId)

    /**
     * A list of annotations applied with specified [classId].
     *
     * To check if annotation is present, please use [contains].
     *
     * The semantic is equivalent to
     * ```
     * annotationsList.annotationsByClassId(classId) == annotationsList.annotations.filter { it.classId == classId }
     * ```
     */
    public operator fun get(classId: ClassId): List<KaAnnotation>

    @Deprecated("Use 'get' instead.", replaceWith = ReplaceWith("get(classId)"))
    public fun annotationsByClassId(classId: ClassId): List<KaAnnotation> = get(classId)

    /**
     * A list of annotations [ClassId].
     *
     * To check if annotation is present, please use [contains].
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

@Deprecated("Use 'KaAnnotationList' instead.", ReplaceWith("KaAnnotationList"))
public typealias KtAnnotationsList = KaAnnotationList

@Deprecated("Use 'KaAnnotationList' instead.", ReplaceWith("KaAnnotationList"))
public typealias KaAnnotationsList = KaAnnotationList