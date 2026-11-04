/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler

class JvmIrBackendInput(
    val state: GenerationState,
    val codegenFactory: JvmIrCodegenFactory,
    val backendInput: JvmIrCodegenFactory.BackendInput,
    val sourceFiles: List<KtSourceFile>,
    override val irMangler: KotlinMangler.IrMangler,
) : IrBackendInput() {
    override val irModuleFragment: IrModuleFragment
        get() = backendInput.irModuleFragment

    override val irBuiltIns: IrBuiltIns
        get() = backendInput.irBuiltIns

    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = state.diagnosticReporter as BaseDiagnosticsCollector
}
