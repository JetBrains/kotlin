package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public abstract class JetNamedDeclaration extends JetDeclaration implements PsiNameIdentifierOwner {
    public JetNamedDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getText() : null;
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return getNameIdentifier().replace(JetChangeUtil.createNameIdentifier(getProject(), name));
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }
}
