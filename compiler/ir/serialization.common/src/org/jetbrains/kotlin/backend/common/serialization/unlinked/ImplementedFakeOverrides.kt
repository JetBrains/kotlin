/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*

internal inline fun <reified T : IrOverridableDeclaration<*>> T.deepCopyWithImplementedFakeOverrides(): T {
    val clazz = parentAsClass

    return deepCopyWithSymbols(clazz, DeepCopySymbolRemapperPreservingSignatures()) { symbolRemapper, typeRemapper ->
        ImplementedFakeOverrideCopier(clazz, symbolRemapper, typeRemapper)
    }
}

internal class ImplementedFakeOverrideCopier(
    private val clazz: IrClass,
    private val symbolRemapper: SymbolRemapper,
    private val typeRemapper: TypeRemapper
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
    override fun visitProperty(declaration: IrProperty): IrProperty =
        declaration.factory.createProperty(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION, // Customized.
            symbol = symbolRemapper.getDeclaredProperty(declaration.symbol),
            name = declaration.name,
            visibility = declaration.visibility,
            modality = clazz.modality, // Customized.
            isVar = declaration.isVar,
            isConst = declaration.isConst,
            isLateinit = declaration.isLateinit,
            isDelegated = declaration.isDelegated,
            isExternal = declaration.isExternal,
            isExpect = declaration.isExpect,
            isFakeOverride = false, // Customized.
            containerSource = declaration.containerSource,
        ).apply {
            overriddenSymbols = declaration.overriddenSymbols.map { symbolRemapper.getReferencedProperty(it) }
            copyAttributes(declaration)
            transformAnnotations(declaration)
            backingField = declaration.backingField?.transform()?.also { it.correspondingPropertySymbol = symbol }
            getter = declaration.getter?.transform()?.also { it.correspondingPropertySymbol = symbol }
            setter = declaration.setter?.transform()?.also { it.correspondingPropertySymbol = symbol }
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        declaration.factory.createFunction(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION, // Customized.
            symbol = symbolRemapper.getDeclaredFunction(declaration.symbol),
            name = declaration.name,
            visibility = declaration.visibility,
            modality = clazz.modality, // Customized.
            returnType = declaration.returnType,
            isInline = declaration.isInline,
            isExternal = declaration.isExternal,
            isTailrec = declaration.isTailrec,
            isSuspend = declaration.isSuspend,
            isOperator = declaration.isOperator,
            isInfix = declaration.isInfix,
            isExpect = declaration.isExpect,
            isFakeOverride = false, // Customized.
            containerSource = declaration.containerSource,
        ).apply {
            overriddenSymbols = declaration.overriddenSymbols.map { symbolRemapper.getReferencedFunction(it) as IrSimpleFunctionSymbol }
            contextReceiverParametersCount = declaration.contextReceiverParametersCount
            copyAttributes(declaration)
            transformAnnotations(this)
            copyTypeParametersFrom(declaration)
            typeRemapper.withinScope(this) {
                dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                returnType = typeRemapper.remapType(declaration.returnType)
                valueParameters = declaration.valueParameters.transform()
                body = factory.createBlockBody(
                    declaration.body?.startOffset ?: declaration.startOffset,
                    declaration.body?.endOffset ?: declaration.endOffset
                ) // the body should be empty
            }
        }
}

private val MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION =
    object : IrDeclarationOriginImpl("MISSING_ABSTRACT_CALLABLE_MEMBER_IMPLEMENTATION", isSynthetic = true) {}
