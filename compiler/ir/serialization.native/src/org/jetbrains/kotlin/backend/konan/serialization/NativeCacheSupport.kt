/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.PartialCacheInfoBase
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName

interface CacheDeserializationStrategyBase {
    fun contains(filePath: String): Boolean
    fun contains(fqName: FqName, fileName: String): Boolean
}

interface NativeCacheSupport{
    val cachedLibraries: CachedLibrariesBase
    val lazyIrForCaches: Boolean
    val libraryBeingCached: PartialCacheInfoBase?

    fun getDescriptorForCachedDeclarationModuleDeserializer(declaration: IrDeclaration): ModuleDescriptor?
    fun getDeserializationStrategy(klib: KotlinLibrary): CacheDeserializationStrategyBase
}
