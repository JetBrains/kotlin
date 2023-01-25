/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.name.ClassId

internal class CompositeAnnotationsProvider(val providers: Collection<AnnotationsProvider>) : AnnotationsProvider {
    override fun classIds(): Collection<ClassId> = buildList {
        for (provider in providers) {
            addAll(provider.classIds())
        }
    }

    override fun get(classId: ClassId): Collection<KtAnnotationApplication> = buildList {
        for (provider in providers) {
            addAll(provider[classId])
        }
    }

    override fun contains(classId: ClassId): Boolean = providers.any { classId in it }

    override fun isTheSameAs(other: Any?): Boolean = other === this ||
            other is CompositeAnnotationsProvider &&
            other.providers.size == providers.size &&
            other.providers.zip(providers).all { (another, myProvider) ->
                myProvider.isTheSameAs(another)
            }

    override fun ownerClassId(): ClassId? = null
}
