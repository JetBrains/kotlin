/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

internal interface AnonymousInitializerCarrier : DeclarationCarrier {
    var bodyField: IrBlockBody?

    override fun clone(): AnonymousInitializerCarrier {
        return AnonymousInitializerCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            bodyField
        )
    }
}

internal class AnonymousInitializerCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var bodyField: IrBlockBody?
) : AnonymousInitializerCarrier
