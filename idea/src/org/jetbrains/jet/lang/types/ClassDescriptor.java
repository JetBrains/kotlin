package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.List;

/**
 * @author abreslav
 */
public interface ClassDescriptor extends DeclarationDescriptor {
    @NotNull
    TypeConstructor getTypeConstructor();

    @NotNull
    JetScope getMemberScope(List<TypeProjection> typeArguments);

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}
