/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.PsiClass

interface KtCodeFragmentBase : JavaCodeFragment {
    override fun importClass(aClass: PsiClass): Boolean {
        return true
    }
}