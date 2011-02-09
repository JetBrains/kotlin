package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ErrorType {
    private static final TypeMemberDomain ERROR_DOMAIN = new TypeMemberDomain() {
        @Override
        public ClassDescriptor getClassDescriptor(@NotNull Type contextType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public Collection<MethodDescriptor> getMethods(Type contextType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public PropertyDescriptor getProperty(Type contextType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public ExtensionDescriptor getExtension(Type contextType, String name) {
            throw new UnsupportedOperationException(); // TODO
        }
    };

    private ErrorType() {}

    public static Type createErrorType(String debugMessage) {
        return createErrorType(debugMessage, ERROR_DOMAIN);
    }

    private static Type createErrorType(String debugMessage, TypeMemberDomain memberDomain) {
        return new ErrorTypeImpl(new TypeConstructor(Collections.<Attribute>emptyList(), false, "[ERROR : " + debugMessage + "]", Collections.<TypeParameterDescriptor>emptyList(), Collections.<Type>emptyList()), memberDomain);
    }

    public static Type createWrongVarianceErrorType(TypeProjection value) {
        return createErrorType(value + " is not allowed here]", value.getType().getMemberDomain());
    }

    public static boolean isErrorType(Type type) {
        return type instanceof ErrorTypeImpl;
    }

    private static class ErrorTypeImpl implements Type {

        private final TypeConstructor constructor;
        private final TypeMemberDomain memberDomain;

        private ErrorTypeImpl(TypeConstructor constructor, TypeMemberDomain memberDomain) {
            this.constructor = constructor;
            this.memberDomain = memberDomain;
        }

        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            return constructor;
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            return Collections.emptyList();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @NotNull
        @Override
        public TypeMemberDomain getMemberDomain() {
            return memberDomain;
        }

        @Override
        public List<Attribute> getAttributes() {
            return Collections.emptyList();
        }
    }
}
