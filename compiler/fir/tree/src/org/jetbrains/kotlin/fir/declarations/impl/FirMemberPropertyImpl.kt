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
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirMemberPropertyImpl(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    platformStatus: FirMemberPlatformStatus,
    isOverride: Boolean,
    override val isConst: Boolean,
    override val isLateInit: Boolean,
    receiverType: FirType?,
    returnType: FirType,
    override val isVar: Boolean,
    override val initializer: FirExpression?,
    override var getter: FirPropertyAccessor,
    override var setter: FirPropertyAccessor,
    override val delegate: FirExpression?
) : FirAbstractCallableMember(session, psi, name, visibility, modality, platformStatus, isOverride, receiverType, returnType),
    FirProperty {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        getter = getter.transformSingle(transformer, data)
        setter = setter.transformSingle(transformer, data)

        return super<FirAbstractCallableMember>.transformChildren(transformer, data)
    }
}