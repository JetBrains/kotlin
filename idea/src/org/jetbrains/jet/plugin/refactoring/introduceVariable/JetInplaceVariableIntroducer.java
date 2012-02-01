package org.jetbrains.jet.plugin.refactoring.introduceVariable;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;

/**
 * User: Alefas
 * Date: 01.02.12
 */
public class JetInplaceVariableIntroducer extends InplaceVariableIntroducer<JetExpression> {

    private boolean myReplaceOccurrence;
    private JetProperty myProperty;

    public JetInplaceVariableIntroducer(PsiNamedElement elementToRename, Editor editor, Project project,
                                        String title, JetExpression[] occurrences,
                                        @Nullable JetExpression expr, boolean replaceOccurrence,
                                        JetProperty property) {
        super(elementToRename, editor, project, title, occurrences, expr);
        this.myReplaceOccurrence = replaceOccurrence;
        myProperty = property;
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        if (!myReplaceOccurrence || myExprMarker == null) {
            myEditor.getCaretModel().moveToOffset(myProperty.getTextRange().getEndOffset());
        } else {
            int startOffset = myExprMarker.getStartOffset();
            PsiFile file = myProperty.getContainingFile();
            PsiElement elementAt = file.findElementAt(startOffset);
            if (elementAt != null) {
                myEditor.getCaretModel().moveToOffset(elementAt.getTextRange().getEndOffset());
            } else {
                myEditor.getCaretModel().moveToOffset(myExprMarker.getEndOffset());
            }
        }
    }
}
