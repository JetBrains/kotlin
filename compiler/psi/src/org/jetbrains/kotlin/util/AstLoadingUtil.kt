/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiFile
import com.intellij.util.AstLoadingFilter

/**
 * Absent in 181. Methods were renamed in 183.
 *
 * BUNCH: 182
 */
@Suppress("IncompatibleAPI", "MissingRecentApi")
object AstLoadingFilter {
    @JvmStatic
    fun <T, E : Throwable> forceAllowTreeLoading(psiFile: PsiFile, computable: ThrowableComputable<out T, E>): T {
        return AstLoadingFilter.forceAllowTreeLoading(psiFile, computable)
    }
}