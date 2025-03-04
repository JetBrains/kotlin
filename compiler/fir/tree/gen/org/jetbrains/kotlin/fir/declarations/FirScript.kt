/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.script]
 */
abstract class FirScript : FirDeclaration(), FirControlFlowGraphOwner {
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract val name: Name
    @DirectDeclarationsAccess
    abstract val declarations: List<FirDeclaration>
    abstract override val source: KtSourceElement
    abstract override val symbol: FirScriptSymbol
    abstract val parameters: List<FirProperty>
    abstract val receivers: List<FirScriptReceiverParameter>
    abstract val resultPropertyName: Name?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitScript(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformScript(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract fun replaceDeclarations(newDeclarations: List<FirDeclaration>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirScript

    abstract fun <D> transformDeclarations(transformer: FirTransformer<D>, data: D): FirScript

    abstract fun <D> transformParameters(transformer: FirTransformer<D>, data: D): FirScript

    abstract fun <D> transformReceivers(transformer: FirTransformer<D>, data: D): FirScript
}
