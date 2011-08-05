package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ErrorValue implements CompileTimeConstant<Void> {
    private final String message;

    public ErrorValue(@NotNull String message) {
        this.message = message;
    }

    @Override
    @Deprecated // Should not be called, for this is not a real value, but a indication of an error
    public Void getValue() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return ErrorUtils.createErrorType(message);
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitErrorValue(this, data);
    }

    @NotNull
    public String getMessage() {
        return message;
    }
}
