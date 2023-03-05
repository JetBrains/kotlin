/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.name.Name

class IrSimpleFunctionBuilder @PublishedApi internal constructor(
    private val name: Name,
    buildingContext: IrBuildingContext
) : IrFunctionBuilder<IrSimpleFunction>(buildingContext), IrOverridableDeclarationBuilder {

    override var declarationModality: Modality by SetAtMostOnce(Modality.FINAL)

    private var isSuspend: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun suspend(isSuspend: Boolean = true) {
        this.isSuspend = isSuspend
    }

    private var isTailrec: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun tailrec(isTailrec: Boolean = true) {
        this.isTailrec = isTailrec
    }

    private var isOperator: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun operator(isOperator: Boolean = true) {
        this.isOperator = isOperator
    }

    private var isInfix: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun infix(isInfix: Boolean = true) {
        this.isInfix = isInfix
    }

    override var isFakeOverride: Boolean by SetAtMostOnce(false)

    override var overriddenSymbols by SetAtMostOnce(mutableListOf<String>())

    private var correspondingPropertySymbolReference: String? by SetAtMostOnce(null)

    @PrettyIrDsl
    fun correspondingPropertySymbol(symbolReference: String) {
        correspondingPropertySymbolReference = symbolReference
    }

    @PublishedApi
    override fun build(): IrSimpleFunction {
        return buildingContext.irFactory.createFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = declarationOrigin,
            symbol = symbol(::IrSimpleFunctionSymbolImpl), // FIXME: Support public symbols
            name = name,
            visibility = declarationVisibility,
            modality = declarationModality,
            returnType = IrUninitializedType, // FIXME!!!
            isInline = isInline,
            isExternal = isExternal,
            isSuspend = isSuspend,
            isTailrec = isTailrec,
            isOperator = isOperator,
            isInfix = isInfix,
            isExpect = isExpect,
            isFakeOverride = isFakeOverride,
        ).also {
            addFunctionPropertiesTo(it)
            addOverriddenSymbolsTo(it, ::IrSimpleFunctionSymbolImpl) // FIXME: Support public symbols
            it.correspondingPropertySymbol = buildingContext.getOrCreateSymbol(
                correspondingPropertySymbolReference,
                ::IrPropertySymbolImpl  // FIXME: Support public symbols
            )
        }
    }
}
