/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

interface TypeAliasCarrier : DeclarationCarrier<TypeAliasCarrier> {

    var typeParametersField: List<IrTypeParameter>

    override fun clone(): TypeAliasCarrier {
        return TypeAliasCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            typeParametersField
        )
    }
}

class TypeAliasCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var typeParametersField: List<IrTypeParameter>
    ) : TypeAliasCarrier