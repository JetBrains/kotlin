/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

abstract class MemoizedValueClassAbstractReplacements(protected val irFactory: IrFactory, protected val context: JvmBackendContext) {
    /**
     * Get a replacement for a function or a constructor.
     */
    abstract val getReplacementFunction: (IrFunction) -> IrSimpleFunction?

    abstract val originalFunctionForStaticReplacement: MutableMap<IrFunction, IrFunction>

    abstract val replaceOverriddenSymbols: (IrSimpleFunction) -> List<IrSimpleFunctionSymbol>
}