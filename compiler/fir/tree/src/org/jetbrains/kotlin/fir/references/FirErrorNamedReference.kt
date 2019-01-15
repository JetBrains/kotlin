/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.name.Name

class FirErrorNamedReference(
    session: FirSession,
    psi: PsiElement?,
    val errorReason: String
) : FirAbstractElement(session, psi), FirNamedReference {
    override val name: Name = Name.special("<$errorReason>")
}