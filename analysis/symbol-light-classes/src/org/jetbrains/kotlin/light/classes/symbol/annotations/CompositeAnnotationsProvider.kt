/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.name.ClassId

internal class CompositeAnnotationsProvider(val providers: Collection<AnnotationsProvider>) : AnnotationsProvider {
    override fun annotationInfos(): List<AnnotationApplication> = buildList {
        for (provider in providers) {
            addAll(provider.annotationInfos())
        }
    }

    override fun get(classId: ClassId): List<AnnotationApplication> = buildList {
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
