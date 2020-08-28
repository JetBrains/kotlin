/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor

class IrExternalPackageFragmentImpl(
    override val symbol: IrExternalPackageFragmentSymbol,
    override val fqName: FqName
) : IrExternalPackageFragment() {
    override val startOffset: Int
        get() = UNDEFINED_OFFSET

    override val endOffset: Int
        get() = UNDEFINED_OFFSET

    init {
        symbol.bind(this)
    }

    @ObsoleteDescriptorBasedAPI
    override val packageFragmentDescriptor: PackageFragmentDescriptor
        get() = symbol.descriptor

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val containerSource: DeserializedContainerSource?
        get() = (symbol.descriptor as? DeserializedMemberDescriptor)?.containerSource

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitExternalPackageFragment(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        declarations.forEachIndexed { i, irDeclaration ->
            declarations[i] = irDeclaration.transform(transformer, data) as IrDeclaration
        }
    }

    companion object {
        fun createEmptyExternalPackageFragment(module: ModuleDescriptor, fqName: FqName): IrExternalPackageFragment =
            IrExternalPackageFragmentImpl(
                IrExternalPackageFragmentSymbolImpl(EmptyPackageFragmentDescriptor(module, fqName)), fqName
            )
    }
}
