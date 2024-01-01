/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirResolveState
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.elementWithResolveState]
 */
abstract class FirElementWithResolveState : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    @Volatile
    @ResolveStateAccess
    lateinit var resolveState: FirResolveState
    abstract val moduleData: FirModuleData

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitElementWithResolveState(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformElementWithResolveState(this, data) as E
}
