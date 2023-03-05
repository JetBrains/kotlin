/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.name.Name

@PrettyIrDsl
class IrClassBuilder @PublishedApi internal constructor(
    private val name: Name,
    buildingContext: IrBuildingContext,
) : IrDeclarationBuilder<IrClass>(buildingContext),
    IrDeclarationWithVisibilityBuilder,
    IrDeclarationWithModalityBuilder,
    IrDeclarationContainerBuilder,
    IrSymbolOwnerBuilder,
    IrPossiblyExternalDeclarationBuilder {

    override var symbolReference: String? by SetAtMostOnce(null)

    override var declarationVisibility: DescriptorVisibility by SetAtMostOnce(DescriptorVisibilities.DEFAULT_VISIBILITY)

    override var isExternal: Boolean by SetAtMostOnce(false)

    override val __internal_declarationBuilders = mutableListOf<IrDeclarationBuilder<*>>()

    private var classKind: ClassKind by SetAtMostOnce(ClassKind.CLASS)

    @IrNodePropertyDsl
    fun kind(kind: ClassKind) {
        this.classKind = kind
    }

    @IrNodePropertyDsl
    fun kindClass() {
        kind(ClassKind.CLASS)
    }

    @IrNodePropertyDsl
    fun kindInterface() {
        kind(ClassKind.INTERFACE)
    }

    @IrNodePropertyDsl
    fun kindEnumClass() {
        kind(ClassKind.ENUM_CLASS)
    }

    @IrNodePropertyDsl
    fun kindEnumEntry() {
        kind(ClassKind.ENUM_ENTRY)
    }

    @IrNodePropertyDsl
    fun kindAnnotationClass() {
        kind(ClassKind.ANNOTATION_CLASS)
    }

    @IrNodePropertyDsl
    fun kindObject() {
        kind(ClassKind.OBJECT)
    }

    override var declarationModality: Modality by SetAtMostOnce(Modality.FINAL)

    private var isCompanion: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun companion(isCompanion: Boolean = true) {
        this.isCompanion = isCompanion
    }

    private var isInner: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun inner(isInner: Boolean = true) {
        this.isInner = isInner
    }

    private var isData: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun data(isData: Boolean = true) {
        this.isData = isData
    }

    private var isValue: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun value(isValue: Boolean = true) {
        this.isValue = isValue
    }

    private var isExpect: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun expect(isExpect: Boolean = true) {
        this.isExpect = isExpect
    }

    private var isFun: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun functional(isFun: Boolean = true) {
        this.isFun = isFun
    }

    @PublishedApi
    override fun build(): IrClass {
        return buildingContext.irFactory.createClass(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = declarationOrigin,
            symbol = symbol(::IrClassSymbolImpl), // FIXME: Support public symbols
            name = name,
            kind = classKind,
            visibility = declarationVisibility,
            modality = declarationModality,
            isCompanion = isCompanion,
            isInner = isInner,
            isData = isData,
            isExternal = isExternal,
            isValue = isValue,
            isExpect = isExpect,
            isFun = isFun,
        ).also {
            recordSymbolFromOwner(it)
            addAnnotationsTo(it)
            // TODO: thisReceiver
            addDeclarationsTo(it)
            // TODO: typeParameters
            // TODO: superTypes
            // TODO: valueClassRepresentation
            // TODO: sealedSubclasses
        }
    }
}
