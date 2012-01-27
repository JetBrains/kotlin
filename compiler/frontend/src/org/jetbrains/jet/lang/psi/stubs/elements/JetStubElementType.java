package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.StubPsiFactory;
import com.intellij.psi.stubs.ILightStubElementType;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Nikolay Krasko
 */
public abstract class JetStubElementType<StubT extends StubElement, PsiT extends PsiElement>
        extends ILightStubElementType<StubT, PsiT> {

    public JetStubElementType(@NotNull @NonNls String debugName, @Nullable Language language) {
        super(debugName, language);
    }



    public abstract PsiT createPsiFromAst(@NotNull ASTNode node);

    protected StubPsiFactory getPsiFactory(StubT stub) {
        return getFileStub(stub).getPsiFactory();
    }

    private PsiJavaFileStub getFileStub(StubT stub) {
        StubElement parent = stub;
        while (!(parent instanceof PsiFileStub)) {
            parent = parent.getParentStub();
        }

        return (PsiJavaFileStub)parent;
    }

    @Override
    public String getExternalId() {
        return "jet." + toString();
    }
}
