package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperClass;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.List;

/**
 * @author svtk
 */
public class ChangeToInvocationFix extends JetIntentionAction<JetDelegatorToSuperClass> {

    public ChangeToInvocationFix(@NotNull JetDelegatorToSuperClass element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.constructor.invocation");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.constructor.invocation");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetDelegatorToSuperClass delegator = (JetDelegatorToSuperClass) element.copy();
        JetClass aClass = JetPsiFactory.createClass(project, "class A : " + delegator.getText() + "()");
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        assert delegationSpecifiers.size() == 1;
        JetDelegationSpecifier specifier = delegationSpecifiers.iterator().next();
        element.replace(specifier);
    }

    public static JetIntentionActionFactory<JetDelegatorToSuperClass> createFactory() {
        return new JetIntentionActionFactory<JetDelegatorToSuperClass>() {
            @Override
            public JetIntentionAction<JetDelegatorToSuperClass> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetDelegatorToSuperClass;
                return new ChangeToInvocationFix((JetDelegatorToSuperClass) diagnostic.getPsiElement());
            }
        };
    }
}
