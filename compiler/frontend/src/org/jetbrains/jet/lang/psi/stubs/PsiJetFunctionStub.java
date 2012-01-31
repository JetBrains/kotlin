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

    /**
     * Is function defined in directly in package.
     * @return
     */
    boolean isTopLevel();

    /**
     * Does function extends some type.
     */
    boolean isExtension();

    @NotNull
    String[] getAnnotations();
}
