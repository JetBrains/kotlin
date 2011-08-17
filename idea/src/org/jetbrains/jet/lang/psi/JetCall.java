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
    JetExpression getCalleeExpression();

    @Nullable
    JetValueArgumentList getValueArgumentList();

    @NotNull
    List<JetValueArgument> getValueArguments();

    @NotNull
    List<JetExpression> getFunctionLiteralArguments();

    @NotNull
    List<JetTypeProjection> getTypeArguments();

    @NotNull
    JetElement asElement();
}
