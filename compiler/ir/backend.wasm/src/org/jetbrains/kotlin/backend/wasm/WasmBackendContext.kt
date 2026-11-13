/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.wasm.ir2wasm.JsModuleAndQualifierReference
import org.jetbrains.kotlin.backend.wasm.utils.WasmInlineClassesUtils
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.PropertyLazyInitialization
import org.jetbrains.kotlin.ir.backend.js.ReflectionSymbols
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.propertyLazyInitialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.backend.wasm.ir2wasm.HotReloadPayloads
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.config.wasmUseStackSwitchingProposal
import org.jetbrains.kotlin.wasm.ir.WasmStructDeclaration
import java.util.TreeMap

class WasmBackendContext(
    override val irBuiltIns: IrBuiltIns,
    override val symbolTable: SymbolTable,
    val irModuleFragment: IrModuleFragment,
    override val configuration: CompilerConfiguration,
) : JsCommonBackendContext {
    val phaseConfig = configuration.phaseConfig ?: PhaseConfig()
    val isDebugFriendlyCompilation = configuration.getBoolean(WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION)

    /** Hot-reload codegen (-Xwasm-hot-reload): every reloadable function is
     *  dispatched through a mutable slot global (with an exported setter),
     *  so a host can hot-swap function bodies — including virtual method
     *  bodies for already-existing instances. */
    val isHotReloadCompilation = configuration.getBoolean(WasmConfigurationKeys.WASM_HOT_RELOAD)

    /** Hot-reload on the wasi target shares one exception tag between all
     *  generations (imported from "intrinsics"."tag", anyref-typed, created
     *  once by the host) so that an exception thrown by code swapped into a
     *  slot is CATCHABLE by Kotlin code in any generation — call sites
     *  dispatch through slots across instance boundaries, so handlers must
     *  match throws from any generation's code. (wasm-js already shares its
     *  tag via WebAssembly.JSTag.) */
    val usesAnyrefExceptionTag = isHotReloadCompilation && configuration.wasmTarget != WasmTarget.JS

    /** The previous image's payload structs (class key -> version ->
     * struct), parsed from the injected registry — the image IS the
     * previous-state store (no CLI args, no IC coupling). Populated by
     * [WasmCompilerWithIC.compile]. */
    var hotReloadPayloadHistory: Map<String, TreeMap<Int, WasmStructDeclaration>> = emptyMap()

    /**
     * Resolved payload plans per class (reference key string -> plan),
     * computed deterministically from [hotReloadPayloadHistory] and the
     * class's current fields; shared across files of one compilation so
     * the declaring file's type generation and any file's field-access
     * codegen agree regardless of file order.
     */
    val hotReloadPayloadPlans = mutableMapOf<String, HotReloadPayloads.PayloadPlan>()

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)
    override var inVerbosePhase: Boolean = false
    override val irFactory: IrFactory = symbolTable.irFactory
    val idSignatureRetriever: IdSignatureRetriever = irFactory as IdSignatureRetriever

    val isWasmJsTarget: Boolean = configuration.wasmTarget == WasmTarget.JS

    // Place to store declarations excluded from code generation
    private val excludedDeclarations = mutableMapOf<FqName, IrPackageFragment>()

    fun getExcludedPackageFragmentOrCreate(fqName: FqName, module: IrModuleFragment): IrPackageFragment =
        excludedDeclarations.getOrPut(fqName) {
            IrExternalPackageFragmentImpl(
                DescriptorlessExternalPackageFragmentSymbol(),
                fqName,
                module,
            )
        }

    fun getExcludedPackageFragment(fqName: FqName): IrPackageFragment? = excludedDeclarations.get(fqName)

    class CrossFileContext {
        var mainFunctionWrapper: IrSimpleFunction? = null
        val closureCallTrampolines = mutableMapOf<String, IrSimpleFunction>()
        val kotlinClosureToJsConverters = mutableMapOf<Int, IrSimpleFunction>()
        val jsClosureCallers = mutableMapOf<String, IrSimpleFunction>()
        val jsToKotlinClosures = mutableMapOf<String, IrSimpleFunction>()
        val jsModuleAndQualifierReferences = mutableSetOf<JsModuleAndQualifierReference>()

        // Map<Class, List<Pair<AnnotationClass, AnnotationObject>>>
        val classAssociatedObjects: MutableMap<IrClass, MutableList<Pair<IrClass, IrClass>>> = mutableMapOf()

        var testFunctionDeclarator: IrSimpleFunction? = null

        var objectInstanceFieldInitializer: IrSimpleFunction? = null
        var nonConstantFieldInitializer: IrSimpleFunction? = null

        // Map from STATIC_FUNCTION_REFERENCE fields to their initializer expressions. Used by DCE
        // to visit initializers when the field is accessed
        val staticFunctionReferenceInitializers = mutableMapOf<IrField, IrExpression>()
    }

    val fileContexts = mutableMapOf<IrFile, CrossFileContext>()
    fun getFileContext(irFile: IrFile): CrossFileContext = fileContexts.getOrPut(irFile, ::CrossFileContext)
    inline fun applyIfDefined(irFile: IrFile, body: (CrossFileContext) -> Unit) = fileContexts[irFile]?.apply(body)

    // Functions that are reachable from JavaScript (e.g., closure call trampolines passed as funcref).
    // They receive a WasmFunctionAnnotation.JsCalled annotation so Binaryen does not treat them as unreachable.
    val jsCalledFunctions = mutableSetOf<IrFunctionSymbol>()

    override val jsPromiseSymbol: IrClassSymbol?
        get() = if (configuration.wasmTarget == WasmTarget.JS) wasmSymbols.jsRelatedSymbols.jsPromise else null

    override val innerClassesSupport: InnerClassesSupport = JsInnerClassesSupport(irFactory)

    override val internalPackageFqn = FqName("kotlin.wasm")

    val wasmSymbols: BackendWasmSymbols = BackendWasmSymbols(irBuiltIns, configuration)
    override val symbols = wasmSymbols
    override val sharedVariablesManager = KlibSharedVariablesManager(wasmSymbols)
    override val reflectionSymbols: ReflectionSymbols get() = wasmSymbols.reflectionSymbols

    override val propertyLazyInitialization: PropertyLazyInitialization =
        PropertyLazyInitialization(
            enabled = configuration.propertyLazyInitialization,
            eagerInitialization = wasmSymbols.eagerInitialization
        )

    override val shouldGenerateHandlerParameterForDefaultBodyFun: Boolean
        get() = true

    override val inlineClassesUtils = WasmInlineClassesUtils(wasmSymbols)

    //
    // Unit test support, mostly borrowed from the JS implementation
    //

    override val diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        configuration.diagnosticsCollector,
        configuration.languageVersionSettings,
    )

    override val partialLinkageSupport = createPartialLinkageSupportForLowerings(diagnosticReporter)

    override val externalPackageFragment = mutableMapOf<IrFileSymbol, IrFile>()

    override val additionalExportedDeclarations = hashSetOf<IrDeclaration>()

    override val bodilessBuiltInsPackageFragment: IrPackageFragment by lazy {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            FqName("kotlin"),
            module = irBuiltIns.moduleFragment,
        )
    }

    companion object {
        internal const val SPECIAL_INTERFACE_TABLE_SIZE = 22
        private fun getSpecialITableTypes(context: WasmBackendContext) = listOf(
            context.irBuiltIns.collectionClass,
            context.irBuiltIns.setClass,
            context.irBuiltIns.listClass,
            context.irBuiltIns.mapClass,
            context.irBuiltIns.mapEntryClass,
            context.irBuiltIns.iterableClass,
            context.irBuiltIns.iteratorClass,
            context.irBuiltIns.listIteratorClass,
            context.irBuiltIns.mutableCollectionClass,
            context.irBuiltIns.mutableSetClass,
            context.irBuiltIns.mutableListClass,
            context.irBuiltIns.mutableMapClass,
            context.irBuiltIns.mutableMapEntryClass,
            context.irBuiltIns.mutableIterableClass,
            context.irBuiltIns.mutableIteratorClass,
            context.irBuiltIns.mutableListIteratorClass,
            context.irBuiltIns.comparableClass,
            context.irBuiltIns.charSequenceClass,
            context.wasmSymbols.enumEntries,
            context.wasmSymbols.continuationClass,
            context.wasmSymbols.sequence!!,
            context.wasmSymbols.appendable,
            //FUNCTION_INTERFACE_CLASS
        )
    }

    internal val specialSlotITableTypes by lazy {
        getSpecialITableTypes(this).also {
            if (it.size != SPECIAL_INTERFACE_TABLE_SIZE) {
                compilationException("Invalid special size count", null)
            }
        }
    }

    val wasmUseStackSwitching = configuration.wasmUseStackSwitchingProposal

    override var effectAnalysisFinished: Boolean = false
}
