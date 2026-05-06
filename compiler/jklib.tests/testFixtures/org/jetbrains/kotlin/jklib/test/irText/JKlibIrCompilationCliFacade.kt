/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibIrCompilationArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibIrCompilationPhase
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BackendKinds.IrBackend
import org.jetbrains.kotlin.test.model.DeserializerFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

@Suppress("UNCHECKED_CAST")
class JKlibIrCompilationCliFacade(testServices: TestServices) :
    DeserializerFacade<JKlibKLibWithArtifact, IrBackendInput>(testServices, ArtifactKinds.KLib as ArtifactKind<JKlibKLibWithArtifact>, IrBackend) {

    override fun transform(module: TestModule, inputArtifact: JKlibKLibWithArtifact): IrBackendInput {
        val serializationArtifact = inputArtifact.cliArtifact

        val compilationArtifact = JKlibIrCompilationPhase.executePhase(serializationArtifact)

        val diagnosticsReporter = serializationArtifact.configuration.diagnosticsCollector

        return JKlibDeserializedIrBackendInput(compilationArtifact, diagnosticsReporter)
    }
}

class JKlibDeserializedIrBackendInput(
    val compilationArtifact: JKlibIrCompilationArtifact,
    override val diagnosticReporter: BaseDiagnosticsCollector
) : IrBackendInput() {

    override val irModuleFragment: IrModuleFragment
        get() = compilationArtifact.moduleFragment

    override val irBuiltIns: IrBuiltIns
        get() = compilationArtifact.pluginContext.irBuiltIns

    // Bypassing full mangler initialization since IR text verification doesn't necessitate linking steps.
    override val irMangler: KotlinMangler.IrMangler
        get() = object : KotlinMangler.IrMangler {
            override fun IrDeclaration.mangleString(compatibleMode: Boolean): String = ""
            override fun IrDeclaration.isExported(compatibleMode: Boolean): Boolean = true
            override fun IrDeclaration.signatureString(compatibleMode: Boolean): String = ""
            override val String.hashMangle: Long get() = 0L
        }
}
