/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression

class Psi2FirMapper(val map: MutableMap<PsiElement, MutableList<FirElement>>) : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        val psi = element.psi
        if (psi != null) {
            if (map.putIfAbsent(psi, mutableListOf(element)) != null) {
                map[psi]?.add(element)
            }
        }
        element.acceptChildren(this)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
        visitElement(callableReferenceAccess.extensionReceiver)
        visitElement(callableReferenceAccess.dispatchReceiver)
        callableReferenceAccess.explicitReceiver?.let { visitElement(it) }

        val psi = (callableReferenceAccess.calleeReference.psi as? KtCallableReferenceExpression)?.children?.last()
            ?: return callableReferenceAccess.calleeReference.accept(this)
        if (map.putIfAbsent(psi, mutableListOf(callableReferenceAccess.calleeReference)) != null) {
            map[psi]?.add(callableReferenceAccess.calleeReference)
        }
    }
}