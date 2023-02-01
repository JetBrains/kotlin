/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.name.ClassId

internal class CompositeAnnotationsProvider(val providers: Collection<AnnotationsProvider>) : AnnotationsProvider {
    constructor(vararg providers: AnnotationsProvider) : this(providers.toList())

    override fun annotationInfos(): List<KtAnnotationApplicationInfo> = buildList {
        for (provider in providers) {
            addAll(provider.annotationInfos())
        }
    }

    override fun get(classId: ClassId): Collection<KtAnnotationApplicationWithArgumentsInfo> = buildList {
        for (provider in providers) {
            addAll(provider[classId])
        }
    }

    override fun contains(classId: ClassId): Boolean = providers.any { classId in it }

    override fun equals(other: Any?): Boolean = other === this ||
            other is CompositeAnnotationsProvider &&
            other.providers == providers

    override fun hashCode(): Int = providers.hashCode()

    override fun ownerClassId(): ClassId? = null
}
