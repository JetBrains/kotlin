/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.DefaultIrPhaseRunner
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name

object JvmPhaseRunner : DefaultIrPhaseRunner<JvmBackendContext, IrFile>() {
    override val startPhaseMarker = IrFileStartPhase
    override val endPhaseMarker = IrFileEndPhase

    override fun phases(context: JvmBackendContext) = context.phases
    override fun elementName(input: IrFile) = input.name
    override fun configuration(context: JvmBackendContext) = context.state.configuration
}