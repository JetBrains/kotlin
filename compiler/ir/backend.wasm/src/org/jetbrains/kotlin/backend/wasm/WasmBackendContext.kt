/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.wasm.ir2wasm.JsModuleAndQualifierReference
import org.jetbrains.kotlin.backend.wasm.lower.WasmSharedVariablesManager
import org.jetbrains.kotlin.backend.wasm.utils.WasmInlineClassesUtils
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget

class WasmBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val irModuleFragment: IrModuleFragment,
    propertyLazyInitialization: Boolean,
    override val configuration: CompilerConfiguration,
) : JsCommonBackendContext {
    val phaseConfig = configuration.phaseConfig ?: PhaseConfig()

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)
    override var inVerbosePhase: Boolean = false
    override val irFactory: IrFactory = symbolTable.irFactory

    val isWasmJsTarget: Boolean = configuration.wasmTarget == WasmTarget.JS

    // Place to store declarations excluded from code generation
    private val excludedDeclarations = mutableMapOf<FqName, IrPackageFragment>()

    fun getExcludedPackageFragment(fqName: FqName): IrPackageFragment = excludedDeclarations.getOrPut(fqName) {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            fqName
        )
    }

    override val mapping = JsMapping()

    class CrossFileContext {
        var mainFunctionWrapper: IrSimpleFunction? = null
        val closureCallExports = mutableMapOf<String, IrSimpleFunction>()
        val kotlinClosureToJsConverters = mutableMapOf<String, IrSimpleFunction>()
        val jsClosureCallers = mutableMapOf<String, IrSimpleFunction>()
        val jsToKotlinClosures = mutableMapOf<String, IrSimpleFunction>()
        val jsModuleAndQualifierReferences = mutableSetOf<JsModuleAndQualifierReference>()

        // Map<Class, List<Pair<AnnotationClass, AnnotationObject>>>
        val classAssociatedObjects: MutableMap<IrClass, MutableList<Pair<IrClass, IrClass>>> = mutableMapOf()
    }

    val fileContexts = mutableMapOf<IrFile, CrossFileContext>()
    fun getFileContext(irFile: IrFile): CrossFileContext = fileContexts.getOrPut(irFile, ::CrossFileContext)
    inline fun applyIfDefined(irFile: IrFile, body: (CrossFileContext) -> Unit) = fileContexts[irFile]?.apply(body)

    //TODO Move to CrossFileContext
    override val testFunsPerFile = hashMapOf<IrFile, IrSimpleFunction>()

    override val jsPromiseSymbol: IrClassSymbol?
        get() = if (configuration.wasmTarget == WasmTarget.JS) wasmSymbols.jsRelatedSymbols.jsPromise else null

    override val innerClassesSupport: InnerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

    override val internalPackageFqn = FqName("kotlin.wasm")

    override val sharedVariablesManager = WasmSharedVariablesManager(this)

    val wasmSymbols: WasmSymbols = WasmSymbols(irBuiltIns, configuration)
    override val symbols = wasmSymbols
    override val reflectionSymbols: ReflectionSymbols get() = wasmSymbols.reflectionSymbols

    override val enumEntries = wasmSymbols.enumEntries
    override val createEnumEntries = wasmSymbols.createEnumEntries

    override val propertyLazyInitialization: PropertyLazyInitialization =
        PropertyLazyInitialization(enabled = propertyLazyInitialization, eagerInitialization = wasmSymbols.eagerInitialization)

    override val ir = object : Ir() {
        override val symbols: WasmSymbols = wasmSymbols
        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    override val inlineClassesUtils = WasmInlineClassesUtils(wasmSymbols)

    //
    // Unit test support, mostly borrowed from the JS implementation
    //

    override val suiteFun: IrSimpleFunctionSymbol?
        get() = wasmSymbols.suiteFun
    override val testFun: IrSimpleFunctionSymbol?
        get() = wasmSymbols.testFun

    override val partialLinkageSupport = createPartialLinkageSupportForLowerings(
        configuration.partialLinkageConfig,
        irBuiltIns,
        configuration.messageCollector
    )

    override val externalPackageFragment = mutableMapOf<IrFileSymbol, IrFile>()

    override val additionalExportedDeclarations = hashSetOf<IrDeclaration>()

    override val bodilessBuiltInsPackageFragment: IrPackageFragment = IrExternalPackageFragmentImpl(
        DescriptorlessExternalPackageFragmentSymbol(),
        FqName("kotlin")
    )

}
