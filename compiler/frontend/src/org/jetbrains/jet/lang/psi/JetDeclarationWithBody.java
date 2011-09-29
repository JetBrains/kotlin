package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public interface JetDeclarationWithBody extends PsiElement {

    @Nullable
    JetExpression getBodyExpression();

    @Nullable
    String getName();

    boolean hasBlockBody();

    boolean hasDeclaredReturnType();

    @NotNull
    JetElement asElement();

    @NotNull
    List<JetParameter> getValueParameters();
}

