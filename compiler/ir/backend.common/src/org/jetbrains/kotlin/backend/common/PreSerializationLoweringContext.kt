/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl

/**
 * This backend context is used in the first compilation stage. Namely, it is passed to lowerings
 * that are run before serializing IR into a KLIB.
 */
abstract class PreSerializationLoweringContext(
    override val irBuiltIns: IrBuiltIns,
    override val configuration: CompilerConfiguration,
    val diagnosticReporter: DiagnosticReporter,
) : LoweringContext {
    override val mapping: Mapping = Mapping()

    override val irFactory: IrFactory
        get() = IrFactoryImpl

    override var inVerbosePhase: Boolean = false
}
