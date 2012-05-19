/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;

/**
* @author alex.tkachman
* @author abreslav
*/
public class CompilationException extends RuntimeException {
    private final PsiElement element;

    CompilationException(@NotNull String message, @Nullable Throwable cause, @NotNull PsiElement element) {
        super(message, cause);
        this.element = element;
    }

    @NotNull
    public PsiElement getElement() {
        return element;
    }


    private String where() {
        Throwable cause = getCause();
        Throwable throwable = cause != null ? cause : this;
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            return stackTrace[0].getFileName() + ":" + stackTrace[0].getLineNumber();
        }
        return "unknown";
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder("Back-end (JVM) Internal error: ").append(super.getMessage()).append("\n");
        Throwable cause = getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            message.append("Cause: ").append(causeMessage == null ? cause.toString() : causeMessage).append("\n");
        }
        message.append("File being compiled and position: ").append(DiagnosticUtils.atLocation(element)).append("\n");
        message.append("The root cause was thrown at: ").append(where());

        return message.toString();
    }
}
