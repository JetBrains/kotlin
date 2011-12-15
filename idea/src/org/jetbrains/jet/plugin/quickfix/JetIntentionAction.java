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
