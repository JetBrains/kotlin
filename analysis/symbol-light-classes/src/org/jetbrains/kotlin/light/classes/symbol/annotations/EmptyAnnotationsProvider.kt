/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.name.ClassId

internal object EmptyAnnotationsProvider : AnnotationsProvider {
    override fun annotationInfos(): List<KtAnnotationApplicationInfo> = emptyList()
    override fun get(classId: ClassId): Collection<KtAnnotationApplicationWithArgumentsInfo> = emptyList()
    override fun contains(classId: ClassId): Boolean = false
    override fun ownerClassId(): ClassId? = null
}
