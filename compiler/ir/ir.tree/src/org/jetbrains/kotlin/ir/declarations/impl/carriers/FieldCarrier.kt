/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol

interface FieldCarrier : DeclarationCarrier<FieldCarrier> {
    var initializerField: IrExpressionBody?
    var correspondingPropertySymbolField: IrPropertySymbol?
    var metadataField: MetadataSource.Property?
    var overridenSymbolsField: List<IrFieldSymbol>

    override fun clone(): FieldCarrier {
        return FieldCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            initializerField,
            correspondingPropertySymbolField,
            metadataField,
            overridenSymbolsField
        )
    }
}

class FieldCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var initializerField: IrExpressionBody?,
    override var correspondingPropertySymbolField: IrPropertySymbol?,
    override var metadataField: MetadataSource.Property?,
    override var overridenSymbolsField: List<IrFieldSymbol>
): FieldCarrier