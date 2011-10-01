package org.jetbrains.jet.plugin.references;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiReference;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetContainerNode;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.List;

/**
* @author yole
*/
class JetArrayAccessReference extends JetPsiReference implements MultiRangeReference {
    private JetArrayAccessExpression expression;

    public static PsiReference[] create(JetArrayAccessExpression expression) {
        JetContainerNode indicesNode = expression.getIndicesNode();
        return indicesNode == null ? PsiReference.EMPTY_ARRAY : new PsiReference[] { new JetArrayAccessReference(expression) };
    }

    public JetArrayAccessReference(JetArrayAccessExpression expression) {
        super(expression);
        this.expression = expression;
    }

    @Override
    public TextRange getRangeInElement() {
        return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
    }

    @Override
    public List<TextRange> getRanges() {
        List<TextRange> list = new ArrayList<TextRange>();

        JetContainerNode indices = expression.getIndicesNode();
        TextRange textRange = indices.getNode().findChildByType(JetTokens.LBRACKET).getTextRange();
        TextRange lBracketRange = textRange.shiftRight(-expression.getTextOffset());

        list.add(lBracketRange);

        ASTNode rBracket = indices.getNode().findChildByType(JetTokens.RBRACKET);
        if (rBracket != null) {
            textRange = rBracket.getTextRange();
            TextRange rBracketRange = textRange.shiftRight(-expression.getTextOffset());
            list.add(rBracketRange);
        }

        return list;
    }
}
