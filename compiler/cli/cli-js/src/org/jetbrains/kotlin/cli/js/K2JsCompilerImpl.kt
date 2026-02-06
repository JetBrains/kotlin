/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.LoweredIr
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsBuilt
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsCodeGenerator
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.util.PhaseType

class Ir2JsTransformer private constructor(
    val module: ModulesStructure,
    val messageCollector: MessageCollector,
    val configuration: CompilerConfiguration,
    val mainCallArguments: List<String>?,
    val keep: Set<String>,
    val dceRuntimeDiagnostic: String?,
    val safeExternalBoolean: Boolean,
    val safeExternalBooleanDiagnostic: String?,
    val granularity: JsGenerationGranularity,
    val dce: Boolean,
    val minimizedMemberNames: Boolean,
) {
    constructor(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        messageCollector: MessageCollector,
        mainCallArguments: List<String>?,
    ) : this(
        module,
        messageCollector,
        configuration,
        mainCallArguments,
        keep = configuration.keep.toSet(),
        dceRuntimeDiagnostic = configuration.dceRuntimeDiagnostic,
        safeExternalBoolean = configuration.safeExternalBoolean,
        safeExternalBooleanDiagnostic = configuration.safeExternalBooleanDiagnostic,
        granularity = configuration.artifactConfiguration!!.granularity,
        dce = configuration.dce,
        minimizedMemberNames = configuration.minimizedMemberNames,
    )

    private val performanceManager = module.compilerConfiguration.perfManager

    private fun lowerIr(): LoweredIr {
        return compile(
            mainCallArguments,
            module,
            IrFactoryImplForJsIC(WholeWorldStageController()),
            keep = keep,
            dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                dceRuntimeDiagnostic,
                configuration
            ),
            safeExternalBoolean = safeExternalBoolean,
            safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                safeExternalBooleanDiagnostic,
                configuration
            ),
            granularity = granularity,
        )
    }

    private fun makeJsCodeGenerator(): JsCodeGenerator {
        val ir = lowerIr()
        val transformer = IrModuleToJsTransformer(ir.context, ir.moduleFragmentToUniqueName, mainCallArguments != null)

        val mode = TranslationMode.fromFlags(dce, granularity, minimizedMemberNames)
        return transformer
            .also { performanceManager?.notifyPhaseStarted(PhaseType.Backend) }
            .makeJsCodeGenerator(ir.allModules, mode)
    }

    fun compileAndTransformIrNew(): CompilationOutputsBuilt {
        return makeJsCodeGenerator()
            .generateJsCode(relativeRequirePath = true, outJsProgram = false)
            .also {
                performanceManager?.notifyPhaseFinished(PhaseType.Backend)
            }
    }
}
