package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Nikolay Krasko
 */
public interface JetTypeParameterListOwner extends JetNamedDeclaration {
    @Nullable
    JetTypeParameterList getTypeParameterList();

    @Nullable
    JetTypeConstraintList getTypeConstraintList();

    @NotNull
    List<JetTypeConstraint> getTypeConstraints();

    @NotNull
    List<JetTypeParameter> getTypeParameters();
}
