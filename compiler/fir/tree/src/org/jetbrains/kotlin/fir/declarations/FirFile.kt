/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirFile : FirAnnotationContainer, FirDeclaration {
    override val psi: PsiElement?
    override val annotations: List<FirAnnotationCall>
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    val imports: List<FirImport>
    val declarations: List<FirDeclaration>
    val name: String
    val packageFqName: FqName

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitFile(this, data)
}
