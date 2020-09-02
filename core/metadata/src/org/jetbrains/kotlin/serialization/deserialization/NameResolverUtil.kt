/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

fun NameResolver.getClassId(index: Int): ClassId {
    return ClassId.fromString(getQualifiedClassName(index), isLocalClassName(index))
}

fun NameResolver.getName(index: Int): Name =
    Name.guessByFirstCharacter(getString(index))
