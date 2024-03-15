/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.declarationStatusBase]
 */
abstract class FirDeclarationStatusBase : FirPureAbstractElement(), FirDeclarationStatus {
    abstract override val source: KtSourceElement?
    abstract override val visibility: Visibility
    abstract override val modality: Modality?
    abstract override val isExpect: Boolean
    abstract override val isActual: Boolean
    abstract override val isOverride: Boolean
    abstract override val isOperator: Boolean
    abstract override val isInfix: Boolean
    abstract override val isInline: Boolean
    abstract override val isTailRec: Boolean
    abstract override val isExternal: Boolean
    abstract override val isConst: Boolean
    abstract override val isLateInit: Boolean
    abstract override val isInner: Boolean
    abstract override val isCompanion: Boolean
    abstract override val isData: Boolean
    abstract override val isSuspend: Boolean
    abstract override val isStatic: Boolean
    abstract override val isFromSealedClass: Boolean
    abstract override val isFromEnumClass: Boolean
    abstract override val isFun: Boolean
    abstract override val hasStableParameterNames: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDeclarationStatusBase(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformDeclarationStatusBase(this, data) as E
}
