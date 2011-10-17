package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

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
        public VariableDescriptor getVariable(@NotNull String name) {
            return ERROR_PROPERTY;
        }

        @Override
        public NamespaceDescriptor getNamespace(@NotNull String name) {
            return null; // TODO : review
        }

        @NotNull
        @Override
        public ReceiverDescriptor getImplicitReceiver() {
            return ReceiverDescriptor.NO_RECEIVER;
        }

        @Override
        public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        }

        @NotNull
        @Override
        public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
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

        @Override
        public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
            return null; // TODO : review
        }

        @Override
        public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
            return ERROR_CLASS; // TODO : review
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getAllDescriptors() {
            return Collections.emptyList();
        }

    };

    private static final ClassDescriptorImpl ERROR_CLASS = new ClassDescriptorImpl(ERROR_MODULE, Collections.<AnnotationDescriptor>emptyList(), "<ERROR CLASS>") {
        @NotNull
        @Override
        public Set<FunctionDescriptor> getConstructors() {
            return ERROR_FUNCTION_GROUP;
        }

        @NotNull
        @Override
        public Modality getModality() {
            return Modality.OPEN;
        }
    };

    private static final Set<FunctionDescriptor> ERROR_FUNCTION_GROUP = Collections.singleton(createErrorFunction(0, Collections.<JetType>emptyList()));
    private static final ConstructorDescriptor ERROR_CONSTRUCTOR = new ConstructorDescriptorImpl(ERROR_CLASS, Collections.<AnnotationDescriptor>emptyList(), true);

    static {
        ERROR_CLASS.initialize(
            true, Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptyList(), getErrorScope(), ERROR_FUNCTION_GROUP, ERROR_CONSTRUCTOR);
    }

    private static JetScope getErrorScope() {
        return ERROR_SCOPE;
    }

    private static final JetType ERROR_PROPERTY_TYPE = createErrorType("<ERROR PROPERTY TYPE>");
    private static final VariableDescriptor ERROR_PROPERTY = new PropertyDescriptor(
            ERROR_CLASS,
            Collections.<AnnotationDescriptor>emptyList(),
            Modality.OPEN,
            Visibility.INTERNAL,
            true,
            null,
            ReceiverDescriptor.NO_RECEIVER,
            "<ERROR PROPERTY>",
            ERROR_PROPERTY_TYPE, ERROR_PROPERTY_TYPE);

    private static FunctionDescriptor createErrorFunction(List<TypeParameterDescriptor> typeParameters, List<JetType> positionedValueArgumentTypes) {
        FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(ERROR_CLASS, Collections.<AnnotationDescriptor>emptyList(), "<ERROR FUNCTION>");
        return functionDescriptor.initialize(
                null,
                ReceiverDescriptor.NO_RECEIVER,
                typeParameters,
                getValueParameters(functionDescriptor, positionedValueArgumentTypes),
                createErrorType("<ERROR FUNCTION RETURN>"),
                Modality.OPEN,
                Visibility.INTERNAL
        );
    }

    public static FunctionDescriptor createErrorFunction(int typeParameterCount, List<JetType> positionedValueParameterTypes) {
        return new FunctionDescriptorImpl(ERROR_CLASS, Collections.<AnnotationDescriptor>emptyList(), "<ERROR FUNCTION>").initialize(
                null,
                ReceiverDescriptor.NO_RECEIVER,
                Collections.<TypeParameterDescriptor>emptyList(), // TODO
                Collections.<ValueParameterDescriptor>emptyList(), // TODO
                createErrorType("<ERROR FUNCTION RETURN TYPE>"),
                Modality.OPEN,
                Visibility.INTERNAL
        );
    }

    private static final JetType ERROR_PARAMETER_TYPE = createErrorType("<ERROR VALUE_PARAMETER TYPE>");
    private static List<ValueParameterDescriptor> getValueParameters(FunctionDescriptor functionDescriptor, List<JetType> argumentTypes) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, argumentTypesSize = argumentTypes.size(); i < argumentTypesSize; i++) {
            result.add(new ValueParameterDescriptorImpl(
                    functionDescriptor,
                    i,
                    Collections.<AnnotationDescriptor>emptyList(),
                    "<ERROR VALUE_PARAMETER>",
                    ERROR_PARAMETER_TYPE,
                    ERROR_PARAMETER_TYPE,
                    false,
                    false));
        }
        return result;
    }

    @NotNull
    public static JetType createErrorType(String debugMessage) {
        return createErrorType(debugMessage, ERROR_SCOPE);
    }

    private static JetType createErrorType(String debugMessage, JetScope memberScope) {
        return new ErrorTypeImpl(new TypeConstructorImpl(ERROR_CLASS, Collections.<AnnotationDescriptor>emptyList(), false, "[ERROR : " + debugMessage + "]", Collections.<TypeParameterDescriptor>emptyList(), Collections.singleton(JetStandardClasses.getAnyType())), memberScope);
    }

    public static JetType createWrongVarianceErrorType(TypeProjection value) {
        return createErrorType(value + " is not allowed here]", value.getType().getMemberScope());
    }

    public static ClassifierDescriptor getErrorClass() {
        return ERROR_CLASS;
    }

    public static boolean isError(@NotNull TypeConstructor typeConstructor) {
        return typeConstructor == ERROR_CLASS.getTypeConstructor();
    }

    public static boolean isErrorType(@NotNull JetType type) {
        return type != TypeUtils.NO_EXPECTED_TYPE &&(
               (type instanceof DeferredType && ((DeferredType) type).getActualType() == null) ||
               type instanceof ErrorTypeImpl ||
               isError(type.getConstructor()));
    }

    public static boolean isError(@NotNull DeclarationDescriptor candidate) {
        return candidate.getContainingDeclaration() == getErrorClass() || candidate == ERROR_MODULE;
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
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return constructor.toString();
        }
    }

    private ErrorUtils() {}
}
