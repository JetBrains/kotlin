/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractCallableMember
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirJavaField(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirFieldSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    returnTypeRef: FirTypeRef,
    override val isVar: Boolean,
    isStatic: Boolean
) : FirAbstractCallableMember<FirField>(
    session, psi, name,
    visibility, modality,
    isExpect = false, isActual = false, isOverride = false,
    receiverTypeRef = null, returnTypeRef = returnTypeRef
), FirField {
    init {
        symbol.bind(this)
        resolvePhase = FirResolvePhase.DECLARATIONS
    }

    override val delegate: FirExpression?
        get() = null

    override val initializer: FirExpression?
        get() = null

    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirField>?
        get() = null

    init {
        status.isStatic = isStatic
    }

    override fun <D> transformChildrenWithoutAccessors(transformer: FirTransformer<D>, data: D) {
        transformChildren(transformer, data)
    }
}