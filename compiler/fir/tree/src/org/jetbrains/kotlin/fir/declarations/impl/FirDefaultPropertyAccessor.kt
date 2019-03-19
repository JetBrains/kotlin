/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirDefaultPropertyAccessor(
    session: FirSession,
    psi: PsiElement?,
    final override val isGetter: Boolean,
    visibility: Visibility
) : FirAbstractElement(session, psi), FirPropertyAccessor {
    override var status = FirDeclarationStatusImpl(
        session, visibility, Modality.FINAL
    )

    final override val body: FirBlock? =
        null

    final override val annotations: List<FirAnnotationCall>
        get() = emptyList()
}

class FirDefaultPropertyGetter(
    session: FirSession,
    psi: PsiElement?,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility
) : FirDefaultPropertyAccessor(session, psi, isGetter = true, visibility = visibility) {
    override val valueParameters: List<FirValueParameter> = emptyList()

    override var returnTypeRef: FirTypeRef = propertyTypeRef

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        status = status.transformSingle(transformer, data)

        return this
    }
}

class FirDefaultPropertySetter(
    session: FirSession,
    psi: PsiElement?,
    propertyTypeRef: FirTypeRef,
    visibility: Visibility
) : FirDefaultPropertyAccessor(session, psi, isGetter = false, visibility = visibility) {
    override val valueParameters = mutableListOf(FirDefaultSetterValueParameter(session, psi, propertyTypeRef))

    override var returnTypeRef: FirTypeRef = FirImplicitUnitTypeRef(session, psi)

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        valueParameters.transformInplace(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        status = status.transformSingle(transformer, data)

        return this
    }
}