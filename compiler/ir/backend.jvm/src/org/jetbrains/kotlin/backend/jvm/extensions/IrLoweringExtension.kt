/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.SameTypeNamedPhaseWrapper
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile

interface IrLoweringExtension {
    companion object : ProjectExtensionDescriptor<IrLoweringExtension>(
        "org.jetbrains.kotlin.irLoweringExtension", IrLoweringExtension::class.java)

    fun interceptLoweringPhases(phases: CompilerPhase<JvmBackendContext, IrFile, IrFile>) = phases
}
