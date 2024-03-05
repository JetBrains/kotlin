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
    private val parentClass: IrClass,
    private val unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy
) {
    fun copySimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        val customization = unimplementedOverridesStrategy.computeCustomization(declaration, parentClass)

        return declaration.factory.createFunctionWithLateBinding(
            startOffset = parentClass.startOffset,
            endOffset = parentClass.endOffset,
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
            parent = parentClass
            contextReceiverParametersCount = declaration.contextReceiverParametersCount
            annotations = declaration.copyAnnotations()
            typeParameters = declaration.typeParameters.map { copyTypeParameter(it, this) }
            for ((i, thisTypeParameter) in typeParameters.withIndex()) {
                val otherTypeParameter = declaration.typeParameters[i]
                thisTypeParameter.superTypes = otherTypeParameter.superTypes.map(typeRemapper::remapType)
            }
            dispatchReceiverParameter = declaration.dispatchReceiverParameter?.let { copyValueParameter(it, this) }
            extensionReceiverParameter = declaration.extensionReceiverParameter?.let { copyValueParameter(it, this) }
            returnType = typeRemapper.remapType(declaration.returnType)
            valueParameters = declaration.valueParameters.map { copyValueParameter(it, this) }
        }
    }

    fun copyProperty(declaration: IrProperty): IrProperty {
        val customization = unimplementedOverridesStrategy.computeCustomization(declaration, parentClass)

        return declaration.factory.createPropertyWithLateBinding(
            parentClass.startOffset, parentClass.endOffset,
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
            parent = parentClass
            annotations = declaration.copyAnnotations()
            this.getter = declaration.getter?.let(::copySimpleFunction)
            this.setter = declaration.setter?.let(::copySimpleFunction)
        }
    }

    private fun copyValueParameter(declaration: IrValueParameter, newParent: IrDeclarationParent): IrValueParameter =
        declaration.factory.createValueParameter(
            startOffset = parentClass.startOffset,
            endOffset = parentClass.endOffset,
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
            parent = newParent
            annotations = declaration.copyAnnotations()
            // Don't set the default value for fake overrides.
        }

    private fun copyTypeParameter(declaration: IrTypeParameter, newParent: IrDeclarationParent): IrTypeParameter =
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
            parent = newParent
            annotations = declaration.copyAnnotations()
        }
}
