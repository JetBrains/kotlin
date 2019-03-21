/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.dump

abstract class IrPhaseDumperVerifier<in Context : CommonBackendContext, Data : IrElement>(
    val verifier: (Context, Data) -> Unit
) : PhaseDumperVerifier<Context, Data> {
    abstract fun Data.getElementName(): String

    // TODO: use a proper logger.
    override fun dump(phase: AnyNamedPhase, phaseConfig: PhaseConfig, data: Data, beforeOrAfter: BeforeOrAfter) {
        fun separator(title: String) = println("\n\n--- $title ----------------------\n")

        if (!shouldBeDumped(phaseConfig, data)) return

        val beforeOrAfterStr = beforeOrAfter.name.toLowerCase()
        val title = "IR for ${data.getElementName()} $beforeOrAfterStr ${phase.description}"
        separator(title)
        println(data.dump())
    }

    override fun verify(context: Context, data: Data) = verifier(context, data)

    private fun shouldBeDumped(phaseConfig: PhaseConfig, input: Data) =
        input.getElementName() !in phaseConfig.namesOfElementsExcludedFromDumping
}

class IrFileDumperVerifier<in Context : CommonBackendContext>(verifier: (Context, IrFile) -> Unit) :
    IrPhaseDumperVerifier<Context, IrFile>(verifier) {
    override fun IrFile.getElementName() = name
}

class IrModuleDumperVerifier<in Context : CommonBackendContext>(verifier: (Context, IrModuleFragment) -> Unit) :
    IrPhaseDumperVerifier<Context, IrModuleFragment>(verifier) {
    override fun IrModuleFragment.getElementName() = name.asString()
}

class EmptyDumperVerifier<in Context : CommonBackendContext, Data> : PhaseDumperVerifier<Context, Data> {
    override fun dump(phase: AnyNamedPhase, phaseConfig: PhaseConfig, data: Data, beforeOrAfter: BeforeOrAfter) {}
    override fun verify(context: Context, data: Data) {}
}

