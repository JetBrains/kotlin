package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.Nullable;

public interface JetFunction extends JetTypeParameterListOwner, JetDeclarationWithBody {
    @Nullable
    JetParameterList getValueParameterList();

    @Nullable
    JetTypeReference getReceiverTypeRef();

    @Nullable
    JetTypeReference getReturnTypeRef();

    boolean isLocal();
}
