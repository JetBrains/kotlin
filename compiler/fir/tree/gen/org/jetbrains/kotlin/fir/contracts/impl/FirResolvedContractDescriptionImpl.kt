/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirResolvedContractDescriptionImpl(
    override val source: KtSourceElement?,
    override val effects: MutableList<FirEffectDeclaration>,
    override val unresolvedEffects: MutableList<FirStatement>,
) : FirResolvedContractDescription() {
    override val elementKind get() = FirElementKind.ResolvedContractDescription

    override fun replaceEffects(newEffects: List<FirEffectDeclaration>) {
        effects.clear()
        effects.addAll(newEffects)
    }

    override fun replaceUnresolvedEffects(newUnresolvedEffects: List<FirStatement>) {
        unresolvedEffects.clear()
        unresolvedEffects.addAll(newUnresolvedEffects)
    }
}
