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
    public val annotationsList: KaAnnotationsList
}

public typealias KtAnnotated = KaAnnotated

/**
 * A list of annotations applied.
 *
 * To check if annotation is present, please use [hasAnnotation].
 * [annotationInfos] is more preferable if suits your needs because it is lightweight.
 *
 * @see [KaAnnotationsList.annotations]
 */
public val KaAnnotated.annotations: List<KaAnnotationApplicationWithArgumentsInfo>
    get() = annotationsList.annotations

/**
 * A list of annotation infos.
 *
 * @see [KaAnnotationsList.annotationInfos]
 */
public val KaAnnotated.annotationInfos: List<KaAnnotationApplicationInfo>
    get() = annotationsList.annotationInfos

/**
 * Checks if entity has annotation with specified [classId] and filtered by [useSiteTargetFilter].
 *
 * @see [KaAnnotationsList.hasAnnotation]
 */
public fun KaAnnotated.hasAnnotation(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = annotationsList.hasAnnotation(classId, useSiteTargetFilter)

/**
 * A list of annotations applied with specified [classId] and filtered by [useSiteTargetFilter].
 *
 * @see [KaAnnotationsList.annotationClassIds]
 */
public fun KaAnnotated.annotationsByClassId(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): List<KaAnnotationApplicationWithArgumentsInfo> = annotationsList.annotationsByClassId(classId, useSiteTargetFilter)

/**
 * A list of annotations applied.
 *
 * @see [KaAnnotationsList.annotationClassIds]
 */
public val KaAnnotated.annotationClassIds: Collection<ClassId>
    get() = annotationsList.annotationClassIds
