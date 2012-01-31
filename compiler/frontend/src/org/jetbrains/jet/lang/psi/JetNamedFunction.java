package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetNamedFunction extends JetFunction implements StubBasedPsiElement<PsiJetFunctionStub<?>> {
    public JetNamedFunction(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitNamedFunction(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitNamedFunction(this, data);
    }

    public boolean hasTypeParameterListBeforeFunctionName() {
        JetTypeParameterList typeParameterList = getTypeParameterList();
        if (typeParameterList == null) {
            return false;
        }
        PsiElement nameIdentifier = getNameIdentifier();
        if (nameIdentifier == null) {
            return false;
        }
        return nameIdentifier.getTextOffset() > typeParameterList.getTextOffset();
    }

    @Override
    public boolean hasBlockBody() {
        return findChildByType(JetTokens.EQ) == null;
    }

    @NotNull
    public JetElement getStartOfSignatureElement() {
        return this;
    }

    @NotNull
    public JetElement getEndOfSignatureElement() {
        JetElement r = getReturnTypeRef();
        if (r != null) {
            return r;
        }
        
        r = getValueParameterList();
        if (r != null) {
            return r;
        }

        // otherwise it is an error

        return this;
    }

    /**
     * Returns full qualified name for function "package_fqn.function_name"
     * Not null for top level functions.
     * @return
     */
    @Nullable
    public String getQualifiedName() {
        final PsiJetFunctionStub stub = getStub();
        if (stub != null) {
            // TODO (stubs): return stub.getQualifiedName();
        }

        PsiElement parent = getParent();
        if (parent instanceof JetFile) {
            JetFile jetFile = (JetFile) parent;
            final String fileFQN = JetPsiUtil.getFQName(jetFile);
            if (!fileFQN.isEmpty()) {
                return fileFQN + "." + getName();
            }

            return getName();
        }

        return null;
    }

    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.FUNCTION;
    }
    
    @Override
    public PsiJetFunctionStub<?> getStub() {
        // TODO (stubs)
        return null;
    }
}
