/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.FqName

fun NameResolver.getClassId(index: Int): ClassId {
    if (this is NameResolverImpl) {
        val identifiers = traverseIds(index)
        return ClassId(FqName.fromSegments(identifiers.first), FqName.fromSegments(identifiers.second), identifiers.third)
    } else {
        return ClassId.fromString(getQualifiedClassName(index), isLocalClassName(index))
    }
}

fun NameResolver.getName(index: Int): Name =
    Name.guessByFirstCharacter(getString(index))
