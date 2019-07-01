/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils;
import org.jetbrains.kotlin.util.ExceptionUtilKt;
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments;

public class CompilationException extends KotlinExceptionWithAttachments {
    private final PsiElement element;

    public CompilationException(@NotNull String message, @Nullable Throwable cause, @Nullable PsiElement element) {
        super(ExceptionUtilKt.getExceptionMessage("Back-end (JVM)", message, cause,
                                                  element == null ? null : PsiDiagnosticUtils.atLocation(element)),
              cause);
        this.element = element;

        if (element != null) {
            withAttachment("element.kt", element.getText());
        }
    }

    @Nullable
    public PsiElement getElement() {
        return element;
    }
}
