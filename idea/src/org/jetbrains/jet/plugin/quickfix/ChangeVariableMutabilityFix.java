package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameter;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.Arrays;

/**
 * @author svtk
 */
public class ChangeVariableMutabilityFix extends JetIntentionAction<JetProperty> {
    public ChangeVariableMutabilityFix(@NotNull JetProperty element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return element.isVar() ? JetBundle.message("make.variable.immutable") : JetBundle.message("make.variable.mutable");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.variable.mutability.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetProperty newElement = (JetProperty) element.copy();
        if (newElement.isVar()) {
            PsiElement varElement = newElement.getNode().findChildByType(JetTokens.VAR_KEYWORD).getPsi();

            JetProperty valProperty = JetPsiFactory.createProperty(project, "x", "Any", false);
            PsiElement valElement = valProperty.getNode().findChildByType(JetTokens.VAL_KEYWORD).getPsi();
            CodeEditUtil.replaceChild(newElement.getNode(), varElement.getNode(), valElement.getNode());
        }
        else {
            PsiElement valElement = newElement.getNode().findChildByType(JetTokens.VAL_KEYWORD).getPsi();

            JetProperty varProperty = JetPsiFactory.createProperty(project, "x", "Any", true);
            PsiElement varElement = varProperty.getNode().findChildByType(JetTokens.VAR_KEYWORD).getPsi();
            CodeEditUtil.replaceChild(newElement.getNode(), valElement.getNode(), varElement.getNode());
        }
        element.replace(newElement);
    }

    public static JetIntentionActionFactory<JetProperty> createFactory() {
        return new JetIntentionActionFactory<JetProperty>() {
            @Nullable
            @Override
            public JetIntentionAction<JetProperty> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetProperty;
                return new ChangeVariableMutabilityFix((JetProperty) diagnostic.getPsiElement());
            }
        };
    }
    
    public static JetIntentionActionFactory<JetElement> createFromSimpleNameFactory() {
        return new JetIntentionActionFactory<JetElement>() {
            @Override
            public JetIntentionAction<JetElement> createAction(DiagnosticWithPsiElement diagnostic) {
                if (diagnostic instanceof DiagnosticWithParameters) {
                    DiagnosticWithParameters<PsiElement> diagnosticWithParameters = assertAndCastToDiagnosticWithParameters(diagnostic, DiagnosticParameters.PROPERTY);
                    JetProperty property = diagnosticWithParameters.getParameter(DiagnosticParameters.PROPERTY);
                    if (diagnostic.getPsiElement().getContainingFile() == property.getContainingFile()) {
                        return (JetIntentionAction) new ChangeVariableMutabilityFix(property);
                    }
                }
                return null;
            }
        };
    }
}
