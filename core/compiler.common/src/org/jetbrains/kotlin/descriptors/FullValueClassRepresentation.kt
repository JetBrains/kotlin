/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.RigidTypeMarker

class FullValueClassRepresentation<Type : RigidTypeMarker>(
    override val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>?
) : ValueClassRepresentation<Type>() {
    private val map = underlyingPropertyNamesToTypes?.toMap()

    override fun containsPropertyWithName(name: Name): Boolean = map != null && name in map
    override fun getPropertyTypeByName(name: Name): Type? = map?.get(name)

    override fun toString(): String =
        "FullValueClassRepresentation(underlyingPropertyNamesToTypes=$underlyingPropertyNamesToTypes)"
}
