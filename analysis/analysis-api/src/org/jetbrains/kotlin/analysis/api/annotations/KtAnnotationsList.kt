/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.name.ClassId

public abstract class KtAnnotationsList : ValidityTokenOwner {
    public abstract val annotations: List<KtAnnotationApplication>
    public abstract fun containsAnnotation(classId: ClassId): Boolean
    public abstract fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication>
    public abstract val annotationClassIds: Collection<ClassId>
}

