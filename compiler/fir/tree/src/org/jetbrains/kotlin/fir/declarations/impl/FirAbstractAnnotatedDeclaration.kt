/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement

abstract class FirAbstractAnnotatedDeclaration(
    final override val session: FirSession,
    psi: PsiElement?
) : FirAbstractAnnotatedElement(psi), FirDeclaration {
    override var resolvePhase = FirResolvePhase.RAW_FIR
}