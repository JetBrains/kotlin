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
import org.jetbrains.kotlin.ir.IrBuiltIns
import java.io.File
import java.util.*

open class WasmICContext(
    protected val allowIncompleteImplementations: Boolean,
    protected val skipLocalNames: Boolean = false,
    private val safeFragmentTags: Boolean,
    private val skipCommentInstructions: Boolean,
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
            safeFragmentTags = safeFragmentTags,
            skipCommentInstructions = skipCommentInstructions
        )

    override fun createSrcFileArtifact(srcFilePath: String, fragments: IrICProgramFragments?, astArtifact: File?): SrcFileArtifact =
        WasmSrcFileArtifact(fragments as? WasmIrProgramFragments, astArtifact, skipLocalNames)

    override fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<SrcFileArtifact>,
        artifactsDir: File?,
        forceRebuildJs: Boolean,
        externalModuleName: String?,
    ): ModuleArtifact =
        WasmModuleArtifact(fileArtifacts.map { it as WasmSrcFileArtifact })
}

class WasmICContextForTesting(
    allowIncompleteImplementations: Boolean,
    skipLocalNames: Boolean = false,
    safeFragmentTags: Boolean = false,
) : WasmICContext(allowIncompleteImplementations, skipLocalNames, safeFragmentTags, skipCommentInstructions = false) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration
    ): IrCompilerICInterface =
        WasmCompilerWithICForTesting(mainModule, irBuiltIns, configuration, allowIncompleteImplementations)
}

class IrFactoryImplForWasmIC(stageController: StageController) : IrFactory(stageController), IdSignatureRetriever {
    private val declarationToSignature = WeakHashMap<IrDeclaration, IdSignature>()

    override fun <T : IrDeclaration> T.declarationCreated(): T {
        val parentSig = stageController.currentDeclaration?.let { declarationSignature(it) } ?: return this

        stageController.createSignature(parentSig)?.let { declarationToSignature[this] = it }

        return this
    }

    private fun eraseSignature(signature: IdSignature, declaration: IrDeclaration): IdSignature {
        val file = declaration.fileOrNull ?: return signature
        return fileSignatureErasure(signature, file.module.name.asString())
    }

    override fun declarationSignature(declaration: IrDeclaration): IdSignature =
        declarationToSignature[declaration]
            ?: declaration.symbol.signature?.let { eraseSignature(it, declaration) }
            ?: declaration.symbol.privateSignature?.let { eraseSignature(it, declaration) }
            ?: compilationException("Can't retrieve a signature", declaration)
}