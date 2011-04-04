package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface JetDeclarationWithBody {

    @Nullable
    JetExpression getBodyExpression();

    @Nullable
    String getName();
}

