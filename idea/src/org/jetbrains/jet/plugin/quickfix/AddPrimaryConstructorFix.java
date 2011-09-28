package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetPsiFactory;

/**
 * @author svtk
 */
public class AddPrimaryConstructorFix extends JetIntentionAction<JetClass> {

    public AddPrimaryConstructorFix(@NotNull JetClass element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return "Add primary constructor to " + element.getName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Add primary constructor";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetClass newClass = (JetClass) element.copy();
        assert !newClass.hasPrimaryConstructor();
        PsiElement primaryConstructor = JetPsiFactory.createPrimaryConstructor(project);
        newClass.addAfter(primaryConstructor, newClass.getNameIdentifier());
        element.replace(newClass);
    }

    public static JetIntentionActionFactory<JetClass> createFactory() {
        return new JetIntentionActionFactory<JetClass>() {
            @Override
            public JetIntentionAction<JetClass> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetClass;
                return new AddPrimaryConstructorFix((JetClass) diagnostic.getPsiElement());
            }
        };
    }
}
