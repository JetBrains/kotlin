/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.name.ClassId

fun NameResolver.getClassId(index: Int): ClassId {
    return ClassId.fromString(getQualifiedClassName(index), isLocalClassName(index))
}
