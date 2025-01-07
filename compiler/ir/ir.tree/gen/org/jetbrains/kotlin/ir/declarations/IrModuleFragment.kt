/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.Name

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.moduleFragment]
 */
abstract class IrModuleFragment : IrElementBase(), IrElement {
    abstract val descriptor: ModuleDescriptor

    abstract val name: Name

    abstract val files: MutableList<IrFile>

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitModuleFragment(this, data)

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrModuleFragment =
        accept(transformer, data) as IrModuleFragment

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        files.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        files.transformInPlace(transformer, data)
    }
}
