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
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

class IrFileImpl(
    override val fileEntry: SourceManager.FileEntry,
    override val symbol: IrFileSymbol,
    override val fqName: FqName
) :
    IrElementBase(0, fileEntry.maxOffset),
    IrFile {

    constructor(
        fileEntry: SourceManager.FileEntry,
        symbol: IrFileSymbol
    ) : this(fileEntry, symbol, symbol.descriptor.fqName)

    constructor(
        fileEntry: SourceManager.FileEntry,
        packageFragmentDescriptor: PackageFragmentDescriptor
    ) : this(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName)

    init {
        symbol.bind(this)
    }

    override val packageFragmentDescriptor: PackageFragmentDescriptor get() = symbol.descriptor

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override val annotations: MutableList<IrConstructorCall> = ArrayList()

    override var metadata: MetadataSource.File? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        for (i in 0 until declarations.size) {
            declarations[i] = declarations[i].transform(transformer, data) as IrDeclaration
        }
    }
}
