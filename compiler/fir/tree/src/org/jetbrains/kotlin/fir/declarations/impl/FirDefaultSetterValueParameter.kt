/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.name.Name

class FirDefaultSetterValueParameter(
    session: FirSession,
    psi: PsiElement?,
    override val returnType: FirType
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirValueParameter {
    override val isCrossinline = false

    override val isNoinline = false

    override val isVararg = false

    override val defaultValue: FirExpression? = null

    companion object {
        val name = Name.identifier("value")
    }
}