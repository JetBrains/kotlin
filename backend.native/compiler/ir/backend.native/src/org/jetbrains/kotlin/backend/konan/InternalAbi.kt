/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.llvm.llvmSymbolOrigin
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Sometimes we need to reference symbols that are not declared in metadata.
 * For example, symbol might be declared during lowering.
 * In case of compiler caches, this means that it is not accessible as Lazy IR
 * and we have to explicitly add an external declaration.
 */
internal class InternalAbi(private val context: Context) {
    /**
     * File that stores all internal ABI declarations.
     *
     * We have to store such declarations in top-level to avoid mangling that
     * makes referencing harder.
     * A bit better solution is to add files with proper packages, but it is impossible
     * during FileLowering (hello, ConcurrentModificationException).
     */
    private lateinit var internalAbiFile: IrFile

    /**
     * Representation of ABI files from external modules.
     */
    private val externalModules = mutableMapOf<ModuleDescriptor, IrFile>()

    fun init(module: IrModuleFragment) {
        internalAbiFile = createAbiFile(module)
    }

    private fun createAbiFile(module: IrModuleFragment): IrFile =
        module.addFile(NaiveSourceBasedFileEntryImpl("internal"), FqName("kotlin.native.caches.abi"))

    /**
     * Adds external [function] from [module] to a list of external references.
     */
    fun reference(function: IrFunction, module: ModuleDescriptor) {
        assert(function.isExternal) { "Function that represents external ABI should be marked as external" }
        context.llvmImports.add(module.llvmSymbolOrigin)
        externalModules.getOrPut(module) {
            createAbiFile(IrModuleFragmentImpl(module, context.irBuiltIns))
        }.addChild(function)
    }

    /**
     * Generate name for declaration that will be a part of internal ABI.
     */
    fun getMangledNameFor(declarationName: String, parent: IrDeclarationParent): Name {
        val prefix = parent.fqNameForIrSerialization
        return "$prefix.$declarationName".synthesizedName
    }

    /**
     * Adds [function] to a list of publicly available symbols.
     */
    fun declare(function: IrFunction) {
        internalAbiFile.addChild(function)
    }

    companion object {
        /**
         * Allows to distinguish external declarations to internal ABI.
         */
        val INTERNAL_ABI_ORIGIN = object : IrDeclarationOriginImpl("INTERNAL_ABI") {}
    }
}