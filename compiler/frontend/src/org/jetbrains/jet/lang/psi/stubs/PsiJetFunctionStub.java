package org.jetbrains.jet.lang.psi.stubs;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFunction;

/**
 * @author Nikolay Krasko
 */
public interface PsiJetFunctionStub <T extends JetFunction> extends StubElement<T> {
    @Nullable
    String getName();

    boolean isDeclaration();

    @NotNull
    String[] getAnnotations();

    @NotNull
    String getReturnTypeText();
}
