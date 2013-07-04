package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;

import java.util.List;

public interface AnnotationDeserializer {
    AnnotationDeserializer UNSUPPORTED = new AnnotationDeserializer() {
        @NotNull
        @Override
        public List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto) {
            return notSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadCallableAnnotations(
                @NotNull ProtoBuf.Callable callableProto
        ) {
            return notSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadSetterAnnotations(@NotNull ProtoBuf.Callable callableProto) {
            return notSupported();
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadValueParameterAnnotations(
                @NotNull ProtoBuf.Callable.ValueParameter parameterProto
        ) {
            return notSupported();
        }

        private List<AnnotationDescriptor> notSupported() {
            throw new UnsupportedOperationException("Annotations are not supported");
        }
    };

    @NotNull
    List<AnnotationDescriptor> loadClassAnnotations(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class classProto);

    @NotNull
    List<AnnotationDescriptor> loadCallableAnnotations(
            @NotNull ProtoBuf.Callable callableProto
    );

    @NotNull
    List<AnnotationDescriptor> loadValueParameterAnnotations(
            @NotNull ProtoBuf.Callable.ValueParameter parameterProto
    );

    @NotNull
    List<AnnotationDescriptor> loadSetterAnnotations(@NotNull ProtoBuf.Callable callableProto);
}
