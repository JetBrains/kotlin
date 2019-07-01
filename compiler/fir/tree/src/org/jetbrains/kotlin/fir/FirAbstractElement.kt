/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement

abstract class FirPureAbstractElement : FirElement

abstract class FirAbstractElement(
    final override val session: FirSession,
    final override val psi: PsiElement?
) : FirPureAbstractElement()