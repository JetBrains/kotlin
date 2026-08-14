/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmCompilerWithICMultimodule
import org.jetbrains.kotlin.backend.wasm.WasmCompilerWithICSingleModule
import org.jetbrains.kotlin.backend.wasm.WasmCompilerWithICWholeWorld
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.createStringBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.jsToKotlinAnyAdapterBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.jsToKotlinStringAdapterBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.registerModuleDescriptorBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.runRootSuitesBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.tryGetAssociatedObjectBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.unitGetInstanceBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.HeapTypes.anyBuiltInType
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.HeapTypes.throwableBuiltInType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.PlatformDependentICContext
import org.jetbrains.kotlin.ir.backend.js.ic.SrcFileArtifact
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File

abstract class WasmICContextBase<TModuleArtifact, TFileArtifact, TFragments> :
    PlatformDependentICContext<TModuleArtifact, TFileArtifact, TFragments, WasmBackendContext>
        where TModuleArtifact : ModuleArtifact,
              TFileArtifact : SrcFileArtifact,
              TFragments : IrICProgramFragments {

    override fun getICCacheStableKeys(): Set<CompilerConfigurationKey<*>> =
        setOf(
            JSConfigurationKeys.LIBRARIES,
            JSConfigurationKeys.FRIEND_LIBRARIES,
            JSConfigurationKeys.INCLUDES,
            WasmConfigurationKeys.WASM_IC_GENERATE_UNCHANGED_MODULES
        )

    override fun createBackendContext(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        configuration: CompilerConfiguration,
    ): WasmBackendContext {
        // Hack (KT-71039, restored after KT-78040) - pre-load functional interfaces in case if IrLoader cut its count
        // `WasmAddFunctionSupertypeToSuspendFunctionLowering` of Kotlin/Wasm backend
        // adds `Function<...>` supertypes to `SuspendFunction<...>` interfaces.
        // Since these supertypes appear only at the lowering stage,
        // after klib deserialization, the linker / Incremental
        // Compilation never sees the `FunctionN`/`KFunctionN` builtins as used
        // and therefore never loads or serializes them.
        // Referencing `functionN(it)`/`kFunctionN(it)` here forces their definitions
        // to be materialized up front.
        repeat(25) {
            irBuiltIns.functionN(it)
            irBuiltIns.kFunctionN(it)
        }

        return WasmBackendContext(
            irBuiltIns,
            symbolTable,
            mainModule,
            configuration = configuration,
        )
    }
}

open class WasmICContextMultimodule(
    protected val allowIncompleteImplementations: Boolean,
    protected val skipLocalNames: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) : WasmICContextBase<WasmModuleArtifactMultimodule, WasmSrcFileArtifactMultimodule, WasmIrProgramFragmentsMultimodule>() {
    override fun createIrFactory(): IrFactory =
        IrFactoryImplForWasmIC(WholeWorldStageController())

    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: WasmBackendContext,
    ): WasmCompilerWithICMultimodule =
        WasmCompilerWithICMultimodule(
            mainModule = mainModule,
            allowIncompleteImplementations = allowIncompleteImplementations,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
            context = context,
        )

    override fun createSrcFileArtifact(
        srcFilePath: String,
        fragments: WasmIrProgramFragmentsMultimodule?,
        astArtifact: File?,
    ): WasmSrcFileArtifactMultimodule =
        WasmSrcFileArtifactMultimodule(fragments, astArtifact, skipLocalNames)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<WasmSrcFileArtifactMultimodule>,
        artifactsDir: File?,
        forceRebuild: Boolean,
        externalModuleName: String?,
    ): WasmModuleArtifactMultimodule =
        WasmModuleArtifactMultimodule(
            fileArtifacts = fileArtifacts,
            moduleName = moduleName,
            externalModuleName = externalModuleName,
            forceRebuildWasm = forceRebuild
        )
}


open class WasmICContextSingleModule(
    protected val allowIncompleteImplementations: Boolean,
    protected val skipLocalNames: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) : WasmICContextBase<WasmModuleArtifactSingleModule, WasmSrcFileArtifactSingleModule, WasmIrProgramFragmentsSingleModule>() {
    override fun createIrFactory(): IrFactory =
        IrFactoryImplForWasmIC(WholeWorldStageController())

    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: WasmBackendContext,
    ): WasmCompilerWithICSingleModule =
        WasmCompilerWithICSingleModule(
            mainModule = mainModule,
            allowIncompleteImplementations = allowIncompleteImplementations,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
            context = context,
        )

    override fun createSrcFileArtifact(
        srcFilePath: String,
        fragments: WasmIrProgramFragmentsSingleModule?,
        astArtifact: File?,
    ): WasmSrcFileArtifactSingleModule =
        WasmSrcFileArtifactSingleModule(fragments, astArtifact, skipLocalNames)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<WasmSrcFileArtifactSingleModule>,
        artifactsDir: File?,
        forceRebuild: Boolean,
        externalModuleName: String?,
    ): WasmModuleArtifactSingleModule =
        WasmModuleArtifactSingleModule(fileArtifacts, moduleName, externalModuleName)
}

open class WasmICContextWholeWorld(
    protected val allowIncompleteImplementations: Boolean,
    protected val skipLocalNames: Boolean,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) : WasmICContextBase<WasmModuleArtifact, WasmSrcFileArtifact, WasmIrProgramFragments>() {
    override fun createIrFactory(): IrFactory =
        IrFactoryImplForWasmIC(WholeWorldStageController())

    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: WasmBackendContext,
    ): WasmCompilerWithICWholeWorld =
        WasmCompilerWithICWholeWorld(
            mainModule = mainModule,
            allowIncompleteImplementations = allowIncompleteImplementations,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
            context = context,
        )

    override fun createSrcFileArtifact(srcFilePath: String, fragments: WasmIrProgramFragments?, astArtifact: File?): WasmSrcFileArtifact =
        WasmSrcFileArtifact(fragments, astArtifact, skipLocalNames)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<WasmSrcFileArtifact>,
        artifactsDir: File?,
        forceRebuild: Boolean,
        externalModuleName: String?,
    ): WasmModuleArtifact = WasmModuleArtifact(fileArtifacts)
}

class IrFactoryImplForWasmIC(stageController: StageController) : IrFactory(stageController), IdSignatureRetriever {
    override fun <T : IrDeclaration> T.declarationCreated(): T {
        val parentSig = stageController.currentDeclaration?.let { declarationSignature(it) } ?: return this

        stageController.createSignature(parentSig)?.let { this.signatureForWasmIC = it }

        return this
    }

    private fun eraseSignature(signature: IdSignature, declaration: IrDeclaration): IdSignature {
        val file = declaration.fileOrNull ?: return signature
        return fileSignatureErasure(signature, file.module.name.asString())
    }

    override fun declarationSignature(declaration: IrDeclaration): IdSignature =
        declaration.signatureForWasmIC
            ?: declaration.symbol.signature?.let { eraseSignature(it, declaration) }
            ?: declaration.symbol.privateSignature?.let { eraseSignature(it, declaration) }
            ?: compilationException("Can't retrieve a signature", declaration)
}

private var IrDeclaration.signatureForWasmIC: IdSignature? by irAttribute(copyByDefault = false)

fun overrideBuiltInsSignatures(backendContext: WasmBackendContext) {
    with(backendContext.wasmSymbols) {
        tryGetAssociatedObject.owner.signatureForWasmIC = tryGetAssociatedObjectBuiltIn.value
        if (backendContext.isWasmJsTarget) {
            jsRelatedSymbols.jsInteropAdapters.jsToKotlinAnyAdapter.owner.signatureForWasmIC = jsToKotlinAnyAdapterBuiltIn.value
            jsRelatedSymbols.jsInteropAdapters.jsToKotlinStringAdapter.owner.signatureForWasmIC = jsToKotlinStringAdapterBuiltIn.value
        }
        runRootSuites?.owner?.signatureForWasmIC = runRootSuitesBuiltIn.value
        createString.owner.signatureForWasmIC = createStringBuiltIn.value
        registerModuleDescriptor.owner.signatureForWasmIC = registerModuleDescriptorBuiltIn.value
    }

    backendContext.findUnitGetInstanceFunction().signatureForWasmIC = unitGetInstanceBuiltIn.value

    with(backendContext.irBuiltIns) {
        throwableClass.owner.signatureForWasmIC = throwableBuiltInType.type
        anyClass.owner.signatureForWasmIC = anyBuiltInType.type
    }
}
