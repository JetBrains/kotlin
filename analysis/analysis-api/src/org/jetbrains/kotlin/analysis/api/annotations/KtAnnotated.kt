/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.name.ClassId

public interface KtAnnotated {
    public val annotationsList: KtAnnotationsList
}

public val KtAnnotated.annotations: List<KtAnnotationApplication>
    get() = annotationsList.annotations

public fun KtAnnotated.containsAnnotation(classId: ClassId): Boolean =
    annotationsList.containsAnnotation(classId)

public fun KtAnnotated.annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> =
    annotationsList.annotationsByClassId(classId)

public val KtAnnotated.annotationClassIds: Collection<ClassId>
    get() = annotationsList.annotationClassIds