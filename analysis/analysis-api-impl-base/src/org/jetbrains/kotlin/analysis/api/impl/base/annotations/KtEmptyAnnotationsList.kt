/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import java.util.Collections

class KaEmptyAnnotationList(override val token: KaLifetimeToken) : AbstractList<KaAnnotation>(), KaAnnotationList {
    override val size: Int
        get() = withValidityAssertion { 0 }

    override fun iterator(): Iterator<KaAnnotation> = withValidityAssertion {
        return Collections.emptyIterator()
    }

    override fun get(index: Int): KaAnnotation = withValidityAssertion {
        throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        return false
    }

    override fun get(classId: ClassId): List<KaAnnotation> = withValidityAssertion {
        return emptyList()
    }

    override val classIds: Set<ClassId>
        get() = withValidityAssertion { emptySet() }
}
