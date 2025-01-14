/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrDeclarationWithAccessorsSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * This node is intended to unify different ways of handling property reference-like objects in IR.
 *
 * In particular, it covers:
 *   * References to regular properties
 *   * References implicitly passed to property delegation functions
 *   * References implicitly passed to local variable delegation functions (see [IrLocalDelegatedProperty])
 *
 * This node is intended to replace [IrPropertyReference] and [IrLocalDelegatedPropertyReference] in the IR tree.
 *
 * It's similar to [IrRichFunctionReference] except for property references, and has the same semantics, with the following differences:
 *   * There is no [IrRichFunctionReference.overriddenFunctionSymbol] because property references cannot implement a `fun interface`
 *     or be SAM-converted
 *   * There is no [IrRichFunctionReference.invokeFunction], but there is [getterFunction] with similar semantics instead
 *   * There is nullable [setterFunction] with similar semantics in case of mutable property
 *   * [boundValues] are passed as the first arguments to both [getterFunction] and [setterFunction]
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.richPropertyReference]
 */
abstract class IrRichPropertyReference : IrExpression() {
    abstract var reflectionTargetSymbol: IrDeclarationWithAccessorsSymbol?

    abstract val boundValues: MutableList<IrExpression>

    abstract var getterFunction: IrSimpleFunction

    abstract var setterFunction: IrSimpleFunction?

    abstract var origin: IrStatementOrigin?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitRichPropertyReference(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitRichPropertyReference(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitRichPropertyReference(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitRichPropertyReference(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        boundValues.forEach { it.accept(visitor, data) }
        getterFunction.accept(visitor, data)
        setterFunction?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        boundValues.forEach { it.acceptVoid(visitor) }
        getterFunction.acceptVoid(visitor)
        setterFunction?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        boundValues.transformInPlace(transformer, data)
        getterFunction = getterFunction.transform(transformer, data) as IrSimpleFunction
        setterFunction = setterFunction?.transform(transformer, data) as IrSimpleFunction?
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        boundValues.transformInPlace(transformer)
        getterFunction = getterFunction.transformVoid(transformer) as IrSimpleFunction
        setterFunction = setterFunction?.transformVoid(transformer) as IrSimpleFunction?
    }
}
