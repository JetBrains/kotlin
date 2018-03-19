/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirUserType
import java.util.*

class FirUserTypeImpl(
    session: FirSession,
    psi: PsiElement?,
    isNullable: Boolean
) : FirAbstractAnnotatedType(session, psi, isNullable), FirUserType {
    override val qualifier = LinkedList<FirQualifierPart>()
}