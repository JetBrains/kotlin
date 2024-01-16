/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.NativeStandardInteropNames

class KonanIrModuleSerializer(
        diagnosticReporter: IrDiagnosticReporter,
        irBuiltIns: IrBuiltIns,
        compatibilityMode: CompatibilityMode,
        normalizeAbsolutePaths: Boolean,
        sourceBaseDirs: Collection<String>,
        private val languageVersionSettings: LanguageVersionSettings,
        private val bodiesOnlyForInlines: Boolean = false,
        private val publicAbiOnly: Boolean = false,
        shouldCheckSignaturesOnUniqueness: Boolean = true,
) : IrModuleSerializer<KonanIrFileSerializer>(
        diagnosticReporter,
        compatibilityMode,
        normalizeAbsolutePaths,
        sourceBaseDirs,
        shouldCheckSignaturesOnUniqueness,
) {

    override val globalDeclarationTable = KonanGlobalDeclarationTable(irBuiltIns)

    // We skip files with IR for C structs and enums because they should be
    // generated anew.
    //
    // See [IrProviderForCEnumAndCStructStubs.kt#L31] on why we generate IR.
    // We may switch from IR generation to LazyIR later (at least for structs; enums are tricky)
    // without changing kotlin libraries that depend on interop libraries.
    override fun backendSpecificFileFilter(file: IrFile): Boolean =
        file.fileEntry.name != NativeStandardInteropNames.cTypeDefinitionsFileName

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(
                    KonanDeclarationTable(globalDeclarationTable),
                    languageVersionSettings = languageVersionSettings,
                    bodiesOnlyForInlines = bodiesOnlyForInlines,
                    compatibilityMode = compatibilityMode,
                    normalizeAbsolutePaths = normalizeAbsolutePaths,
                    sourceBaseDirs = sourceBaseDirs,
                    publicAbiOnly = publicAbiOnly
            )
}
