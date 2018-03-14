/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.name.Name

class FirAbstractNamedDeclaration(
    override val session: FirSession,
    override val psi: PsiElement?,
    override val name: Name
) : FirNamedDeclaration, FirAnnotationContainer {
    override val annotations = mutableListOf<FirAnnotationCall>()
}