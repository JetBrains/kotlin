/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

sealed class ExpectActualAnnotationsIncompatibilityType<out A> {
    abstract val expectAnnotation: A

    class MissingOnActual<out A>(
        override val expectAnnotation: A,
    ) : ExpectActualAnnotationsIncompatibilityType<A>()

    class DifferentOnActual<out A>(
        override val expectAnnotation: A,
        val actualAnnotation: A
    ) : ExpectActualAnnotationsIncompatibilityType<A>()

    fun <A2> mapAnnotationType(mapper: (A) -> A2): ExpectActualAnnotationsIncompatibilityType<A2> {
        return when (this) {
            is MissingOnActual -> MissingOnActual(mapper(expectAnnotation))
            is DifferentOnActual<A> -> DifferentOnActual(mapper(expectAnnotation), mapper(actualAnnotation))
        }
    }
}
