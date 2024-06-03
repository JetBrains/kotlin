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
    public val annotations: KaAnnotationList

    @Deprecated("Use 'annotations' instead.", replaceWith = ReplaceWith("annotations"))
    public val annotationsList: KaAnnotationList
        get() = annotations
}

public typealias KtAnnotated = KaAnnotated

/**
 * A list of annotations applied.
 *
 * To check if annotation is present, please use [hasAnnotation].
 *
 * @see [KaAnnotationList.annotations]
 */
@Deprecated("Use the 'annotations' the member property instead.")
public val KaAnnotated.annotations: List<KaAnnotation>
    get() = annotations

@Deprecated("Use 'annotations' instead.", replaceWith = ReplaceWith("annotations"))
public val KaAnnotated.annotationInfos: List<KaAnnotationApplicationInfo>
    get() = annotations

/**
 * Checks if entity has annotation with specified [classId].
 *
 * @see [KaAnnotationList.hasAnnotation]
 */
public fun KaAnnotated.hasAnnotation(classId: ClassId): Boolean {
    return annotations.hasAnnotation(classId)
}

/**
 * A list of annotations applied with specified [classId].
 *
 * @see [KaAnnotationList.classIds]
 */
public fun KaAnnotated.annotationsByClassId(classId: ClassId): List<KaAnnotation> {
    return annotations.annotationsByClassId(classId)
}

/**
 * A list of annotations applied.
 *
 * @see [KaAnnotationList.classIds]
 */
public val KaAnnotated.annotationClassIds: Collection<ClassId>
    get() = annotations.classIds
