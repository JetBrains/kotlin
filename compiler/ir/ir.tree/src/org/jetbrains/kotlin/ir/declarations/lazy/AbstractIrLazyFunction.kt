/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.DeserializableClass

abstract class AbstractIrLazyFunction : IrSimpleFunction(), IrLazyFunctionBase {
    abstract val isDeserializationEnabled: Boolean

    fun tryLoadIr(): Boolean {
        if (!isInline || isFakeOverride) return false
        if (!isDeserializationEnabled) return false
        val toplevel = getTopLevelDeclaration()
        return (toplevel as? DeserializableClass)?.loadIr() ?: false
    }
}