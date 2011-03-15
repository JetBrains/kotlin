package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ErrorType {
    private static final JetScope ERROR_SCOPE = new JetScope() {
        @Override
        public ClassDescriptor getClass(@NotNull String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public PropertyDescriptor getProperty(@NotNull String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public ExtensionDescriptor getExtension(@NotNull String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public NamespaceDescriptor getNamespace(@NotNull String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public JetType getThisType() {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public FunctionGroup getFunctionGroup(@NotNull String name) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            throw new UnsupportedOperationException(); // TODO
        }

    };

    private ErrorType() {}

    public static JetType createErrorType(String debugMessage) {
        return createErrorType(debugMessage, ERROR_SCOPE);
    }

    private static JetType createErrorType(String debugMessage, JetScope memberScope) {
        return new ErrorTypeImpl(new TypeConstructor(Collections.<Attribute>emptyList(), false, "[ERROR : " + debugMessage + "]", Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptyList()), memberScope);
    }

    public static JetType createWrongVarianceErrorType(TypeProjection value) {
        return createErrorType(value + " is not allowed here]", value.getType().getMemberScope());
    }

    public static boolean isErrorType(JetType type) {
        return type instanceof ErrorTypeImpl;
    }

    private static class ErrorTypeImpl implements JetType {

        private final TypeConstructor constructor;
        private final JetScope memberScope;

        private ErrorTypeImpl(TypeConstructor constructor, JetScope memberScope) {
            this.constructor = constructor;
            this.memberScope = memberScope;
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
        public JetScope getMemberScope() {
            return memberScope;
        }

        @Override
        public List<Attribute> getAttributes() {
            return Collections.emptyList();
        }
    }
}
