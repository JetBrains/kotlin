/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import java.util.Collections

class KaEmptyAnnotationList(override val token: KaLifetimeToken) : AbstractList<KaAnnotationApplication>(), KaAnnotationList {
    override val size: Int
        get() = withValidityAssertion { 0 }

    override fun iterator(): Iterator<KaAnnotationApplication> = withValidityAssertion {
        return Collections.emptyIterator()
    }

    override fun get(index: Int): KaAnnotationApplication = withValidityAssertion {
        throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        return false
    }

    override fun annotationsByClassId(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): List<KaAnnotationApplication> {
        withValidityAssertion {
            return emptyList()
        }
    }

    override val annotationClassIds: Set<ClassId>
        get() = withValidityAssertion { emptySet() }
}
