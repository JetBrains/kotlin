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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPackageFragmentSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

interface IrPackageFragment : IrElement, IrDeclarationContainer, IrSymbolOwner {
    val packageFragmentDescriptor: PackageFragmentDescriptor
    override val symbol: IrPackageFragmentSymbol
}

interface IrExternalPackageFragment : IrPackageFragment {
    override val symbol: IrExternalPackageFragmentSymbol
}

interface IrFile : IrPackageFragment {
    override val symbol: IrFileSymbol

    val fileEntry: SourceManager.FileEntry
    val fileAnnotations: MutableList<AnnotationDescriptor>

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrFile =
            accept(transformer, data) as IrFile
}

val IrFile.name: String get() = fileEntry.name
