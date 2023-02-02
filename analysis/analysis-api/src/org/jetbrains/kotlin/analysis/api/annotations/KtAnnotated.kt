/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

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
 * To check if annotation is present, please use [hasAnnotation].
 * [annotationInfos] is more preferable if suits your needs because it is lightweight.
 *
 * @see [KtAnnotationsList.annotations]
 */
public val KtAnnotated.annotations: List<KtAnnotationApplicationWithArgumentsInfo>
    get() = annotationsList.annotations

/**
 * A list of annotation infos.
 *
 * @see [KtAnnotationsList.annotationInfos]
 */
public val KtAnnotated.annotationInfos: List<KtAnnotationApplicationInfo>
    get() = annotationsList.annotationInfos

/**
 * Checks if entity has annotation with specified [classId] and filtered by [useSiteTargetFilter].
 *
 * @see [KtAnnotationsList.hasAnnotation]
 */
public fun KtAnnotated.hasAnnotation(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = annotationsList.hasAnnotation(classId, useSiteTargetFilter)

/**
 * A list of annotations applied with specified [classId] and filtered by [useSiteTargetFilter].
 *
 * @see [KtAnnotationsList.annotationClassIds]
 */
public fun KtAnnotated.annotationsByClassId(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): List<KtAnnotationApplicationWithArgumentsInfo> = annotationsList.annotationsByClassId(classId, useSiteTargetFilter)

/**
 * A list of annotations applied.
 *
 * @see [KtAnnotationsList.annotationClassIds]
 */
public val KtAnnotated.annotationClassIds: Collection<ClassId>
    get() = annotationsList.annotationClassIds
