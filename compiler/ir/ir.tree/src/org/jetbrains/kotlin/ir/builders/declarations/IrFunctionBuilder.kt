/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrFunctionBuilder : IrDeclarationBuilder() {
    override var startOffset: Int = UNDEFINED_OFFSET
    override var endOffset: Int = UNDEFINED_OFFSET
    override var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    override var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    @Suppress("LateinitVarOverridesLateinitVar")
    override lateinit var name: Name

    var isInline: Boolean = false
    var isExternal: Boolean = false

    var returnType: IrType? = null
    val parameters: MutableList<IrValueParameter> = []
    var body: IrBody? = null

    var modality: Modality = Modality.FINAL
    var isTailrec: Boolean = false
    var isSuspend: Boolean = false
    var isExpect: Boolean = false
    var isOperator: Boolean = false
    var isInfix: Boolean = false

    var isPrimary: Boolean = false

    var isFakeOverride: Boolean = false
    val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = []
    var correspondingPropertySymbol: IrPropertySymbol? = null
    var companionExtensionClass: IrClassSymbol? = null

    var parent: IrDeclarationParent? = null
    val annotations: MutableList<IrAnnotation> = []
    val typeParameters: MutableList<IrTypeParameter> = []
    var metadata: MetadataSource? = null
    var attributeOwnerId: IrElement? = null

    var originalDeclaration: IrFunction? = null
    var containerSource: DeserializedContainerSource? = null

    fun updateFrom(from: IrFunction) {
        super.updateFrom(from)

        containerSource = from.containerSource

        isInline = from.isInline
        isExternal = from.isExternal
        isExpect = from.isExpect

        if (from is IrSimpleFunction) {
            modality = from.modality
            isTailrec = from.isTailrec
            isSuspend = from.isSuspend
            isOperator = from.isOperator
            isInfix = from.isInfix
            isFakeOverride = from.isFakeOverride
        } else {
            modality = Modality.FINAL
            isTailrec = false
            isSuspend = false
            isOperator = false
            isInfix = false
        }

        if (from is IrConstructor) {
            isPrimary = from.isPrimary
        }
    }
}
