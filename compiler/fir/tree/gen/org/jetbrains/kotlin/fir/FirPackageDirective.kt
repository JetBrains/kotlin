/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.FqName

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.packageDirective]
 */
abstract class FirPackageDirective : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    abstract val packageFqName: FqName

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitPackageDirective(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformPackageDirective(this, data) as E
}
