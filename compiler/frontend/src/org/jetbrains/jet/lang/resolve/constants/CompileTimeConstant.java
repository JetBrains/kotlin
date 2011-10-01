package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface CompileTimeConstant<T> {
    T getValue();

    @NotNull
    JetType getType(@NotNull JetStandardLibrary standardLibrary);

    <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data);
}
