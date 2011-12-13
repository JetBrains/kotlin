package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

import java.text.MessageFormat;

/**
 * @author abreslav
 */
public enum ConstraintType implements Comparable<ConstraintType> {
    // The order of these constants DOES matter
    // they are compared according to ordinal() values
    // First element has the highest priority
    RECEIVER("{0} is not a subtype of the expected receiver type {1}"),
    VALUE_ARGUMENT("Type mismatch: argument type is {0}, but {1} was expected"),
    EXPECTED_TYPE("Resulting type is {0} but {1} was expected"),
    PARAMETER_BOUND("Type parameter bound is not satisfied: {0} is not a subtype of {1}");

    private final String errorMessageTemplate; // {0} is subtype, {1} is supertye

    private ConstraintType(@NotNull String errorMessageTemplate) {
        this.errorMessageTemplate = errorMessageTemplate;
    }

    @NotNull
    public SubtypingConstraint assertSubtyping(@NotNull JetType subtype, @NotNull JetType supertype) {
        return new SubtypingConstraint(this, subtype, supertype);
    }

    @NotNull
    public String makeErrorMessage(@NotNull SubtypingConstraint constraint) {
        return MessageFormat.format(errorMessageTemplate, constraint.getSubtype(), constraint.getSupertype());
    }
}
