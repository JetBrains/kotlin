/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirFileImpl(
    session: FirSession,
    psi: PsiElement?,
    override val name: String,
    override val packageFqName: FqName
) : FirAbstractAnnotatedDeclaration(session, psi), FirFile {
    override val imports = mutableListOf<FirImport>()

    override val declarations = mutableListOf<FirDeclaration>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        imports.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        return super<FirAbstractAnnotatedDeclaration>.transformChildren(transformer, data)
    }
}