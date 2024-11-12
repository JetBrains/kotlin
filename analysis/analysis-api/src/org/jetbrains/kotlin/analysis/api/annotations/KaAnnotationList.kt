/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
}
