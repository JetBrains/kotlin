/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

class KotlinFrontEndException(message: String, cause: Throwable) : KotlinExceptionWithAttachments(message, cause) {
    constructor(
        message: String,
        cause: Throwable,
        element: PsiElement
    ) : this(getExceptionMessage("Front-end", message, cause, PsiDiagnosticUtils.atLocation(element)), cause) {
        withAttachment(
            "element.kt",
            if (element.isValid) {
                element.text
            } else {
                "PsiElement (invalid): " + element.toString()
            }
        )
    }
}
