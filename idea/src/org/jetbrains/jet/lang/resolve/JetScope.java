package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.modules.MemberDomain;
import org.jetbrains.jet.lang.modules.NamespaceDomain;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public interface JetScope extends NamespaceDomain, MemberDomain {
    JetScope EMPTY = new JetScopeImpl() {};

    @Nullable
    TypeParameterDescriptor getTypeParameterDescriptor(String name);

    @NotNull
    Type getThisType();
}
