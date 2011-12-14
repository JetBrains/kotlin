package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class SubtypingConstraint {
    private final ConstraintType type;
    private final JetType subtype;
    private final JetType supertype;

    public SubtypingConstraint(@NotNull ConstraintType type, @NotNull JetType subtype, @NotNull JetType supertype) {
        this.type = type;
        this.subtype = subtype;
        this.supertype = supertype;
    }

    @NotNull
    public JetType getSubtype() {
        return subtype;
    }

    @NotNull
    public JetType getSupertype() {
        return supertype;
    }

    @NotNull
    public ConstraintType getType() {
        return type;
    }

    @NotNull
    public String getErrorMessage() {
        return type.makeErrorMessage(this);
    }

    @Override
    public String toString() {
        return getSubtype() + " :< " + getSupertype();
    }
}
