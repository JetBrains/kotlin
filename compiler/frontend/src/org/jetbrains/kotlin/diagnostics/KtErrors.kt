/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement

object KtErrors {

    val NON_LOCAL_RETURN_IN_DISABLED_INLINE by error0<PsiElement>()
    val TYPEOF_SUSPEND_TYPE by error0<PsiElement>()
    val TYPEOF_EXTENSION_FUNCTION_TYPE by error0<PsiElement>()
    val TYPEOF_ANNOTATED_TYPE by error0<PsiElement>()
    val TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND by error1<PsiElement, String>()

    val SUSPENSION_POINT_INSIDE_MONITOR by error1<PsiElement, String>()
}