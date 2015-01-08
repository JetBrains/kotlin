/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;

public class CompilationException extends RuntimeException {
    private final PsiElement element;

    public CompilationException(@NotNull String message, @Nullable Throwable cause, @NotNull PsiElement element) {
        super(getMessage(message, cause, element), cause);
        this.element = element;
    }

    @NotNull
    public PsiElement getElement() {
        return element;
    }


    private static String where(@NotNull Throwable cause) {
        StackTraceElement[] stackTrace = cause.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            return stackTrace[0].getFileName() + ":" + stackTrace[0].getLineNumber();
        }
        return "unknown";
    }

    public static String getMessage(@NotNull final String message, @Nullable final Throwable cause, @NotNull final PsiElement element) {
        return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override
            public String compute() {
                StringBuilder result =
                        new StringBuilder("Back-end (JVM) Internal error: ").append(message).append("\n");
                if (cause != null) {
                    String causeMessage = cause.getMessage();
                    result.append("Cause: ").append(causeMessage == null ? cause.toString() : causeMessage).append("\n");
                }
                result.append("File being compiled and position: ").append(DiagnosticUtils.atLocation(element)).append("\n");
                result.append("PsiElement: ").append(element.getText()).append("\n");
                if (cause != null) {
                    result.append("The root cause was thrown at: ").append(where(cause));
                }

                return result.toString();
            }
        });
    }
}
