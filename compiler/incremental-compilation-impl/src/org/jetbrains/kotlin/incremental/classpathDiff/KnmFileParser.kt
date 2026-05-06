/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.getClassId

internal fun parseKnmClassId(bytes: ByteArray): ClassId {
    val proto = parsePackageFragment(bytes)
    val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
    val classes = proto.class_List
    check(classes.size == 1) {
        "[OSIP-75 prototype] Expected exactly 1 class in .knm file, but found ${classes.size}"
    }
    return nameResolver.getClassId(classes.single().fqName)
}
