/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * val lambda = fun(x: Int, _: String, `_`: Double) = 1
 *
 * This property is true only for second value parameter in the example above
 */
val KtNamedDeclaration.isSingleUnderscore: Boolean
    get() {
        // The name goes first: it is read from the stub, while the name identifier forces AST loading for stubs.
        // The name identifier is needed only to tell `_` from `` `_` ``, which has the same name
        if (name != "_") return false
        return nameIdentifier?.textMatches("_") == true
    }
