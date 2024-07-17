/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol

class KlibSyntheticAccessorGenerator(
    context: CommonBackendContext
) : SyntheticAccessorGenerator<CommonBackendContext>(context, addAccessorToParent = true) {
    override fun accessorModality(parent: IrDeclarationParent) = Modality.FINAL
    override fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopes: List<ScopeWithIr>) = parent

    override fun AccessorNameBuilder.contributeFunctionName(function: IrSimpleFunction) = contribute(function.name.asString())
    override fun AccessorNameBuilder.contributeFunctionSuffix(
        function: IrSimpleFunction,
        superQualifier: IrClassSymbol?,
        scopes: List<ScopeWithIr>,
    ) = Unit

    override fun AccessorNameBuilder.contributeFieldGetterName(field: IrField) = contribute("<get-${field.name}>")
    override fun AccessorNameBuilder.contributeFieldSetterName(field: IrField) = contribute("<set-${field.name}>")

    override fun AccessorNameBuilder.contributeFieldAccessorSuffix(field: IrField, superQualifierSymbol: IrClassSymbol?) =
        contribute(PROPERTY_MARKER)
}
