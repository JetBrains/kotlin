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
        public ClassDescriptor getClassDescriptor(@NotNull Type contextType, String name) {
            return null;
        }

        @NotNull
        @Override
        public Collection<MethodDescriptor> getMethods(Type contextType, String name) {
            return Collections.emptyList();
        }

        @Override
        public PropertyDescriptor getProperty(Type contextType, String name) {
            return null;
        }

        @Override
        public ExtensionDescriptor getExtension(Type contextType, String name) {
            return null;
        }
    };

    @Nullable
    ClassDescriptor getClassDescriptor(@NotNull Type contextType, String name);

    @NotNull
    Collection<MethodDescriptor> getMethods(Type contextType, String name);

    @Nullable
    PropertyDescriptor getProperty(Type contextType, String name);

    @Nullable
    ExtensionDescriptor getExtension(Type contextType, String name);
}
