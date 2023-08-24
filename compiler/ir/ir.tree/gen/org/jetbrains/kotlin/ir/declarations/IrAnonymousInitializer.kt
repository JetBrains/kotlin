/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.anonymousInitializer]
 */
abstract class IrAnonymousInitializer : IrDeclarationBase() {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor

    abstract override val symbol: IrAnonymousInitializerSymbol

    abstract var isStatic: Boolean

    abstract var body: IrBlockBody

    override fun <R, D> accept(visitor: IrElementVisitorShallow<R, D>, data: D): R =
        visitor.visitAnonymousInitializer(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) {
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>,
            data: D) {
        body = body.transform(transformer, data) as IrBlockBody
    }
}
