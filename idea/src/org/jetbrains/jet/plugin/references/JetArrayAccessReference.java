package org.jetbrains.jet.plugin.references;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetContainerNode;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

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
    protected PsiElement doResolve() {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) getElement().getContainingFile());
        FunctionDescriptor getFunction = bindingContext.get(INDEXED_LVALUE_GET, expression);
        FunctionDescriptor setFunction = bindingContext.get(INDEXED_LVALUE_SET, expression);
        if (getFunction != null && setFunction != null) {
            return null; // Call doMultiResolve
        }
        return super.doResolve();
    }

    @Override
    protected ResolveResult[] doMultiResolve() {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) getElement().getContainingFile());
        FunctionDescriptor getFunction = bindingContext.get(INDEXED_LVALUE_GET, expression);
        PsiElement getFunctionElement = bindingContext.get(DESCRIPTOR_TO_DECLARATION, getFunction);
        FunctionDescriptor setFunction = bindingContext.get(INDEXED_LVALUE_SET, expression);
        PsiElement setFunctionElement = bindingContext.get(DESCRIPTOR_TO_DECLARATION, setFunction);
        return new ResolveResult[] {new PsiElementResolveResult(getFunctionElement, true), new PsiElementResolveResult(setFunctionElement, true)};
//        return super.doMultiResolve();
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
