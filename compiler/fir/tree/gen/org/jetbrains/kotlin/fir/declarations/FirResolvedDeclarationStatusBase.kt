/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.resolvedDeclarationStatusBase]
 */
interface FirResolvedDeclarationStatusBase : FirDeclarationStatusBase {
    override val source: KtSourceElement?
    override val visibility: Visibility
    override val isExpect: Boolean
    override val isActual: Boolean
    override val isExternal: Boolean
    override val isFromSealedClass: Boolean
    override val isFromEnumClass: Boolean
    override val defaultVisibility: Visibility
    override val defaultModality: Modality
    override val modality: Modality
    val effectiveVisibility: EffectiveVisibility

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedDeclarationStatusBase(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformResolvedDeclarationStatusBase(this, data) as E
}
