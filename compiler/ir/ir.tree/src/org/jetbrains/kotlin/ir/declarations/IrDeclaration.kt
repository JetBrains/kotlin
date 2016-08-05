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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

interface IrDeclaration : IrElement {
    val descriptor: DeclarationDescriptor
    override val parent: IrDeclaration?
}

interface IrCompoundDeclaration : IrDeclaration {
    val childDeclarations: List<IrDeclaration>
}

interface IrDeclarationNonRoot : IrDeclaration {
    override val parent: IrDeclaration
}

abstract class IrDeclarationBase(sourceLocation: SourceLocation) : IrElementBase(sourceLocation), IrDeclaration {
    override var parent: IrDeclaration? = null
}

abstract class IrDeclarationNonRootBase(sourceLocation: SourceLocation) : IrElementBase(sourceLocation), IrDeclarationNonRoot

abstract class IrCompoundDeclarationBase(sourceLocation: SourceLocation) : IrDeclarationBase(sourceLocation), IrCompoundDeclaration {
    override val childDeclarations: MutableList<IrDeclaration> = ArrayList()

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        childDeclarations.forEach { it.accept(visitor, data) }
    }
}

val IrDeclaration.containingDeclaration: IrDeclaration?
    get() = parent

val IrDeclarationNonRoot.continingDeclaration: IrDeclaration
    get() = parent
