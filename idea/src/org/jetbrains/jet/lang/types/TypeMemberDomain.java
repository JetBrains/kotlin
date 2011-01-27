package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface TypeMemberDomain {
    TypeMemberDomain EMPTY = new TypeMemberDomain() {
        @Override
        public ClassDescriptor getClassDescriptor(@NotNull Type type) {
            return null;
        }
    };

    @Nullable ClassDescriptor getClassDescriptor(@NotNull Type type);
    // ...
}
