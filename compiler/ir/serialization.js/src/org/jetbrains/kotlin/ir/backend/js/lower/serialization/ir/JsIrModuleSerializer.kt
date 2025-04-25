/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile

class JsIrModuleSerializer(
    settings: IrSerializationSettings,
    diagnosticReporter: IrDiagnosticReporter,
    irBuiltIns: IrBuiltIns,
    private val jsIrFileMetadataFactory: JsIrFileMetadataFactory = JsIrFileEmptyMetadataFactory,
) : IrModuleSerializer<JsIrFileSerializer>(settings, diagnosticReporter) {

    override val globalDeclarationTable = JsGlobalDeclarationTable(irBuiltIns)

    override fun createSerializerForFile(file: IrFile): JsIrFileSerializer =
        JsIrFileSerializer(settings, DeclarationTable.Default(globalDeclarationTable), jsIrFileMetadataFactory)
}
