package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface ValueArgument {
    @Nullable
    @JetElement.IfNotParsed
    JetExpression getArgumentExpression();

    @Nullable
    JetValueArgumentName getArgumentName();

    boolean isNamed();

    boolean isOut();

    boolean isRef();

    @NotNull
    PsiElement asElement();

}
