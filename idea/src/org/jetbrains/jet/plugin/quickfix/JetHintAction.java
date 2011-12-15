package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInspection.HintAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * A fix with the user information hint.
 *
 * @author Nikolay Krasko
 */
public abstract class JetHintAction<T extends PsiElement> extends JetIntentionAction<T> implements HintAction {

    public JetHintAction(@NotNull T element) {
        super(element);
    }

    protected boolean isCaretNearRef(Editor editor, T ref) {
        TextRange range = element.getTextRange();
        int offset = editor.getCaretModel().getOffset();

        return offset == range.getEndOffset();
    }
}
