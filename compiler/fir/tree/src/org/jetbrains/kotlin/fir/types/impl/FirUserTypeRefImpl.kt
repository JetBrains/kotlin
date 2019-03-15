/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

open class FirUserTypeRefImpl(
    session: FirSession,
    psi: PsiElement?,
    isMarkedNullable: Boolean
) : FirAbstractAnnotatedTypeRef(session, psi, isMarkedNullable), FirUserTypeRef {
    override val qualifier = mutableListOf<FirQualifierPart>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        for (part in qualifier) {
            (part.typeArguments as MutableList<FirTypeProjection>).transformInplace(transformer, data)
        }

        return super<FirAbstractAnnotatedTypeRef>.transformChildren(transformer, data)
    }
}