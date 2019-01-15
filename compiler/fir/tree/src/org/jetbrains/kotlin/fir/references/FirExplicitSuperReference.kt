/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirExplicitSuperReference(
    session: FirSession,
    psi: PsiElement?,
    override var superType: FirType
) : FirAbstractElement(session, psi), FirSuperReference {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        superType = superType.transformSingle(transformer, data)
        return this
    }
}