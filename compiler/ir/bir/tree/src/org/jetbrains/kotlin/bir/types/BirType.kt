/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.declarations.BirAnnotationContainer
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

abstract class BirType : KotlinTypeMarker, BirAnnotationContainer {

    /**
     * @return true if this type is equal to [other] symbolically. Note that this is NOT EQUIVALENT to the full type checking algorithm
     * used in the compiler frontend. For example, this method will return `false` on the types `List<*>` and `List<Any?>`,
     * whereas the real type checker from the compiler frontend would return `true`.
     *
     * Classes are compared by FQ names, which means that even if two types refer to different symbols of the class with the same FQ name,
     * such types will be considered equal. Type annotations do not have any effect on the behavior of this method.
     */
    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}
