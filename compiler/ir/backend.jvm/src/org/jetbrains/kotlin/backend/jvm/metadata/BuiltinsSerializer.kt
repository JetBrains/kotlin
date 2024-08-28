/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.metadata

import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.name.FqName

interface BuiltinsSerializer {
    fun serialize(filesMetadata: List<MetadataSource.File>): List<Pair<FqName, ByteArray>>
    fun serializeEmptyPackage(fqName: FqName): ByteArray
}
