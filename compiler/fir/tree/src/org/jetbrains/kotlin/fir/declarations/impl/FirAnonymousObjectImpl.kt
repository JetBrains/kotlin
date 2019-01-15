/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirAnonymousObjectImpl(
    session: FirSession,
    psi: PsiElement?
) : FirAbstractAnnotatedDeclaration(session, psi), FirAnonymousObject, FirModifiableClass {
    override val superTypes = mutableListOf<FirType>()

    override val declarations = mutableListOf<FirDeclaration>()

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R {
        return super<FirAnonymousObject>.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        superTypes.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)

        return super<FirAbstractAnnotatedDeclaration>.transformChildren(transformer, data)
    }
}