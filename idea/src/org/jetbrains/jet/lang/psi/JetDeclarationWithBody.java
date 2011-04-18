package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface JetDeclarationWithBody {

    @Nullable
    JetExpression getBodyExpression();

    @Nullable
    String getName();

    boolean hasBlockBody();

    @NotNull
    JetElement asElement();
}

