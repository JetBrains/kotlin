/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.FirUserType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.name.Name

class FirUserTypeImpl(
    session: FirSession,
    psi: PsiElement?,
    isNullable: Boolean,
    override val name: Name
) : FirAbstractAnnotatedType(session, psi, isNullable), FirUserType {
    override val arguments = mutableListOf<FirTypeProjection>()
}