package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public interface JetCall extends PsiElement {
    @Nullable
    JetArgumentList getValueArgumentList();

    @NotNull
    List<JetArgument> getValueArguments();

    @NotNull
    List<JetExpression> getFunctionLiteralArguments();

    @NotNull
    List<JetTypeProjection> getTypeArguments();

    @NotNull
    JetElement asElement();
}
