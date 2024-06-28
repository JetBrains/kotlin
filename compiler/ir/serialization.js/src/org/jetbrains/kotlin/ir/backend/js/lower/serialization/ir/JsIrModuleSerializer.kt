/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile

class JsIrModuleSerializer(
    diagnosticReporter: IrDiagnosticReporter,
    irBuiltIns: IrBuiltIns,
    compatibilityMode: CompatibilityMode,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>,
    private val languageVersionSettings: LanguageVersionSettings,
    shouldCheckSignaturesOnUniqueness: Boolean = true,
    private val jsIrFileMetadataFactory: JsIrFileMetadataFactory = JsIrFileEmptyMetadataFactory,
) : IrModuleSerializer<JsIrFileSerializer>(
    diagnosticReporter,
    compatibilityMode,
    normalizeAbsolutePaths,
    sourceBaseDirs,
    shouldCheckSignaturesOnUniqueness,
) {

    override val globalDeclarationTable = JsGlobalDeclarationTable(irBuiltIns)

    override fun createSerializerForFile(file: IrFile): JsIrFileSerializer =
        JsIrFileSerializer(
            DeclarationTable(globalDeclarationTable),
            compatibilityMode = compatibilityMode,
            normalizeAbsolutePaths = normalizeAbsolutePaths,
            sourceBaseDirs = sourceBaseDirs,
            languageVersionSettings = languageVersionSettings,
            jsIrFileMetadataFactory = jsIrFileMetadataFactory
        )
}
