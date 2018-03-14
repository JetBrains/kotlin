/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractNamedAnnotatedDeclaration(
    session: FirSession,
    psi: PsiElement?,
    final override val name: Name
) : FirAbstractAnnotatedDeclaration(session, psi), FirNamedDeclaration