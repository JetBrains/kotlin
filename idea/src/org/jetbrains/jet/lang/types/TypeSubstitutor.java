package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author abreslav
 */
public class TypeSubstitutor {
    public static final TypeSubstitutor INSTANCE = new TypeSubstitutor();

    private TypeSubstitutor() {}

    public JetType substitute(@NotNull JetType context, @NotNull JetType subject, @NotNull Variance howThisTypeIsUsed) {
        return substitute(getSubstitutionContext(context), subject, howThisTypeIsUsed);
    }

    @NotNull
    public JetType substitute(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext, @NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        TypeProjection value = substitutionContext.get(type.getConstructor());
        if (value != null) {
            Variance projectionKind = value.getProjectionKind();
            if (howThisTypeIsUsed.allowsInPosition() && !projectionKind.allowsInPosition()
                    || howThisTypeIsUsed.allowsOutPosition() && !projectionKind.allowsOutPosition()) {
                return ErrorType.createWrongVarianceErrorType(value);
            }
            return value.getType();
        }

        return specializeType(type, substituteInArguments(substitutionContext, type));
    }

    public Map<TypeConstructor, TypeProjection> getSubstitutionContext(JetType context) {
        List<TypeParameterDescriptor> parameters = context.getConstructor().getParameters();
        List<TypeProjection> contextArguments = context.getArguments();

        return buildSubstitutionContext(parameters, contextArguments);
    }

    public Map<TypeConstructor, TypeProjection> buildSubstitutionContext(List<TypeParameterDescriptor> parameters, List<TypeProjection> contextArguments) {
        Map<TypeConstructor, TypeProjection> parameterValues = new HashMap<TypeConstructor, TypeProjection>();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection value = contextArguments.get(i);
            parameterValues.put(parameter.getTypeConstructor(), value);
        }
        return parameterValues;
    }

    @NotNull
    private TypeProjection substituteInProjection(Map<TypeConstructor, TypeProjection> parameterValues, TypeProjection subject) {
        @NotNull JetType subjectType = subject.getType();
        TypeProjection value = parameterValues.get(subjectType.getConstructor());
        if (value != null) {
            return value;
        }
        List<TypeProjection> newArguments = substituteInArguments(parameterValues, subjectType);
        return new TypeProjection(subject.getProjectionKind(), specializeType(subjectType, newArguments));
    }

    private List<TypeProjection> substituteInArguments(Map<TypeConstructor, TypeProjection> parameterValues, JetType subjectType) {
        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        for (TypeProjection argument : subjectType.getArguments()) {
            newArguments.add(substituteInProjection(parameterValues, argument));
        }
        return newArguments;
    }

    private JetType specializeType(JetType type, List<TypeProjection> newArguments) {
        return new JetTypeImpl(type.getAttributes(), type.getConstructor(), type.isNullable(), newArguments, type.getMemberScope());
    }

    public Set<JetType> substituteInSet(Map<TypeConstructor, TypeProjection> substitutionContext, Set<JetType> types, Variance howTheseTypesWillBeUsed) {
        Set<JetType> result = new HashSet<JetType>();
        for (JetType type : types) {
            result.add(substitute(substitutionContext, type, howTheseTypesWillBeUsed));
        }
        return result;
    }
}
