/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.script]
 */
abstract class IrScript : IrDeclarationBase(), IrDeclarationWithName, IrDeclarationParent,
        IrStatementContainer, IrMetadataSourceOwner {
    abstract override val symbol: IrScriptSymbol

    abstract var thisReceiver: IrValueParameter?

    abstract var baseClass: IrType?

    abstract var explicitCallParameters: List<IrVariable>

    abstract var implicitReceiversParameters: List<IrValueParameter>

    abstract var providedProperties: List<IrPropertySymbol>

    abstract var providedPropertiesParameters: List<IrValueParameter>

    abstract var resultProperty: IrPropertySymbol?

    abstract var earlierScriptsParameter: IrValueParameter?

    abstract var importedScripts: List<IrScriptSymbol>?

    abstract var earlierScripts: List<IrScriptSymbol>?

    abstract var targetClass: IrClassSymbol?

    abstract var constructor: IrConstructor?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitScript(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
        thisReceiver?.accept(visitor, data)
        explicitCallParameters.forEach { it.accept(visitor, data) }
        implicitReceiversParameters.forEach { it.accept(visitor, data) }
        providedPropertiesParameters.forEach { it.accept(visitor, data) }
        earlierScriptsParameter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        statements.transformInPlace(transformer, data)
        thisReceiver = thisReceiver?.transform(transformer, data)
        explicitCallParameters = explicitCallParameters.transformIfNeeded(transformer, data)
        implicitReceiversParameters = implicitReceiversParameters.transformIfNeeded(transformer,
                data)
        providedPropertiesParameters = providedPropertiesParameters.transformIfNeeded(transformer,
                data)
        earlierScriptsParameter = earlierScriptsParameter?.transform(transformer, data)
    }
}
