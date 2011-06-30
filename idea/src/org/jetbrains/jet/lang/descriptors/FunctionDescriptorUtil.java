package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.WritableScope;
import org.jetbrains.jet.lang.resolve.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class FunctionDescriptorUtil {
    /** @return Minimal number of arguments to be passed */
    public static int getMinimumArity(@NotNull FunctionDescriptor functionDescriptor) {
        int result = 0;
        for (ValueParameterDescriptor valueParameter : functionDescriptor.getValueParameters()) {
            if (valueParameter.hasDefaultValue()) {
                break;
            }
            result++;
        }
        return result;
    }

    /**
     * @return Maximum number of arguments that can be passed. -1 if unbound (vararg)
     */
    public static int getMaximumArity(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getValueParameters();
        if (unsubstitutedValueParameters.isEmpty()) {
            return 0;
        }
        // TODO : check somewhere that vararg is only the last one, and that varargs do not have default values

        ValueParameterDescriptor lastParameter = unsubstitutedValueParameters.get(unsubstitutedValueParameters.size() - 1);
        if (lastParameter.isVararg()) {
            return -1;
        }
        return unsubstitutedValueParameters.size();
    }

    private static Map<TypeConstructor, TypeProjection> createSubstitutionContext(@NotNull FunctionDescriptor functionDescriptor, List<JetType> typeArguments) {
        if (functionDescriptor.getTypeParameters().isEmpty()) return Collections.emptyMap();

        Map<TypeConstructor, TypeProjection> result = new HashMap<TypeConstructor, TypeProjection>();

        int typeArgumentsSize = typeArguments.size();
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        assert typeArgumentsSize == typeParameters.size();
        for (int i = 0; i < typeArgumentsSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            result.put(typeParameterDescriptor.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        return result;
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(FunctionDescriptor substitutedDescriptor, @NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor substitutor) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
            // TODO : Lazy?
            JetType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getOutType(), Variance.IN_VARIANCE);
            if (substitutedType == null) return null;
            result.add(new ValueParameterDescriptorImpl(
                    substitutedDescriptor,
                    i,
                    unsubstitutedValueParameter.getAnnotations(),
                    unsubstitutedValueParameter.getName(),
                    unsubstitutedValueParameter.getInType() == null ? null : substitutedType,
                    substitutedType,
                    unsubstitutedValueParameter.hasDefaultValue(),
                    unsubstitutedValueParameter.isVararg()
            ));
        }
        return result;
    }

    @Nullable
    public static JetType getSubstitutedReturnType(@NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor substitutor) {
        return substitutor.substitute(functionDescriptor.getReturnType(), Variance.OUT_VARIANCE);
    }

    @Nullable
    public static FunctionDescriptor substituteFunctionDescriptor(@NotNull List<JetType> typeArguments, @NotNull FunctionDescriptor functionDescriptor) {
        Map<TypeConstructor, TypeProjection> substitutionContext = createSubstitutionContext(functionDescriptor, typeArguments);
        return functionDescriptor.substitute(TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    public static JetScope getFunctionInnerScope(@NotNull JetScope outerScope, @NotNull FunctionDescriptor descriptor, @NotNull BindingTrace trace) {
        WritableScope parameterScope = new WritableScopeImpl(outerScope, descriptor, trace.getErrorHandler()).setDebugName("Function inner scope");
        JetType receiverType = descriptor.getReceiverType();
        if (receiverType != null) {
            parameterScope.setThisType(receiverType);
        }
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
            parameterScope.addTypeParameterDescriptor(typeParameter);
        }
        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
        }
        parameterScope.addLabeledDeclaration(descriptor);
        return parameterScope;
    }

    public static class OverrideCompatibilityInfo {

        private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(false, "SUCCESS");

        @NotNull
        public static OverrideCompatibilityInfo nameMismatch() {
            return new OverrideCompatibilityInfo(true, "nameMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo typeParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(true, "typeParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterNumberMismatch() {
            return new OverrideCompatibilityInfo(true, "valueParameterNumberMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo boundsMismatch(TypeParameterDescriptor superTypeParameter, TypeParameterDescriptor subTypeParameter) {
            return new OverrideCompatibilityInfo(true, "boundsMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo valueParameterTypeMismatch(ValueParameterDescriptor superValueParameter, ValueParameterDescriptor subValueParameter) {
            return new OverrideCompatibilityInfo(true, "valueParameterTypeMismatch"); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo returnTypeMismatch(JetType substitutedSuperReturnType, JetType unsubstitutedSubReturnType) {
            return new OverrideCompatibilityInfo(true, "returnTypeMismatch: " + unsubstitutedSubReturnType + " >< " + substitutedSuperReturnType); // TODO
        }

        @NotNull
        public static OverrideCompatibilityInfo success() {
            return SUCCESS;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private final boolean isError;
        private final String message;

        public OverrideCompatibilityInfo(boolean error, String message) {
            isError = error;
            this.message = message;
        }

        public boolean isSuccess() {
            return !isError;
        }

        public String getMessage() {
            return message;
        }
    }

    @NotNull
    public static OverrideCompatibilityInfo isOverridableBy(@NotNull JetTypeChecker typeChecker, @NotNull FunctionDescriptor superFunction, @NotNull FunctionDescriptor subFunction) {
        if (!superFunction.getName().equals(subFunction.getName())) {
            return OverrideCompatibilityInfo.nameMismatch();
        }

        // TODO : Visibility

        if (superFunction.getTypeParameters().size() != subFunction.getTypeParameters().size()) {
            return OverrideCompatibilityInfo.typeParameterNumberMismatch();
        }

        if (superFunction.getValueParameters().size() != subFunction.getValueParameters().size()) {
            return OverrideCompatibilityInfo.valueParameterNumberMismatch();
        }

        List<TypeParameterDescriptor> superTypeParameters = superFunction.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subFunction.getTypeParameters();

        Map<TypeConstructor, TypeProjection> substitutionContext = Maps.newHashMap();
        BiMap<TypeConstructor, TypeConstructor> axioms = HashBiMap.create();
        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);
            substitutionContext.put(
                    superTypeParameter.getTypeConstructor(),
                    new TypeProjection(subTypeParameter.getDefaultType()));
            axioms.put(superTypeParameter.getTypeConstructor(), subTypeParameter.getTypeConstructor());
        }

        for (int i = 0, typeParametersSize = superTypeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor superTypeParameter = superTypeParameters.get(i);
            TypeParameterDescriptor subTypeParameter = subTypeParameters.get(i);


            if (!JetTypeImpl.equalTypes(superTypeParameter.getBoundsAsType(), subTypeParameter.getBoundsAsType(), axioms)) {
                return OverrideCompatibilityInfo.boundsMismatch(superTypeParameter, subTypeParameter);
            }
        }

        List<ValueParameterDescriptor> superValueParameters = superFunction.getValueParameters();
        List<ValueParameterDescriptor> subValueParameters = subFunction.getValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = superValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor superValueParameter = superValueParameters.get(i);
            ValueParameterDescriptor subValueParameter = subValueParameters.get(i);

            if (!JetTypeImpl.equalTypes(superValueParameter.getOutType(), subValueParameter.getOutType(), axioms)) {
                return OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameter, subValueParameter);
            }
        }

        // TODO : Default values, varargs etc

        TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitutionContext);
        JetType substitutedSuperReturnType = typeSubstitutor.substitute(superFunction.getReturnType(), Variance.OUT_VARIANCE);
        assert substitutedSuperReturnType != null;
        if (!typeChecker.isSubtypeOf(subFunction.getReturnType(), substitutedSuperReturnType)) {
            return OverrideCompatibilityInfo.returnTypeMismatch(substitutedSuperReturnType, subFunction.getReturnType());
        }

        return OverrideCompatibilityInfo.success();
    }

}
