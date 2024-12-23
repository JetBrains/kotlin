/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

abstract class PreSerializationLoweringPhasesProvider<Context : LoweringContext> {
    abstract fun getLowerings(): List<SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>>
}