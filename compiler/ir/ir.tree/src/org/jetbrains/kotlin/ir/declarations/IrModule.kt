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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

interface IrModule : IrDeclaration {
    override val descriptor: ModuleDescriptor

    override val startOffset: Int get() = UNDEFINED_OFFSET
    override val endOffset: Int get() = UNDEFINED_OFFSET

    override val parent: Nothing? get() = null
    override val indexInParent: Int get() = MODULE_INDEX

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.MODULE

    val files: List<IrFile>

    companion object {
        const val MODULE_INDEX = -1
    }
}

class IrModuleImpl(
        override val descriptor: ModuleDescriptor
) : IrModule {

    override val originKind: IrDeclarationOriginKind
        get() = IrDeclarationOriginKind.DEFINED

    override val files: MutableList<IrFile> = ArrayList()

    fun addFile(file: IrFileImpl) {
        files.add(file)
        file.module = this
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitModule(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        files.forEach { it.accept(visitor, data) }
    }
}