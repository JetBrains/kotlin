package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * @author abreslav
 */
public class ErrorType {
    private static final TypeMemberDomain ERROR_DOMAIN = new TypeMemberDomain() {
        @Override
        public ClassDescriptor getClassDescriptor(@NotNull Type type) {
            throw new UnsupportedOperationException(); // TODO
        }
    };
    private static final TypeConstructor ERROR = new TypeConstructor(Collections.<Attribute>emptyList(), "ERROR", Collections.<TypeParameterDescriptor>emptyList(), Collections.<Type>emptyList());

    public static Type createErrorType(String debugMessage) {
        return new TypeImpl(ERROR, ERROR_DOMAIN);
    }

    public static boolean isError(Type type) {
        return type.getConstructor() == ERROR;
    }
}
