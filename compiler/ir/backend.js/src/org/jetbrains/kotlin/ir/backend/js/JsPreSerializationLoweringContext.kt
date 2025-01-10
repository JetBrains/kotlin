/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.types.Variance

class JsPreSerializationLoweringContext(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
) : PreSerializationLoweringContext(irBuiltIns, configuration) {
    val dynamicType: IrDynamicType = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)
    val intrinsics: JsIntrinsics by lazy { JsIntrinsics(irBuiltIns) }

    override val ir: Ir by lazy {
        object : Ir() {
            override val symbols: Symbols = JsSymbols(irBuiltIns, irFactory.stageController, intrinsics)
        }
    }

    override val sharedVariablesManager: SharedVariablesManager by lazy { JsSharedVariablesManager(irBuiltIns, dynamicType, intrinsics) }

    override val allowExternalInlining: Boolean
        get() = true
}
