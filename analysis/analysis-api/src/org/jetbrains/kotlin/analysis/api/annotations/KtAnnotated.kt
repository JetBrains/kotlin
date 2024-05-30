/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.name.ClassId

/**
 * Entity which may have annotations applied inside. E.g, type or declaration
 */
public interface KaAnnotated {
    public val annotationsList: KaAnnotationList
}

public typealias KtAnnotated = KaAnnotated

/**
 * A list of annotations applied.
 *
 * To check if annotation is present, please use [hasAnnotation].
 *
 * @see [KaAnnotationList.annotations]
 */
public val KaAnnotated.annotations: List<KaAnnotationApplication>
    get() = annotationsList

@Deprecated("Use 'annotations' instead.", replaceWith = ReplaceWith("annotations"))
public val KaAnnotated.annotationInfos: List<KaAnnotationApplicationInfo>
    get() = annotationsList

/**
 * Checks if entity has annotation with specified [classId] and filtered by [useSiteTargetFilter].
 *
 * @see [KaAnnotationList.hasAnnotation]
 */
public fun KaAnnotated.hasAnnotation(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = annotationsList.hasAnnotation(classId, useSiteTargetFilter)

/**
 * A list of annotations applied with specified [classId] and filtered by [useSiteTargetFilter].
 *
 * @see [KaAnnotationList.annotationClassIds]
 */
public fun KaAnnotated.annotationsByClassId(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): List<KaAnnotationApplication> = annotationsList.annotationsByClassId(classId, useSiteTargetFilter)

/**
 * A list of annotations applied.
 *
 * @see [KaAnnotationList.annotationClassIds]
 */
public val KaAnnotated.annotationClassIds: Collection<ClassId>
    get() = annotationsList.annotationClassIds
