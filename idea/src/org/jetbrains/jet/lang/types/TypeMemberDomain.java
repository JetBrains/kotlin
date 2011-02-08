package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public interface TypeMemberDomain {
    TypeMemberDomain EMPTY = new TypeMemberDomain() {
        @Override
        public ClassDescriptor getClassDescriptor(@NotNull Type type, String name) {
            return null;
        }

        @NotNull
        @Override
        public Collection<MethodDescriptor> getMethods(Type receiverType, String name) {
            return Collections.emptyList();
        }

        @Override
        public PropertyDescriptor getProperty(Type receiverType, String name) {
            return null;
        }

        @Override
        public ExtensionDescriptor getExtension(Type receiverType, String name) {
            return null;
        }
    };

    @Nullable
    ClassDescriptor getClassDescriptor(@NotNull Type type, String name);

    @NotNull
    Collection<MethodDescriptor> getMethods(Type receiverType, String name);

    @Nullable
    PropertyDescriptor getProperty(Type receiverType, String name);

    @Nullable
    ExtensionDescriptor getExtension(Type receiverType, String name);
}
