/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import com.intellij.psi.StubBasedPsiElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/**
 * val lambda = fun(x: Int, _: String, `_`: Double) = 1
 *
 * This property is true only for second value parameter in the example above
 */
val KtNamedDeclaration.isSingleUnderscore: Boolean
    get() {
        // We don't want to call 'getNameIdentifier' on stubs to prevent text building
        // But it's fine because one-underscore names are prohibited for non-local declarations (only lambda parameters, local vars are allowed)
        if (this is StubBasedPsiElement<*> && this.stub != null) return false
        return nameIdentifier?.text == "_"
    }
