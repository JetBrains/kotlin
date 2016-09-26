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

package org.jetbrains.kotlin.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface DiagnosticSink {
    DiagnosticSink DO_NOTHING = new DiagnosticSink() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
        }

        @Override
        public boolean wantsDiagnostics() {
            return false;
        }
    };

    DiagnosticSink THROW_EXCEPTION = new DiagnosticSink() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                PsiFile psiFile = diagnostic.getPsiFile();
                List<TextRange> textRanges = diagnostic.getTextRanges();
                String diagnosticText = DefaultErrorMessages.render(diagnostic);
                throw new IllegalStateException(diagnostic.getFactory().getName() + ": " + diagnosticText + " " + DiagnosticUtils.atLocation(psiFile, textRanges.get(0)));
            }
        }

        @Override
        public boolean wantsDiagnostics() {
            return true;
        }
    };

    void report(@NotNull Diagnostic diagnostic);
    boolean wantsDiagnostics();
}
