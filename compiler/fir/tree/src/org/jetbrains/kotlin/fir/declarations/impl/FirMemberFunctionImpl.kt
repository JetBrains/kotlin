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
import org.jetbrains.kotlin.fir.declarations.FirMemberPlatformStatus
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirMemberFunctionImpl(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    platformStatus: FirMemberPlatformStatus,
    isOverride: Boolean,
    override val isOperator: Boolean,
    override val isInfix: Boolean,
    override val isInline: Boolean,
    override val isTailRec: Boolean,
    override val isExternal: Boolean,
    receiverType: FirType?,
    returnType: FirType,
    override val body: FirBody?
) : FirAbstractCallableMember(
    session, psi, name, visibility, modality,
    platformStatus, isOverride, receiverType, returnType
), FirNamedFunction {
    override val valueParameters = mutableListOf<FirValueParameter>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        valueParameters.transformInplace(transformer, data)

        return super<FirAbstractCallableMember>.transformChildren(transformer, data)
    }
}