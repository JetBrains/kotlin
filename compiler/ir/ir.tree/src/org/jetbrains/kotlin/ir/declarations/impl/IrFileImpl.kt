/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName

class IrFileImpl(
    override var fileEntry: IrFileEntry,
    override val symbol: IrFileSymbol,
    override var packageFqName: FqName
) : IrFile() {
    constructor(
        fileEntry: IrFileEntry,
        packageFragmentDescriptor: PackageFragmentDescriptor
    ) : this(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName)

    constructor(
        fileEntry: IrFileEntry,
        packageFragmentDescriptor: PackageFragmentDescriptor,
        module: IrModuleFragment,
    ) : this(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName, module)

    constructor(
        fileEntry: IrFileEntry,
        symbol: IrFileSymbol,
        fqName: FqName,
        module: IrModuleFragment
    ) : this(fileEntry, symbol, fqName) {
        this.module = module
    }

    init {
        symbol.bind(this)
    }

    override lateinit var module: IrModuleFragment

    override val startOffset: Int
        get() = 0

    override val endOffset: Int
        get() = fileEntry.maxOffset

    @ObsoleteDescriptorBasedAPI
    override val packageFragmentDescriptor: PackageFragmentDescriptor
        get() = symbol.descriptor

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var annotations: List<IrConstructorCall> = emptyList()

    override var metadata: MetadataSource? = null
}
