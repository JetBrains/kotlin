/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirBackingFieldReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol

class FirBackingFieldReferenceImpl(
    psi: PsiElement?,
    override val coneSymbol: FirBackingFieldSymbol
) : FirAbstractElement(psi), FirBackingFieldReference