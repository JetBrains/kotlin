package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author Nikolay Krasko
 */
public interface JetFunction extends JetTypeParameterListOwner, JetDeclarationWithBody {
    @Nullable
    JetParameterList getValueParameterList();

    @Nullable
    JetTypeReference getReceiverTypeRef();

    @Nullable
    JetTypeReference getReturnTypeRef();

    boolean isLocal();
}
