/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter

class JKlibGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(JKlibIrMangler()) {
    init {
        loadKnownBuiltins(builtIns)
    }
}

class JKlibModuleSerializer(
    settings: IrSerializationSettings,
    diagnosticReporter: IrDiagnosticReporter,
    builtIns: IrBuiltIns,
) : IrModuleSerializer<IrFileSerializer>(settings, diagnosticReporter) {
    override val globalDeclarationTable = JKlibGlobalDeclarationTable(builtIns)

    override fun createFileSerializer(settings: IrSerializationSettings): IrFileSerializer =
        IrFileSerializer(settings, DeclarationTable.Default(globalDeclarationTable))
}