/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.PsiClass

/**
 * The Java-interop base for [KtCodeFragment], adapting the platform's [JavaCodeFragment] contract to Kotlin.
 *
 * It exists so that Kotlin code fragments can participate in platform machinery (such as the debugger) that is defined
 * in terms of [JavaCodeFragment].
 */
interface KtCodeFragmentBase : JavaCodeFragment {
    /**
     * Accepts a class import request from the platform. Kotlin code fragments manage imports themselves, so this always
     * returns `true` without modifying the fragment.
     */
    override fun importClass(aClass: PsiClass): Boolean {
        return true
    }
}