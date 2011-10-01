package org.jetbrains.jet.lang.descriptors.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

/**
 * @author abreslav
 */
public class AnnotationDescriptor {
    private JetType annotationType;
    private List<CompileTimeConstant<?>> valueArguments;

    @NotNull
    public JetType getType() {
        return annotationType;
    }

    @NotNull
    public List<CompileTimeConstant<?>> getValueArguments() {
        return valueArguments;
    }

    public void setAnnotationType(@NotNull JetType annotationType) {
        this.annotationType = annotationType;
    }

    public void setValueArguments(@NotNull List<CompileTimeConstant<?>> valueArguments) {
        this.valueArguments = valueArguments;
    }
}
