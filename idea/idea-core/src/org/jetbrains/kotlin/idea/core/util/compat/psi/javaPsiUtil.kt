/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.util.compat.psi

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil

/**
 * Method is absent in 172.
 * BUNCH: 173
 */
@Suppress("IncompatibleAPI")
fun canBeOverridden(method: PsiMethod): Boolean {
    return PsiUtil.canBeOverridden(method)
}