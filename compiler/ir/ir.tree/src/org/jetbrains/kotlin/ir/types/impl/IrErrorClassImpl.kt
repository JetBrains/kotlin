/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object IrErrorClassImpl : IrClassImpl(
    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.ERROR_CLASS, IrClassSymbolImpl(),
    Name.special("<error>"), ClassKind.CLASS, DescriptorVisibilities.DEFAULT_VISIBILITY, Modality.FINAL
) {
    override var parent: IrDeclarationParent
        get() = object : IrFile() {
            override val startOffset: Int
                get() = TODO("Not yet implemented")
            override val endOffset: Int
                get() = TODO("Not yet implemented")
            override var annotations: List<IrConstructorCall>
                get() = TODO("Not yet implemented")
                set(_) {}
            override val declarations: MutableList<IrDeclaration>
                get() = TODO("Not yet implemented")
            override val symbol: IrFileSymbol
                get() = TODO("Not yet implemented")
            override var module: IrModuleFragment
                get() = TODO("Not yet implemented")
                set(_) = TODO("Not yet implemented")
            override var fileEntry: IrFileEntry
                get() = TODO("Not yet implemented")
                set(_) = TODO("Not yet implemented")
            override var metadata: MetadataSource?
                get() = TODO("Not yet implemented")
                set(_) {}

            @ObsoleteDescriptorBasedAPI
            override val packageFragmentDescriptor: PackageFragmentDescriptor
                get() = TODO("Not yet implemented")
            override var packageFqName: FqName
                get() = FqName.ROOT
                set(_) = TODO("Not yet implemented")
        }
        set(_) = TODO()
}
