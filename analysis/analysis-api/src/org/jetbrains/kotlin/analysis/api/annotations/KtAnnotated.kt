/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId

/**
 * Entity which may have annotations applied inside. E.g, type or declaration
 */
public interface KtAnnotated {
    public val annotationsList: KtAnnotationsList
}

/**
 * A list of annotations applied.
 *
 * @see [KtAnnotationsList.annotations]
 */
public val KtAnnotated.annotations: List<KtAnnotationApplication>
    get() = annotationsList.annotations

/**
 * Checks if entity has annotation with specified [classId].
 *
 * @see [KtAnnotationsList.hasAnnotation]
 */
public fun KtAnnotated.hasAnnotation(classId: ClassId): Boolean = annotationsList.hasAnnotation(classId)

/**
 * Checks if entity has annotation with specified [classId] and [useSiteTarget].
 *
 * @see [KtAnnotationsList.hasAnnotation]
 */
public fun KtAnnotated.hasAnnotation(
    classId: ClassId,
    useSiteTarget: AnnotationUseSiteTarget?,
    acceptAnnotationsWithoutUseSite: Boolean = false,
): Boolean = annotationsList.hasAnnotation(classId, useSiteTarget, acceptAnnotationsWithoutUseSite)

/**
 * A list of annotations applied with specified [classId].
 *
 * @see [KtAnnotationsList.annotationClassIds]
 */
public fun KtAnnotated.annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> =
    annotationsList.annotationsByClassId(classId)

/**
 * A list of annotations applied.
 *
 * @see [KtAnnotationsList.annotationClassIds]
 */
public val KtAnnotated.annotationClassIds: Collection<ClassId>
    get() = annotationsList.annotationClassIds