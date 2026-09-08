/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.KotlinLibrary

interface CInteropModuleDeserializerFactory<D> where D : IrModuleDeserializer, D : CInteropModuleDeserializer {
    fun createIrModuleDeserializer(
        moduleFragment: IrModuleFragment,
        klib: KotlinLibrary,
        linker: KonanIrLinker,
    ): D
}

interface CInteropModuleDeserializer {
    /**
     * Whether there are any declarations that were actually loaded and linked from the
     * C-interop library represented by the current module deserializer.
     */
    fun hasAnyLinkedIrDeclarations(): Boolean
}
