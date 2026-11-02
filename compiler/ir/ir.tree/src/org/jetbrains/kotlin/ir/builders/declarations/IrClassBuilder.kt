/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrClassBuilder : IrDeclarationBuilder() {
    override var startOffset: Int = UNDEFINED_OFFSET
    override var endOffset: Int = UNDEFINED_OFFSET
    override var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    override var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    @Suppress("LateinitVarOverridesLateinitVar")
    override lateinit var name: Name

    var kind: ClassKind = ClassKind.CLASS
    var modality: Modality = Modality.FINAL

    var isCompanion: Boolean = false
    var isInner: Boolean = false
    var isData: Boolean = false
    var isExternal: Boolean = false
    var isValue: Boolean = false
    var isExpect: Boolean = false
    var isFun: Boolean = false
    var hasEnumEntries: Boolean = false

    var source: SourceElement = SourceElement.NO_SOURCE
    val superTypes: MutableList<IrType> = []
    var thisReceiver: IrValueParameter? = null
    var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? = null
    val sealedSubclasses: MutableList<IrClassSymbol> = []

    var parent: IrDeclarationParent? = null
    val annotations: MutableList<IrAnnotation> = []
    val typeParameters: MutableList<IrTypeParameter> = []
    val declarations: MutableList<IrDeclaration> = []
    var metadata: MetadataSource? = null
    var attributeOwnerId: IrElement? = null

    fun updateFrom(from: IrClass) {
        super.updateFrom(from)

        kind = from.kind
        modality = from.modality
        isCompanion = from.isCompanion
        isInner = from.isInner
        isData = from.isData
        isExternal = from.isExternal
        isValue = from.isValue
        isExpect = from.isExpect
        isFun = from.isFun
        hasEnumEntries = from.hasEnumEntries
    }
}
