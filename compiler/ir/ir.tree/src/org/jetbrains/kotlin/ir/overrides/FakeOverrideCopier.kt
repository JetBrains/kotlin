/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.copyAnnotations

internal class FakeOverrideCopier(
    private val symbolRemapper: SymbolRemapper,
    private val typeRemapper: TypeRemapper,
    private val parent: IrClass,
    private val unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy
) {
    fun copySimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        val customization = unimplementedOverridesStrategy.computeCustomization(declaration, parent)

        return declaration.factory.createFunctionWithLateBinding(
            startOffset = parent.startOffset,
            endOffset = parent.endOffset,
            origin = customization.origin ?: IrDeclarationOrigin.FAKE_OVERRIDE,
            name = declaration.name,
            visibility = declaration.visibility,
            isInline = declaration.isInline,
            isExpect = declaration.isExpect,
            returnType = declaration.returnType,
            modality = customization.modality ?: declaration.modality,
            isTailrec = declaration.isTailrec,
            isSuspend = declaration.isSuspend,
            isOperator = declaration.isOperator,
            isInfix = declaration.isInfix,
            isExternal = declaration.isExternal,
        ).apply {
            contextReceiverParametersCount = declaration.contextReceiverParametersCount
            annotations = declaration.copyAnnotations()
            typeParameters = declaration.typeParameters.map(::copyTypeParameter)
            for ((i, thisTypeParameter) in typeParameters.withIndex()) {
                val otherTypeParameter = declaration.typeParameters[i]
                thisTypeParameter.superTypes = otherTypeParameter.superTypes.map(typeRemapper::remapType)
            }
            dispatchReceiverParameter = declaration.dispatchReceiverParameter?.let(::copyValueParameter)
            extensionReceiverParameter = declaration.extensionReceiverParameter?.let(::copyValueParameter)
            returnType = typeRemapper.remapType(declaration.returnType)
            valueParameters = declaration.valueParameters.map(::copyValueParameter)
        }
    }

    fun copyProperty(declaration: IrProperty): IrProperty {
        val customization = unimplementedOverridesStrategy.computeCustomization(declaration, parent)

        return declaration.factory.createPropertyWithLateBinding(
            parent.startOffset, parent.endOffset,
            customization.origin ?: IrDeclarationOrigin.FAKE_OVERRIDE,
            declaration.name,
            declaration.visibility,
            customization.modality ?: declaration.modality,
            isVar = declaration.isVar,
            isConst = declaration.isConst,
            isLateinit = declaration.isLateinit,
            isDelegated = declaration.isDelegated,
            isExpect = declaration.isExpect,
            isExternal = declaration.isExternal,
        ).apply {
            annotations = declaration.copyAnnotations()
            this.getter = declaration.getter?.let(::copySimpleFunction)
            this.setter = declaration.setter?.let(::copySimpleFunction)
        }
    }

    private fun copyValueParameter(declaration: IrValueParameter): IrValueParameter =
        declaration.factory.createValueParameter(
            startOffset = parent.startOffset,
            endOffset = parent.endOffset,
            origin = IrDeclarationOrigin.DEFINED,
            name = declaration.name,
            type = typeRemapper.remapType(declaration.type),
            isAssignable = declaration.isAssignable,
            symbol = symbolRemapper.getDeclaredValueParameter(declaration.symbol),
            index = declaration.index,
            varargElementType = declaration.varargElementType?.let(typeRemapper::remapType),
            isCrossinline = declaration.isCrossinline,
            isNoinline = declaration.isNoinline,
            isHidden = declaration.isHidden,
        ).apply {
            annotations = declaration.copyAnnotations()
            // Don't set the default value for fake overrides.
        }

    private fun copyTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
        declaration.factory.createTypeParameter(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = declaration.origin,
            name = declaration.name,
            symbol = symbolRemapper.getDeclaredTypeParameter(declaration.symbol),
            variance = declaration.variance,
            index = declaration.index,
            isReified = declaration.isReified,
        ).apply {
            annotations = declaration.copyAnnotations()
        }
}
