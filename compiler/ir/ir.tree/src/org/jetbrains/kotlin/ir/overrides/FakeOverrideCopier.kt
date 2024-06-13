/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.copyAnnotations

internal class FakeOverrideCopier(
    private val valueParameters: MutableMap<IrValueParameterSymbol, IrValueParameterSymbol>,
    private val typeParameters: MutableMap<IrTypeParameterSymbol, IrTypeParameterSymbol>,
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
            getter = declaration.getter?.let(::copySimpleFunction)
            setter = declaration.setter?.let(::copySimpleFunction)
            if (getter == null) {
                backingField = declaration.backingField?.let(::copyBackingField)
            }
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
            symbol = IrValueParameterSymbolImpl(null),
            index = declaration.index,
            varargElementType = declaration.varargElementType?.let(typeRemapper::remapType),
            isCrossinline = declaration.isCrossinline,
            isNoinline = declaration.isNoinline,
            isHidden = declaration.isHidden,
        ).apply {
            valueParameters[declaration.symbol] = symbol
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
            symbol = IrTypeParameterSymbolImpl(null),
            variance = declaration.variance,
            index = declaration.index,
            isReified = declaration.isReified,
        ).apply {
            typeParameters[declaration.symbol] = symbol
            parent = newParent
            annotations = declaration.copyAnnotations()
        }

    private fun copyBackingField(declaration: IrField): IrField =
        declaration.factory.createField(
            startOffset = parentClass.startOffset,
            endOffset = parentClass.endOffset,
            origin = declaration.origin,
            name = declaration.name,
            visibility = declaration.visibility,
            symbol = IrFieldSymbolImpl(null),
            type = typeRemapper.remapType(declaration.type),
            isFinal = declaration.isFinal,
            isStatic = declaration.isStatic,
            isExternal = declaration.isExternal,
        ).apply {
            parent = parentClass
        }
}
