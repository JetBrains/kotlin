/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface BodyCarrier : Carrier {
    var containerFieldSymbol: IrSymbol?

    var containerField: IrDeclaration?
        get() = containerFieldSymbol?.owner?.cast()
        set(v) {
            containerFieldSymbol = v?.symbol
        }


    override fun clone(): BodyCarrier {
        return BodyCarrierImpl(lastModified, containerFieldSymbol)
    }
}

internal class BodyCarrierImpl(
    override val lastModified: Int,
    override var containerFieldSymbol: IrSymbol?
) : BodyCarrier
