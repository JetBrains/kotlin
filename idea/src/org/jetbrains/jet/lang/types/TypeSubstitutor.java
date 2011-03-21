package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;

import java.util.*;

/**
 * @author abreslav
 */
public class TypeSubstitutor {
    public static final TypeSubstitutor INSTANCE = new TypeSubstitutor();

    private TypeSubstitutor() {}

    public JetType substitute(@NotNull JetType context, @NotNull JetType subject, @NotNull Variance howThisTypeIsUsed) {
        return substitute(buildSubstitutionContext(context), subject, howThisTypeIsUsed);
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

        return specializeType(type, substitutionContext);
    }

    private JetType specializeType(JetType subjectType, Map<TypeConstructor, TypeProjection> substitutionContext) {
        return new JetTypeImpl(
                subjectType.getAttributes(),
                subjectType.getConstructor(),
                subjectType.isNullable(),
                substituteInArguments(substitutionContext, subjectType),
                new SubstitutingScope(subjectType.getMemberScope(), substitutionContext));
    }

    private List<TypeProjection> substituteInArguments(Map<TypeConstructor, TypeProjection> substitutionContext, JetType subjectType) {
        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        for (TypeProjection argument : subjectType.getArguments()) {
            newArguments.add(substituteInProjection(substitutionContext, argument));
        }
        return newArguments;
    }

    @NotNull
    private TypeProjection substituteInProjection(Map<TypeConstructor, TypeProjection> substitutionContext, TypeProjection subject) {
        JetType subjectType = subject.getType();
        TypeProjection value = substitutionContext.get(subjectType.getConstructor());
        if (value != null) {
            return value;
        }
        return new TypeProjection(subject.getProjectionKind(), specializeType(subjectType, substitutionContext));
    }

    public Set<JetType> substituteInSet(Map<TypeConstructor, TypeProjection> substitutionContext, Set<JetType> types, Variance howTheseTypesWillBeUsed) {
        Set<JetType> result = new HashSet<JetType>();
        for (JetType type : types) {
            result.add(substitute(substitutionContext, type, howTheseTypesWillBeUsed));
        }
        return result;
    }

    public Map<TypeConstructor, TypeProjection> buildSubstitutionContext(JetType context) {
        return buildSubstitutionContext(context.getConstructor().getParameters(), context.getArguments());
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
}
