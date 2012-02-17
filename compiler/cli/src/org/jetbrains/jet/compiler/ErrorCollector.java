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

package org.jetbrains.jet.compiler;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithTextRange;
import org.jetbrains.jet.lang.diagnostics.Severity;

import java.io.PrintStream;
import java.util.Collection;

/**
* @author alex.tkachman
*/
class ErrorCollector {
    Multimap<PsiFile,DiagnosticWithTextRange> maps = LinkedHashMultimap.<PsiFile, DiagnosticWithTextRange>create();

    boolean hasErrors;

    public ErrorCollector() {
    }

    public void report(Diagnostic diagnostic) {
        hasErrors |= diagnostic.getSeverity() == Severity.ERROR;
        if(diagnostic instanceof DiagnosticWithTextRange) {
            DiagnosticWithTextRange diagnosticWithTextRange = (DiagnosticWithTextRange) diagnostic;
            maps.put(diagnosticWithTextRange.getPsiFile(), diagnosticWithTextRange);
        }
        else {
            System.out.println(diagnostic.getSeverity().toString() + ": " + diagnostic.getMessage());
        }
    }

    void flushTo(final PrintStream out) {
        if(!maps.isEmpty()) {
            for (PsiFile psiFile : maps.keySet()) {
                String path = psiFile.getVirtualFile().getPath();
                Collection<DiagnosticWithTextRange> diagnosticWithTextRanges = maps.get(psiFile);
                for (DiagnosticWithTextRange diagnosticWithTextRange : diagnosticWithTextRanges) {
                    String position = DiagnosticUtils.formatPosition(diagnosticWithTextRange);
                    out.println(diagnosticWithTextRange.getSeverity().toString() + ": " + path + ":" + position + " " + diagnosticWithTextRange.getMessage());
                }
            }
        }
    }

}
