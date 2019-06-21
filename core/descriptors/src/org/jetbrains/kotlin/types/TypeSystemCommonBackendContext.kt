/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext

interface TypeSystemCommonBackendContext : TypeSystemContext {
    fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean

    fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean

    /**
     * @return value of the first argument of the annotation with the given [fqName], if the annotation is present and
     * the argument is of a primitive type or a String, or null otherwise.
     *
     * Note that this method returns null if no arguments are provided, even if the corresponding annotation parameter has a default value.
     *
     * TODO: provide a more granular & elaborate API here to reduce confusion
     */
    fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any?
}
