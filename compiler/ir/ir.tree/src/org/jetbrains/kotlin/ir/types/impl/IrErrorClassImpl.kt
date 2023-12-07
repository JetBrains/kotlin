/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled


@OptIn(IrImplementationDetail::class)
object IrErrorClassImpl : IrClassImpl(
    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.ERROR_CLASS, IrClassSymbolImpl(),
    Name.special("<error>"), ClassKind.CLASS, DescriptorVisibilities.DEFAULT_VISIBILITY, Modality.FINAL
) {
    override var parent: IrDeclarationParent
        get() = object : IrFile() {
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

            @ObsoleteDescriptorBasedAPI
            override val packageFragmentDescriptor: PackageFragmentDescriptor
                get() = shouldNotBeCalled()
            override val moduleDescriptor: ModuleDescriptor
                get() = shouldNotBeCalled()
            override var packageFqName: FqName
                get() = FqName.ROOT
                set(_) = shouldNotBeCalled()
        }
        set(_) = shouldNotBeCalled()
}
