/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Represents a single `init {}` block in a Kotlin class.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.anonymousInitializer]
 */
abstract class IrAnonymousInitializer : IrDeclarationBase() {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor

    abstract override val symbol: IrAnonymousInitializerSymbol

    abstract var isStatic: Boolean

    abstract var body: IrBlockBody

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitAnonymousInitializer(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        body = body.transform(transformer, data) as IrBlockBody
    }
}
