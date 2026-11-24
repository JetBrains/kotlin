/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

abstract class IrFakeOverrideSymbolBase<S : IrBindableSymbol<I>, I : IrDeclaration>(
    val originalSymbol: S,
    val containingClassSymbol: IrClassSymbol,
    override val signature: IdSignature?
) : IrBindableSymbol<I> {

    override val isBound: Boolean
        get() = false

    override var privateSignature: IdSignature?
        get() = shouldNotBeCalled()
        set(_) {
            shouldNotBeCalled()
        }

    @UnsafeDuringIrConstructionAPI
    @Deprecated("Fake-override symbols never has its owner", level = DeprecationLevel.HIDDEN)
    override val owner: I
        get() = shouldNotBeCalled()

    @Deprecated("Fake-override symbols never has its owner", level = DeprecationLevel.HIDDEN)
    override fun bind(owner: I) {
        shouldNotBeCalled()
    }
}

class IrFunctionFakeOverrideSymbol(
    originalSymbol: IrSimpleFunctionSymbol,
    containingClassSymbol: IrClassSymbol,
    idSignature: IdSignature?
) : IrFakeOverrideSymbolBase<IrSimpleFunctionSymbol, IrSimpleFunction>(
    originalSymbol, containingClassSymbol, idSignature
), IrSimpleFunctionSymbol

class IrPropertyFakeOverrideSymbol(
    originalSymbol: IrPropertySymbol,
    containingClassSymbol: IrClassSymbol,
    idSignature: IdSignature?
) : IrFakeOverrideSymbolBase<IrPropertySymbol, IrProperty>(
    originalSymbol, containingClassSymbol, idSignature
), IrPropertySymbol

class IrFieldFakeOverrideSymbol(
    originalSymbol: IrFieldSymbol,
    containingClassSymbol: IrClassSymbol,
    idSignature: IdSignature?,
    val correspondingPropertySymbol: IrPropertySymbol
) : IrFakeOverrideSymbolBase<IrFieldSymbol, IrField>(
    originalSymbol, containingClassSymbol, idSignature
), IrFieldSymbol
