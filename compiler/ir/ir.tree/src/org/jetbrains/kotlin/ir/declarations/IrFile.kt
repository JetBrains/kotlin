/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPackageFragmentSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.FqName
import java.io.File

interface IrPackageFragment : IrElement, IrDeclarationContainer, IrSymbolOwner {
    val packageFragmentDescriptor: PackageFragmentDescriptor
    override val symbol: IrPackageFragmentSymbol

    val fqName: FqName
}

interface IrExternalPackageFragment : IrPackageFragment {
    override val symbol: IrExternalPackageFragmentSymbol
}

interface IrFile : IrPackageFragment, IrMutableAnnotationContainer, IrMetadataSourceOwner {
    override val symbol: IrFileSymbol

    val fileEntry: SourceManager.FileEntry

    override var metadata: MetadataSource.File?

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrFile =
        accept(transformer, data) as IrFile
}

val IrFile.path: String get() = fileEntry.name
val IrFile.name: String get() = File(path).name
