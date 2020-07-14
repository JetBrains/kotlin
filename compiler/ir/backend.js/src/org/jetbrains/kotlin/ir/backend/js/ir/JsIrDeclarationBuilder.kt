/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

// This is a temporary class for migration to IrFactory. Usages should be refactored to use the factory directly once possible,
// since it doesn't add sufficient value on its own.
class JsIrDeclarationBuilder(private val irFactory: IrFactory) {
    fun buildValueParameter(parent: IrFunction, name: String, index: Int, type: IrType): IrValueParameter =
        buildValueParameter(parent) {
            this.origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            this.name = Name.identifier(name)
            this.index = index
            this.type = type
        }

    fun buildFunction(
        name: String,
        returnType: IrType,
        parent: IrDeclarationParent,
        visibility: Visibility = Visibilities.PUBLIC,
        modality: Modality = Modality.FINAL,
        isInline: Boolean = false,
        isExternal: Boolean = false,
        isTailrec: Boolean = false,
        isSuspend: Boolean = false,
        isExpect: Boolean = false,
        isOperator: Boolean = false,
        isInfix: Boolean = false,
        isFakeOverride: Boolean = false,
        origin: IrDeclarationOrigin = JsIrBuilder.SYNTHESIZED_DECLARATION,
    ): IrSimpleFunction =
        buildFunction(
            Name.identifier(name), returnType, parent, visibility, modality,
            isInline, isExternal, isTailrec, isSuspend, isExpect, isOperator, isInfix, isFakeOverride, origin
        )

    fun buildFunction(
        name: Name,
        returnType: IrType,
        parent: IrDeclarationParent,
        visibility: Visibility = Visibilities.PUBLIC,
        modality: Modality = Modality.FINAL,
        isInline: Boolean = false,
        isExternal: Boolean = false,
        isTailrec: Boolean = false,
        isSuspend: Boolean = false,
        isExpect: Boolean = false,
        isOperator: Boolean = false,
        isInfix: Boolean = false,
        isFakeOverride: Boolean = false,
        origin: IrDeclarationOrigin = JsIrBuilder.SYNTHESIZED_DECLARATION,
    ): IrSimpleFunction = irFactory.buildFun {
        this.origin = origin
        this.name = name
        this.visibility = visibility
        this.modality = modality
        this.returnType = returnType
        this.isInline = isInline
        this.isExternal = isExternal
        this.isTailrec = isTailrec
        this.isSuspend = isSuspend
        this.isOperator = isOperator
        this.isInfix = isInfix
        this.isExpect = isExpect
        this.isFakeOverride = isFakeOverride
    }.also {
        it.parent = parent
    }
}
