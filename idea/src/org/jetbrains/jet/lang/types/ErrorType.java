package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class ErrorType {
    private static final TypeMemberDomain ERROR_DOMAIN = new TypeMemberDomain() {
        @Override
        public ClassDescriptor getClassDescriptor(@NotNull Type type, String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public Collection<MethodDescriptor> getMethods(Type receiverType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public PropertyDescriptor getProperty(Type receiverType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public ExtensionDescriptor getExtension(Type receiverType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }
    };
    private static final TypeConstructor ERROR = new TypeConstructor(Collections.<Attribute>emptyList(), false, "ERROR", Collections.<TypeParameterDescriptor>emptyList(), Collections.<Type>emptyList());

    public static Type createErrorType(String debugMessage) {
        return new TypeImpl(ERROR, ERROR_DOMAIN);
    }

    public static boolean isError(Type type) {
        return type.getConstructor() == ERROR;
    }
}
