package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author svtk
 */
public class ChangeAccessorTypeFix extends JetIntentionAction<JetPropertyAccessor> {
    private final JetType type;

    public ChangeAccessorTypeFix(@NotNull JetPropertyAccessor element, JetType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        return (element.isGetter() ? "Change getter " : "Change setter parameter ") + "type to " + type;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Change accessor type";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetPropertyAccessor newElement = (JetPropertyAccessor) element.copy();
        JetTypeReference newTypeReference = JetPsiFactory.createType(project, type.toString());

        if (element.isGetter()) {
            JetTypeReference returnTypeReference = newElement.getReturnTypeReference();
            assert returnTypeReference != null;
            CodeEditUtil.replaceChild(newElement.getNode(), returnTypeReference.getNode(), newTypeReference.getNode());
            element.replace(newElement);
        }
        else {
            JetParameter parameter = newElement.getParameter();
            assert parameter != null;
            JetTypeReference typeReference = parameter.getTypeReference();
            assert typeReference != null;
            CodeEditUtil.replaceChild(parameter.getNode(), typeReference.getNode(), newTypeReference.getNode());
            element.replace(newElement);
        }
    }
    
    public static JetIntentionActionFactory<JetPropertyAccessor> createFactory() {
        return new JetIntentionActionFactory<JetPropertyAccessor>() {
            @Override
            public JetIntentionAction<JetPropertyAccessor> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetPropertyAccessor;
                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = assertAndCastToDiagnosticWithParameters(diagnostic, DiagnosticParameters.TYPE);
                JetType type = diagnosticWithParameters.getParameter(DiagnosticParameters.TYPE);
                return new ChangeAccessorTypeFix((JetPropertyAccessor) diagnostic.getPsiElement(), type);
            }
        };
    }
}
