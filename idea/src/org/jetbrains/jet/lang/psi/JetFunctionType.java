package org.jetbrains.jet.lang.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetFunctionType extends JetTypeElement {
    public JetFunctionType(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        ArrayList<JetTypeReference> result = Lists.newArrayList();
        JetTypeReference receiverTypeRef = getReceiverTypeRef();
        if (receiverTypeRef != null) {
            result.add(receiverTypeRef);
        }
        for (JetParameter jetParameter : getParameters()) {
            result.add(jetParameter.getTypeReference());
        }
        JetTypeReference returnTypeRef = getReturnTypeRef();
        if (returnTypeRef != null) {
            result.add(returnTypeRef);
        }
        return result;
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitFunctionType(this);
    }

    @Nullable
    public JetParameterList getParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getParameters() {
        JetParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Nullable
    public JetTypeReference getReceiverTypeRef() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.LPAR || tt == JetTokens.COLON) break;
            if (child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @Nullable
    public JetTypeReference getReturnTypeRef() {
        boolean colonPassed = false;
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.COLON) {
                colonPassed = true;
            }
            if (colonPassed && child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }
}
