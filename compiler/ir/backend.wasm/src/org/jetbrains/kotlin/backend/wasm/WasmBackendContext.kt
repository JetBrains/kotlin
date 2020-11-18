/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.wasm.lower.WasmSharedVariablesManager
import org.jetbrains.kotlin.backend.wasm.utils.WasmInlineClassesUtils
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsMapping
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class WasmBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    @Suppress("UNUSED_PARAMETER") symbolTable: SymbolTable,
    @Suppress("UNUSED_PARAMETER") irModuleFragment: IrModuleFragment,
    val additionalExportedDeclarations: Set<FqName>,
    override val configuration: CompilerConfiguration
) : JsCommonBackendContext {
    override val builtIns = module.builtIns
    override var inVerbosePhase: Boolean = false
    override val scriptMode = false
    override val extractedLocalClasses: MutableSet<IrClass> = hashSetOf()
    override val irFactory: IrFactory = PersistentIrFactory

    // Place to store declarations excluded from code generation
    private val excludedDeclarations = mutableMapOf<FqName, IrPackageFragment>()

    fun getExcludedPackageFragment(fqName: FqName): IrPackageFragment = excludedDeclarations.getOrPut(fqName) {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            fqName
        )
    }

    override val mapping = JsMapping()

    val innerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

    override val internalPackageFqn = FqName("kotlin.wasm")

    private val internalPackageFragmentDescriptor = EmptyPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.wasm.internal"))
    // TODO: Merge with JS IR Backend context lazy file
    val internalPackageFragment by lazy {
        IrFileImpl(object : SourceManager.FileEntry {
            override val name = "<implicitDeclarations>"
            override val maxOffset = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                SourceRangeInfo(
                    "",
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET
                )

            override fun getLineNumber(offset: Int) = UNDEFINED_OFFSET
            override fun getColumnNumber(offset: Int) = UNDEFINED_OFFSET
        }, internalPackageFragmentDescriptor).also {
            irModuleFragment.files += it
        }
    }


    val startFunction = irFactory.buildFun {
        name = Name.identifier("startFunction")
        returnType = irBuiltIns.unitType
    }.apply {
        body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        internalPackageFragment.addChild(this)
    }

    override val sharedVariablesManager =
        WasmSharedVariablesManager(this, irBuiltIns, internalPackageFragment)

    val wasmSymbols: WasmSymbols = WasmSymbols(this@WasmBackendContext, symbolTable)
    override val ir = object : Ir<WasmBackendContext>(this, irModuleFragment) {
        override val symbols: Symbols<WasmBackendContext> = wasmSymbols
        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    override val inlineClassesUtils = WasmInlineClassesUtils(wasmSymbols)

    override fun log(message: () -> String) {
        /*TODO*/
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}
