/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPublicSymbolBase
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.IdSignature

object KonanFakeOverrideClassFilter : FakeOverrideClassFilter {
    private fun IdSignature.isInteropSignature(): Boolean = with(this) {
        IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrClassSymbol.isInterop(): Boolean {
        if (this is IrPublicSymbolBase<*> && this.signature.isInteropSignature()) return true

        // K2 doesn't properly put signatures into such symbols yet, workaround:
        return this.isBound && this.owner is Fir2IrLazyClass && this.owner.isFromInteropLibraryByDescriptor()
    }

    // This is an alternative to .isObjCClass that doesn't need to walk up all the class heirarchy,
    // rather it only looks at immediate super class symbols.
    private fun IrClass.hasInteropSuperClass() = this.superTypes
        .mapNotNull { it.classOrNull }
        .any { it.isInterop() }

    override fun needToConstructFakeOverrides(clazz: IrClass): Boolean {
        return !clazz.hasInteropSuperClass() && clazz !is IrLazyClass
    }
}
