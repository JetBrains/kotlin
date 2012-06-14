package org.jetbrains.jet.lang.psi;

import com.intellij.psi.NavigatablePsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Nikolay Krasko
 */
public interface JetElement extends NavigatablePsiElement {
    <D> void acceptChildren(@NotNull JetTreeVisitor<D> visitor, D data);

    void accept(@NotNull JetVisitorVoid visitor);

    <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data);
}
