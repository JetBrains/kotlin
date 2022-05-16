/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps track of replacement functions and multi-field value class box/unbox functions.
 */
class MemoizedMultiFieldValueClassReplacements(
    irFactory: IrFactory,
    context: JvmBackendContext
) : MemoizedValueClassAbstractReplacements(irFactory, context) {
    private val storageManager = LockBasedStorageManager("multi-field-value-class-replacements")

    override val originalFunctionForStaticReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()
    internal val originalFunctionForMethodReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()

    /**
     * Get a replacement for a function or a constructor.
     */
    override val getReplacementFunction: (IrFunction) -> IrSimpleFunction? = storageManager.createMemoizedFunctionWithNullableValues {
        TODO()
    }

    override val replaceOverriddenSymbols: (IrSimpleFunction) -> List<IrSimpleFunctionSymbol> = storageManager.createMemoizedFunction {
        TODO()
    }
}
