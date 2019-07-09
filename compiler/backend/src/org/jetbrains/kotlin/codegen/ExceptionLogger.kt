/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

object ExceptionLogger {
    @JvmStatic
    fun logDescriptorNotFound(problemDescription: String, psi: PsiElement): AssertionError {
        LOG.error(problemDescription, Attachment("psi.kt", psi.getElementTextWithContext()))
        throw AssertionError(problemDescription)
    }

    private val LOG = Logger.getInstance(ExceptionLogger::class.java)
}