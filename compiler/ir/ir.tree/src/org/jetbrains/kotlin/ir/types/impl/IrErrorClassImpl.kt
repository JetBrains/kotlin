/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

val IrErrorClassImpl: IrClass = IrFactoryImpl.createClass(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    origin = IrDeclarationOrigin.ERROR_CLASS,
    symbol = IrClassSymbolImpl(),
    name = Name.special("<error>"),
    kind = ClassKind.CLASS,
    visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
    modality = Modality.FINAL,
    source = SourceElement.NO_SOURCE,
).apply {
    parent = ErrorFile
}

private object ErrorFile : IrFile() {
    override val startOffset: Int
        get() = shouldNotBeCalled()
    override val endOffset: Int
        get() = shouldNotBeCalled()
    override var annotations: List<IrConstructorCall>
        get() = shouldNotBeCalled()
        set(_) {}

    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration>
        get() = shouldNotBeCalled()
    override val symbol: IrFileSymbol
        get() = shouldNotBeCalled()
    override var module: IrModuleFragment
        get() = shouldNotBeCalled()
        set(_) = shouldNotBeCalled()
    override var fileEntry: IrFileEntry
        get() = shouldNotBeCalled()
        set(_) = shouldNotBeCalled()
    override var metadata: MetadataSource?
        get() = shouldNotBeCalled()
        set(_) {}

    override var packageFqName: FqName
        get() = FqName.ROOT
        set(_) = shouldNotBeCalled()
}