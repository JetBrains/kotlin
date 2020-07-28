/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclaration

interface BodyCarrier : Carrier {
    var containerField: IrDeclaration?

    override fun clone(): BodyCarrier {
        return BodyCarrierImpl(lastModified, containerField)
    }
}

internal class BodyCarrierImpl(
    override val lastModified: Int,
    override var containerField: IrDeclaration?
) : BodyCarrier
