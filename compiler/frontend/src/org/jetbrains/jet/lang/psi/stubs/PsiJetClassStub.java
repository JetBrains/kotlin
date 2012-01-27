package org.jetbrains.jet.lang.psi.stubs;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;

/**
 * @author Nikolay Krasko
 */
public interface PsiJetClassStub<T extends JetClass> extends StubElement<T> {
    @NonNls
    @Nullable
    String getQualifiedName();

    @Nullable
    String getName();

    boolean isDeprecated();
    boolean hasDeprecatedAnnotation();
}
