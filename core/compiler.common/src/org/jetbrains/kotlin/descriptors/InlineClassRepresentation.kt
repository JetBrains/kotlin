/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class InlineClassRepresentation<Type : SimpleTypeMarker>(
    val underlyingPropertyName: Name,
    val underlyingType: Type,
) {
    inline fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): InlineClassRepresentation<Other> =
        InlineClassRepresentation(underlyingPropertyName, transform(underlyingType))
}
