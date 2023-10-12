/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.expressions.BirStatementContainer
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.transformIfNeeded
import org.jetbrains.kotlin.bir.util.transformInPlace
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.script]
 */
abstract class BirScript : BirDeclarationBase(), BirDeclarationWithName,
        BirDeclarationParent, BirStatementContainer, BirMetadataSourceOwner {
    abstract override val symbol: BirScriptSymbol

    abstract var thisReceiver: BirValueParameter?

    abstract var baseClass: BirType?

    abstract var explicitCallParameters: List<BirVariable>

    abstract var implicitReceiversParameters: List<BirValueParameter>

    abstract var providedProperties: List<BirPropertySymbol>

    abstract var providedPropertiesParameters: List<BirValueParameter>

    abstract var resultProperty: BirPropertySymbol?

    abstract var earlierScriptsParameter: BirValueParameter?

    abstract var importedScripts: List<BirScriptSymbol>?

    abstract var earlierScripts: List<BirScriptSymbol>?

    abstract var targetClass: BirClassSymbol?

    abstract var constructor: BirConstructor?

    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitScript(this, data)

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
        thisReceiver?.accept(visitor, data)
        explicitCallParameters.forEach { it.accept(visitor, data) }
        implicitReceiversParameters.forEach { it.accept(visitor, data) }
        providedPropertiesParameters.forEach { it.accept(visitor, data) }
        earlierScriptsParameter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
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
