/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.WasmCompilerWithIC
import org.jetbrains.kotlin.backend.wasm.WasmCompilerWithICForTesting
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.createStringBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.jsToKotlinAnyAdapterBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.jsToKotlinStringAdapterBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.registerModuleDescriptorBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.runRootSuitesBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.tryGetAssociatedObjectBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.Functions.unitGetInstanceBuiltIn
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.HeapTypes.anyBuiltInType
import org.jetbrains.kotlin.backend.wasm.ir2wasm.Synthetics.HeapTypes.throwableBuiltInType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.irAttribute
import java.io.File

open class WasmICContext(
    protected val allowIncompleteImplementations: Boolean,
    protected val skipLocalNames: Boolean = false,
    private val skipCommentInstructions: Boolean,
    private val skipLocations: Boolean,
) : PlatformDependentICContext {
    override fun createIrFactory(): IrFactory =
        IrFactoryImplForWasmIC(WholeWorldStageController())

    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration
    ): IrCompilerICInterface =
        WasmCompilerWithIC(
            mainModule = mainModule,
            irBuiltIns = irBuiltIns,
            configuration = configuration,
            allowIncompleteImplementations = allowIncompleteImplementations,
            skipCommentInstructions = skipCommentInstructions,
            skipLocations = skipLocations,
        )

    override fun createSrcFileArtifact(srcFilePath: String, fragments: IrICProgramFragments?, astArtifact: File?): SrcFileArtifact =
        WasmSrcFileArtifactMultimodule(fragments as? WasmIrProgramFragmentsMultimodule, astArtifact, skipLocalNames)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<SrcFileArtifact>,
        artifactsDir: File?,
        forceRebuildJs: Boolean,
        externalModuleName: String?,
    ): ModuleArtifact =
        WasmModuleArtifactMultimodule(fileArtifacts.map { it as WasmSrcFileArtifactMultimodule }, moduleName, externalModuleName)
}

class WasmICContextForTesting(
    allowIncompleteImplementations: Boolean,
    skipLocalNames: Boolean = false,
) : WasmICContext(
    allowIncompleteImplementations,
    skipLocalNames,
    skipCommentInstructions = false,
    skipLocations = false
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration
    ): IrCompilerICInterface =
        WasmCompilerWithICForTesting(mainModule, irBuiltIns, configuration, allowIncompleteImplementations)
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