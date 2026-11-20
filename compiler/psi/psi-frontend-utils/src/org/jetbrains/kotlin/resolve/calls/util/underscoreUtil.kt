/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import com.intellij.psi.StubBasedPsiElement
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.stubs.KotlinDestructuringDeclarationEntryStub

/**
 * val lambda = fun(x: Int, _: String, `_`: Double) = 1
 *
 * This property is true only for second value parameter in the example above
 */
@OptIn(KtImplementationDetail::class)
val KtNamedDeclaration.isSingleUnderscore: Boolean
    get() {
        // The name goes first: it is read from the stub, while the name identifier forces AST loading for stubs.
        // The name identifier is needed only to tell `_` from `` `_` ``, which has the same name
        if (name != "_") return false

        // The stubs of destructuring entries keep the distinction, as script entries named `_` are common
        val entryStub = (this as? StubBasedPsiElement<*>)?.stub as? KotlinDestructuringDeclarationEntryStub
        if (entryStub != null) return entryStub.isSingleUnderscore

        return nameIdentifier?.textMatches("_") == true
    }
