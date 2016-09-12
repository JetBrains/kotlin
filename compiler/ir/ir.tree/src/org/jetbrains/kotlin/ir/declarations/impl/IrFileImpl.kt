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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

class IrFileImpl(
        override val fileEntry: SourceManager.FileEntry,
        override val name: String,
        override val packageFragmentDescriptor: PackageFragmentDescriptor
) : IrElementBase(0, fileEntry.maxOffset), IrFile {
    override val fileAnnotations: MutableList<AnnotationDescriptor> = SmartList()

    fun addAnnotation(annotation: AnnotationDescriptor) {
        fileAnnotations.add(annotation)
    }

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    fun addDeclaration(declaration: IrDeclaration) {
        declaration.setTreeLocation(this, declarations.size)
        declarations.add(declaration)
    }

    override fun getChild(slot: Int): IrElement? =
            declarations.getOrNull(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        declarations.getOrNull(slot)?.detach() ?: throwNoSuchSlot(slot)
        declarations[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitFile(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }


}