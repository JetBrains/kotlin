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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameter;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;

import java.util.Arrays;

/**
* @author svtk
*/
public abstract class JetIntentionAction<T extends PsiElement> implements IntentionAction {
    protected @NotNull T element;

    public JetIntentionAction(@NotNull T element) {
        this.element = element;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid() && file.getManager().isInProject(file);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static DiagnosticWithParameters<PsiElement> assertAndCastToDiagnosticWithParameters(Diagnostic diagnostic, DiagnosticParameter... parameters) {
        assert diagnostic instanceof DiagnosticWithParameters :
                "For this type of quick fix diagnostic with additional " +
                (parameters.length == 1 ? "parameter '" + parameters[0] + "'" : "parameters " + Arrays.asList(parameters)) + " is expected";

        for (DiagnosticParameter parameter : parameters) {
            assert ((DiagnosticWithParameters) diagnostic).hasParameter(parameter) :
                    "For this type of quick fix diagnostic with additional parameter '" + parameter + "' is expected";
        }
        return (DiagnosticWithParameters<PsiElement>) diagnostic;
    }
}
