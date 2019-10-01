/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.library.KotlinLibrary

// TODO: consider making two implementations: for producing cache and anything else.
internal class LlvmModuleSpecificationImpl(
        private val cachedLibraries: CachedLibraries,
        private val producingCache: Boolean,
        private val librariesToCache: Set<KotlinLibrary>
) : LlvmModuleSpecification {
    override val isFinal: Boolean
        get() = !producingCache

    override fun importsKotlinDeclarationsFromOtherObjectFiles(): Boolean =
            cachedLibraries.hasStaticCaches // A bit conservative but still valid.

    override fun importsKotlinDeclarationsFromOtherSharedLibraries(): Boolean =
            cachedLibraries.hasDynamicCaches // A bit conservative but still valid.

    override fun containsLibrary(library: KotlinLibrary): Boolean = if (producingCache) {
        library in librariesToCache
    } else {
        !cachedLibraries.isLibraryCached(library)
    }

    override fun containsModule(module: IrModuleFragment): Boolean =
            containsModule(module.descriptor)

    override fun containsModule(module: ModuleDescriptor): Boolean =
            module.konanLibrary.let { it == null || containsLibrary(it) }

    override fun containsDeclaration(declaration: IrDeclaration): Boolean =
            declaration.module.konanLibrary.let { it == null || containsLibrary(it) }
}
