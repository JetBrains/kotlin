/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.lexer.KtTokens

class TypeModifier(
    session: FirSession,
    psi: PsiElement? = null,

    private var suspendModifier: SuspendModifier? = null
) : FirAbstractAnnotatedElement(session, psi) {
    fun addModifier(modifier: LighterASTNode) {
        if (modifier.toString() == KtTokens.SUSPEND_KEYWORD.value) {
            this.suspendModifier = SuspendModifier.SUSPEND
        }
    }

    fun hasSuspend(): Boolean {
        return suspendModifier == SuspendModifier.SUSPEND
    }
}

enum class SuspendModifier {
    SUSPEND
}