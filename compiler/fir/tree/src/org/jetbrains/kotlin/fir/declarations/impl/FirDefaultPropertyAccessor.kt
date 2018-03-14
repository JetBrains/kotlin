/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.impl.FirUnitType

class FirDefaultPropertyAccessor(
    override val session: FirSession,
    override val psi: PsiElement?,
    override val isGetter: Boolean,
    propertyType: FirType
) : FirPropertyAccessor {
    override val visibility =
        Visibilities.UNKNOWN

    override val valueParameters: List<FirValueParameter> =
        if (isGetter) emptyList() else listOf(FirDefaultSetterValueParameter(session, psi, propertyType))

    override val returnType: FirType =
        if (isGetter) propertyType else FirUnitType(session, psi)

    override val body: FirBody? =
        null

    override val annotations: List<FirAnnotationCall>
        get() = emptyList()
}