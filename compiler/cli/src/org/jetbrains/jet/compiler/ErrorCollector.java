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
import org.jetbrains.jet.lang.diagnostics.Severity;

import java.io.PrintStream;
import java.util.Collection;

/**
* @author alex.tkachman
*/
class ErrorCollector {
    private final Multimap<PsiFile, Diagnostic> maps = LinkedHashMultimap.create();

    private boolean hasErrors;

    public ErrorCollector() {
    }

    public void report(Diagnostic diagnostic) {
        hasErrors |= diagnostic.getSeverity() == Severity.ERROR;
        maps.put(diagnostic.getFactory().getPsiFile(diagnostic), diagnostic);
    }

    public void flushTo(final PrintStream out) {
        if(!maps.isEmpty()) {
            for (PsiFile psiFile : maps.keySet()) {
                String path = psiFile.getVirtualFile().getPath();
                Collection<Diagnostic> diagnostics = maps.get(psiFile);
                for (Diagnostic diagnostic : diagnostics) {
                    String position = DiagnosticUtils.formatPosition(diagnostic);
                    out.println(diagnostic.getSeverity().toString() + ": " + path + ":" + position + " " + diagnostic.getMessage());
                }
            }
        }
    }

    public boolean hasErrors() {
        return hasErrors;
    }
}
