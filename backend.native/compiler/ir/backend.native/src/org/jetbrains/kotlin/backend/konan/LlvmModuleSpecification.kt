/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.library.KonanLibrary

/**
 * Defines what LLVM module should consist of.
 */
interface LlvmModuleSpecification {
    fun importsKotlinDeclarationsFromOtherObjectFiles(): Boolean
    fun containsLibrary(library: KonanLibrary): Boolean
    fun containsModule(module: ModuleDescriptor): Boolean
    fun containsModule(module: IrModuleFragment): Boolean
    fun containsDeclaration(declaration: IrDeclaration): Boolean
}
