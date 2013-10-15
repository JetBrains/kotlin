package org.jetbrains.jet.lang.descriptors.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;

public interface AnnotationDescriptor {
    @NotNull
    JetType getType();

    @Nullable
    CompileTimeConstant<?> getValueArgument(@NotNull ValueParameterDescriptor valueParameterDescriptor);

    @NotNull
    Map<ValueParameterDescriptor, CompileTimeConstant<?>> getAllValueArguments();
}
