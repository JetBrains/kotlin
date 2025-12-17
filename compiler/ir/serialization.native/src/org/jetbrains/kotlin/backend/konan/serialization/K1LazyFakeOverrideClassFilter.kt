/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClassBase

object K1LazyFakeOverrideClassFilter : FakeOverrideClassFilter {
    override fun needToConstructFakeOverrides(clazz: IrClass): Boolean = (clazz as? IrLazyClassBase)?.isK2 != false
}
