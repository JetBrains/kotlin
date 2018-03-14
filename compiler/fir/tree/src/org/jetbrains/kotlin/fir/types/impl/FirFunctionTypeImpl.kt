/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.FirFunctionType
import org.jetbrains.kotlin.fir.types.FirType

class FirFunctionTypeImpl(
    session: FirSession,
    psi: PsiElement?,
    isNullable: Boolean,
    override val receiverType: FirType?,
    override val returnType: FirType
) : FirAbstractAnnotatedType(session, psi, isNullable), FirFunctionType {
    override val valueParameters = mutableListOf<FirValueParameter>()
}