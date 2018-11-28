/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

open class FirConstructorImpl(
    session: FirSession,
    psi: PsiElement?,
    visibility: Visibility,
    isExpect: Boolean,
    isActual: Boolean,
    delegatedSelfType: FirType,
    final override var delegatedConstructor: FirDelegatedConstructorCall?,
    override val body: FirBody?
) : FirAbstractCallableMember(
    session, psi, NAME, visibility, Modality.FINAL,
    isExpect, isActual, isOverride = false, receiverType = null, returnType = delegatedSelfType
), FirConstructor {
    override val valueParameters = mutableListOf<FirValueParameter>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        valueParameters.transformInplace(transformer, data)
        delegatedConstructor?.transformSingle(transformer, data)

        return super<FirAbstractCallableMember>.transformChildren(transformer, data)
    }

    companion object {
        val NAME = Name.special("<init>")
    }
}