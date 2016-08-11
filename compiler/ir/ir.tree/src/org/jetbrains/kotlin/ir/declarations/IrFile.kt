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
import org.jetbrains.kotlin.ir.SourceLocationManager
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrFile : IrCompoundDeclaration {
    val name: String
    val fileEntry: SourceLocationManager.FileEntry
    val module: IrModule
    override val descriptor: PackageFragmentDescriptor

    override val parent: Nothing? get() = null

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.FILE
}

class IrFileImpl(
        override val fileEntry: SourceLocationManager.FileEntry,
        override val name: String,
        override val descriptor: PackageFragmentDescriptor
) : IrCompoundDeclarationBase(0, fileEntry.maxOffset, IrDeclarationOriginKind.DEFINED), IrFile {
    override lateinit var module: IrModule

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitFile(this, data)
}
