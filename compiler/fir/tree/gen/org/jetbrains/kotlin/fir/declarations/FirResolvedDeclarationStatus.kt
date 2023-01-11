/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirResolvedDeclarationStatus : FirDeclarationStatus {
    override val source: KtSourceElement?
    override val visibility: Visibility
    override val modality: Modality?
    override val isExpect: Boolean
    override val isActual: Boolean
    override val isOverride: Boolean
    override val isOperator: Boolean
    override val isInfix: Boolean
    override val isInline: Boolean
    override val isTailRec: Boolean
    override val isExternal: Boolean
    override val isConst: Boolean
    override val isLateInit: Boolean
    override val isInner: Boolean
    override val isCompanion: Boolean
    override val isData: Boolean
    override val isSuspend: Boolean
    override val isStatic: Boolean
    override val isFromSealedClass: Boolean
    override val isFromEnumClass: Boolean
    override val isFun: Boolean
    val effectiveVisibility: EffectiveVisibility

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> accept(visitor: VT, data: D): R = visitor.visitResolvedDeclarationStatus(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D, @Monomorphic TT: FirTransformer<D>> transform(transformer: TT, data: D): E = 
        transformer.transformResolvedDeclarationStatus(this, data) as E
}
