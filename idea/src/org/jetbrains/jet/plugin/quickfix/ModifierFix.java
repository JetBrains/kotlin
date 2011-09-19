package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetKeywordToken;

/**
 * @author svtk
 */
public abstract class ModifierFix extends IntentionActionForPsiElement<JetModifierListOwner> {
    protected final JetKeywordToken modifier;

    protected ModifierFix(@NotNull JetModifierListOwner element, JetKeywordToken modifier) {
        super(element);
        this.modifier = modifier;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

}
