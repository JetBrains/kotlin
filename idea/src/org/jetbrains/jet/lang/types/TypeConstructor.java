package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Annotated;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public interface TypeConstructor extends Annotated {
    @NotNull
    List<TypeParameterDescriptor> getParameters();

    @NotNull
    Collection<? extends JetType> getSupertypes();

    boolean isSealed();

    @Nullable
    DeclarationDescriptor getDeclarationDescriptor();
}
