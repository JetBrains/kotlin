/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirDefaultPropertyAccessor(
    final override val session: FirSession,
    final override val psi: PsiElement?,
    final override val isGetter: Boolean
) : FirPropertyAccessor {
    final override val visibility =
        Visibilities.UNKNOWN

    final override val body: FirBody? =
        null

    final override val annotations: List<FirAnnotationCall>
        get() = emptyList()
}

class FirDefaultPropertyGetter(
    session: FirSession,
    psi: PsiElement?,
    propertyType: FirType
) : FirDefaultPropertyAccessor(session, psi, isGetter = true) {
    override val valueParameters: List<FirValueParameter> = emptyList()

    override var returnType: FirType = propertyType

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnType = returnType.transformSingle(transformer, data)

        return this
    }
}

class FirDefaultPropertySetter(
    session: FirSession,
    psi: PsiElement?,
    propertyType: FirType
) : FirDefaultPropertyAccessor(session, psi, isGetter = false) {
    override val valueParameters = mutableListOf(FirDefaultSetterValueParameter(session, psi, propertyType))

    override var returnType: FirType = FirImplicitUnitType(session, psi)

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        valueParameters.transformInplace(transformer, data)
        returnType = returnType.transformSingle(transformer, data)

        return this
    }
}