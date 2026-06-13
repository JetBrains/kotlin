/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.copyAnnotations
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class FakeOverrideCopier(
    private val valueParameters: MutableMap<IrValueParameterSymbol, IrValueParameterSymbol>,
    private val typeParameters: MutableMap<IrTypeParameterSymbol, IrTypeParameterSymbol>,
    private val typeRemapper: TypeRemapper,
    private val parentClass: IrClass
) {
    fun copySimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        return declaration.factory.createFunctionWithLateBinding(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            origin = IrDeclarationOrigin.FAKE_OVERRIDE,
            name = declaration.name,
            visibility = declaration.visibility,
            isInline = declaration.isInline,
            isExpect = declaration.isExpect,
            returnType = declaration.returnType,
            modality = declaration.modality,
            isTailrec = declaration.isTailrec,
            isSuspend = declaration.isSuspend,
            isOperator = declaration.isOperator,
            isInfix = declaration.isInfix,
            isExternal = declaration.isExternal,
        ).apply {
            parent = parentClass
            annotations = declaration.copyAnnotations().withSyntheticOffsets()
            typeParameters = declaration.typeParameters.map { copyTypeParameter(it, this) }
            for ([i, thisTypeParameter] in typeParameters.withIndex()) {
                val otherTypeParameter = declaration.typeParameters[i]
                thisTypeParameter.superTypes = otherTypeParameter.superTypes.map(typeRemapper::remapType)
            }
            parameters = declaration.parameters.map { copyValueParameter(it, this) }
            returnType = typeRemapper.remapType(declaration.returnType)
        }
    }

    fun copyProperty(declaration: IrProperty): IrProperty {
        return declaration.factory.createPropertyWithLateBinding(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            declaration.name,
            declaration.visibility,
            declaration.modality,
            isVar = declaration.isVar,
            isConst = declaration.isConst,
            isLateinit = declaration.isLateinit,
            isDelegated = declaration.isDelegated,
            isExpect = declaration.isExpect,
            isExternal = declaration.isExternal,
        ).apply {
            parent = parentClass
            annotations = declaration.copyAnnotations().withSyntheticOffsets()
            getter = declaration.getter?.let(::copySimpleFunction)
            setter = declaration.setter?.let(::copySimpleFunction)
            if (getter == null) {
                backingField = declaration.backingField?.let(::copyBackingField)
            }
        }
    }

    private fun copyValueParameter(declaration: IrValueParameter, newParent: IrDeclarationParent): IrValueParameter =
        declaration.factory.createValueParameter(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            kind = declaration.kind,
            name = declaration.name,
            type = typeRemapper.remapType(declaration.type),
            isAssignable = declaration.isAssignable,
            symbol = IrValueParameterSymbolImpl(null),
            varargElementType = declaration.varargElementType?.let(typeRemapper::remapType),
            isCrossinline = declaration.isCrossinline,
            isNoinline = declaration.isNoinline,
            isHidden = declaration.isHidden,
        ).apply {
            valueParameters[declaration.symbol] = symbol
            parent = newParent
            annotations = declaration.copyAnnotations().withSyntheticOffsets()
            // Don't set the default value for fake overrides.
        }

    private fun copyTypeParameter(declaration: IrTypeParameter, newParent: IrDeclarationParent): IrTypeParameter =
        declaration.factory.createTypeParameter(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            origin = declaration.origin,
            name = declaration.name,
            symbol = IrTypeParameterSymbolImpl(null),
            variance = declaration.variance,
            index = declaration.index,
            isReified = declaration.isReified,
        ).apply {
            typeParameters[declaration.symbol] = symbol
            parent = newParent
            annotations = declaration.copyAnnotations().withSyntheticOffsets()
        }

    private fun copyBackingField(declaration: IrField): IrField =
        declaration.factory.createField(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
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

    private fun List<IrAnnotation>.withSyntheticOffsets(): List<IrAnnotation> = onEach { annotation ->
        annotation.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.startOffset = SYNTHETIC_OFFSET
                element.endOffset = SYNTHETIC_OFFSET
                element.acceptChildrenVoid(this)
            }
        })
    }
}
