package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;

import java.util.*;

/**
 * @author abreslav
 */
public class ErrorUtils {

    private static final ModuleDescriptor ERROR_MODULE = new ModuleDescriptor("<ERROR MODULE>");
    private static final JetScope ERROR_SCOPE = new JetScope() {

        @Override
        public ClassifierDescriptor getClassifier(@NotNull String name) {
            return ERROR_CLASS;
        }

        @Override
        public PropertyDescriptor getProperty(@NotNull String name) {
            return ERROR_PROPERTY;
        }

        @Override
        public NamespaceDescriptor getNamespace(@NotNull String name) {
            return null; // TODO : review
        }

        @NotNull
        @Override
        public JetType getThisType() {
            return createErrorType("<ERROR TYPE>");
        }

        @NotNull
        @Override
        public FunctionGroup getFunctionGroup(@NotNull String name) {
            return ERROR_FUNCTION_GROUP;
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return ERROR_MODULE;
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
            return Collections.emptyList();
        }

    };

    private static final FunctionGroup ERROR_FUNCTION_GROUP = new FunctionGroup() {
        @NotNull
        @Override
        public String getName() {
            return "<ERROR FUNCTION>";
        }

        @NotNull
        @Override
        public OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
            List<TypeParameterDescriptor> typeParameters = Collections.<TypeParameterDescriptor>emptyList();
            return OverloadResolutionResult.success(createErrorFunction(typeParameters, positionedValueArgumentTypes));
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    };

    private static final ClassDescriptor ERROR_CLASS = new ClassDescriptorImpl(ERROR_MODULE, Collections.<Attribute>emptyList(), "<ERROR CLASS>").initialize(
            true, Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptyList(), getErrorScope(), ERROR_FUNCTION_GROUP);

    private static JetScope getErrorScope() {
        return ERROR_SCOPE;
    }

    private static final JetType ERROR_PROPERTY_TYPE = createErrorType("<ERROR PROPERTY TYPE>");
    private static final PropertyDescriptor ERROR_PROPERTY = new PropertyDescriptorImpl(
            ERROR_CLASS, Collections.<Attribute>emptyList(), "<ERROR PROPERTY>", ERROR_PROPERTY_TYPE, ERROR_PROPERTY_TYPE);

    private static FunctionDescriptor createErrorFunction(List<TypeParameterDescriptor> typeParameters, List<JetType> positionedValueArgumentTypes) {
        FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(ERROR_CLASS, Collections.<Attribute>emptyList(), "<ERROR FUNCTION>");
        return functionDescriptor.initialize(
                typeParameters,
                getValueParameters(functionDescriptor, positionedValueArgumentTypes),
                createErrorType("<ERROR FUNCTION RETURN>")
        );
    }

    private static FunctionDescriptor createErrorFunction(int typeParameterCount, Map<String, JetType> valueParameters) {
        throw new UnsupportedOperationException(); // TODO
    }

    private static FunctionDescriptor createErrorFunction(int typeParameterCount, List<JetType> positionedValueParameterTypes) {
        return new FunctionDescriptorImpl(ERROR_CLASS, Collections.<Attribute>emptyList(), "<ERROR FUNCTION>").initialize(
                Collections.<TypeParameterDescriptor>emptyList(), // TODO
                Collections.<ValueParameterDescriptor>emptyList(), // TODO
                createErrorType("<ERROR FUNCTION RETURN TYPE>")
        );
    }

    private static final JetType ERROR_PARAMETER_TYPE = createErrorType("<ERROR PARAMETER TYPE>");
    private static List<ValueParameterDescriptor> getValueParameters(FunctionDescriptor functionDescriptor, List<JetType> argumentTypes) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, argumentTypesSize = argumentTypes.size(); i < argumentTypesSize; i++) {
            result.add(new ValueParameterDescriptorImpl(
                    functionDescriptor,
                    i,
                    Collections.<Attribute>emptyList(),
                    "<ERROR PARAMETER>",
                    ERROR_PARAMETER_TYPE,
                    ERROR_PARAMETER_TYPE,
                    false,
                    false));
        }
        return result;
    }

    public static JetType createErrorType(String debugMessage) {
        return createErrorType(debugMessage, ERROR_SCOPE);
    }

    private static JetType createErrorType(String debugMessage, JetScope memberScope) {
        return new ErrorTypeImpl(new TypeConstructorImpl(ERROR_CLASS, Collections.<Attribute>emptyList(), false, "[ERROR : " + debugMessage + "]", Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptyList()), memberScope);
    }

    public static JetType createWrongVarianceErrorType(TypeProjection value) {
        return createErrorType(value + " is not allowed here]", value.getType().getMemberScope());
    }

    public static boolean isErrorType(JetType type) {
        return type instanceof ErrorTypeImpl;
    }

    public static ClassifierDescriptor getErrorClass() {
        return ERROR_CLASS;
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

        @Override
        public String toString() {
            return constructor.toString();
        }
    }

    private ErrorUtils() {}
}
