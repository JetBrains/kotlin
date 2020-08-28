/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsMapping
import org.jetbrains.kotlin.ir.backend.js.JsSharedVariablesManager
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName

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
    override val transformedFunction = mutableMapOf<IrFunctionSymbol, IrSimpleFunctionSymbol>()
    override val lateinitNullableFields = mutableMapOf<IrField, IrField>()
    override val extractedLocalClasses: MutableSet<IrClass> = hashSetOf()
    override val irFactory: IrFactory = PersistentIrFactory

    // Place to store declarations excluded from code generation
    val excludedDeclarations: IrPackageFragment by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            FqName("kotlin")
        )
    }

    override val mapping = JsMapping()

    val innerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

    val objectToGetInstanceFunction = mutableMapOf<IrClassSymbol, IrSimpleFunction>()
    override val internalPackageFqn = FqName("kotlin.wasm")

    private val internalPackageFragment = IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
        builtIns.builtInsModule, FqName("kotlin.wasm.internal")
    )

    override val sharedVariablesManager = JsSharedVariablesManager(TODO("..."))

    val wasmSymbols: WasmSymbols = WasmSymbols(this@WasmBackendContext, symbolTable)
    override val ir = object : Ir<WasmBackendContext>(this, irModuleFragment) {
        override val symbols: Symbols<WasmBackendContext> = wasmSymbols
        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    override fun log(message: () -> String) {
        /*TODO*/
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}
