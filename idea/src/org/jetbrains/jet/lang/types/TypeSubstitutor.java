package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class TypeSubstitutor {
    public static final TypeSubstitutor INSTANCE = new TypeSubstitutor();

    private TypeSubstitutor() {}

    public Type substitute(@NotNull Type context, @NotNull Type subject, @NotNull Variance howThisTypeIsUsed) {
        return substitute(getSubstitutionContext(context), subject, howThisTypeIsUsed);
    }

    @NotNull
    public Type substitute(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext, @NotNull Type type, @NotNull Variance howThisTypeIsUsed) {
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

    public Map<TypeConstructor, TypeProjection> getSubstitutionContext(Type context) {
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
        @NotNull Type subjectType = subject.getType();
        TypeProjection value = parameterValues.get(subjectType.getConstructor());
        if (value != null) {
            return value;
        }
        List<TypeProjection> newArguments = substituteInArguments(parameterValues, subjectType);
        return new TypeProjection(subject.getProjectionKind(), specializeType(subjectType, newArguments));
    }

    private List<TypeProjection> substituteInArguments(Map<TypeConstructor, TypeProjection> parameterValues, Type subjectType) {
        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        for (TypeProjection argument : subjectType.getArguments()) {
            newArguments.add(substituteInProjection(parameterValues, argument));
        }
        return newArguments;
    }

    private Type specializeType(Type type, List<TypeProjection> newArguments) {
        return new TypeImpl(type.getAttributes(), type.getConstructor(), type.isNullable(), newArguments, type.getMemberScope());
    }
}
