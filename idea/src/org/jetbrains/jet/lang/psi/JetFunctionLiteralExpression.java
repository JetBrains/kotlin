package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetFunctionLiteralExpression extends JetExpression implements JetDeclarationWithBody {
    public JetFunctionLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitFunctionLiteralExpression(this);
    }

    @NotNull
    public JetFunctionLiteral getFunctionLiteral() {
        return (JetFunctionLiteral) findChildByType(JetNodeTypes.FUNCTION_LITERAL);
    }

//    //TODO: Due to the lack of multiple implementation inheritance these getters are copied from JetNamedFunction
//    @Nullable
//    public JetParameterList getValueParameterList() {
//        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
//    }
//
//    @NotNull
//    public List<JetParameter> getValueParameters() {
//        JetParameterList list = getValueParameterList();
//        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
//    }
//
//    @NotNull
//    public ASTNode getBodyNode() {
//        ASTNode answer = getNode().findChildByType(JetNodeTypes.BODY);
//        assert answer != null;
//        return answer;
//    }
//
//    @NotNull
//    public List<JetElement> getBody() {
//        return PsiTreeUtil.getChildrenOfTypeAsList(getBodyNode().getPsi(), JetElement.class);
//    }
//
//    @Nullable
//    public JetTypeReference getReceiverTypeRef() {
//        PsiElement child = getFirstChild();
//        while (child != null) {
//            IElementType tt = child.getNode().getElementType();
//            if (tt == JetTokens.LPAR || tt == JetTokens.COLON) break;
//            if (child instanceof JetTypeReference) {
//                return (JetTypeReference) child;
//            }
//            child = child.getNextSibling();
//        }
//
//        return null;
//    }
//
//    @Nullable
//    public JetTypeReference getReturnTypeRef() {
//        boolean colonPassed = false;
//        PsiElement child = getFirstChild();
//        while (child != null) {
//            IElementType tt = child.getNode().getElementType();
//            if (tt == JetTokens.COLON) {
//                colonPassed = true;
//            }
//            if (colonPassed && child instanceof JetTypeReference) {
//                return (JetTypeReference) child;
//            }
//            child = child.getNextSibling();
//        }
//
//        return null;
//    }

    @Override
    public JetExpression getBodyExpression() {
        return getFunctionLiteral().getBodyExpression();
    }

    @Override
    public boolean hasBlockBody() {
        return getFunctionLiteral().hasBlockBody();
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }
}
