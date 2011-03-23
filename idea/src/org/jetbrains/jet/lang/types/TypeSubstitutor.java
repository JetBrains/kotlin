package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;

import java.util.*;

/**
 * @author abreslav
 */
public class TypeSubstitutor {

    public static final class SubstitutionException extends Exception {
        public SubstitutionException(String message) {
            super(message);
        }
    }

    public static final TypeSubstitutor INSTANCE = new TypeSubstitutor();

    private TypeSubstitutor() {}

    public JetType safeSubstitute(@NotNull JetType context, @NotNull JetType subject, @NotNull Variance howThisTypeIsUsed) {
        return safeSubstitute(TypeUtils.buildSubstitutionContext(context), subject, howThisTypeIsUsed);
    }

    @NotNull
    public JetType safeSubstitute(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext, @NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        try {
            return unsafeSubstitute(substitutionContext, type, howThisTypeIsUsed);
        } catch (SubstitutionException e) {
            return ErrorType.createErrorType(e.getMessage());
        }
    }

    @Nullable
    public JetType substitute(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext, @NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        try {
            return unsafeSubstitute(substitutionContext, type, howThisTypeIsUsed);
        } catch (SubstitutionException e) {
            return null;
        }
    }

    @NotNull
    private JetType unsafeSubstitute(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext, @NotNull JetType type, @NotNull Variance howThisTypeIsUsed) throws SubstitutionException {
        TypeConstructor constructor = type.getConstructor();
        TypeProjection value = substitutionContext.get(constructor);
        if (value != null) {
            assert constructor.getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            if (!allows(howThisTypeIsUsed, value.getProjectionKind())) {
                throw new SubstitutionException("!!" + value.toString());
            }
            return value.getType();
        }

        return specializeType(type, substitutionContext, howThisTypeIsUsed);
    }

    private JetType specializeType(JetType subjectType, Map<TypeConstructor, TypeProjection> substitutionContext, Variance callSiteVariance) throws SubstitutionException {
        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        List<TypeProjection> arguments = subjectType.getArguments();
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            TypeProjection argument = arguments.get(i);
            TypeParameterDescriptor parameterDescriptor = subjectType.getConstructor().getParameters().get(i);
            newArguments.add(substituteInProjection(
                    substitutionContext,
                    argument,
                    parameterDescriptor,
                    callSiteVariance));
        }
        return new JetTypeImpl(
                subjectType.getAttributes(),
                subjectType.getConstructor(),
                subjectType.isNullable(),
                newArguments,
                new SubstitutingScope(subjectType.getMemberScope(), substitutionContext));
    }

    @NotNull
    private TypeProjection substituteInProjection(
            @NotNull Map<TypeConstructor, TypeProjection> substitutionContext,
            @NotNull TypeProjection passedProjection,
            @NotNull TypeParameterDescriptor correspondingTypeParameter,
            @NotNull Variance contextCallSiteVariance) throws SubstitutionException {
        JetType typeToSubstituteIn = passedProjection.getType();
        Variance passedProjectionKind = passedProjection.getProjectionKind();
        Variance parameterVariance = correspondingTypeParameter.getVariance();

        Variance effectiveProjectionKind = (passedProjectionKind == Variance.INVARIANT) ? parameterVariance : passedProjectionKind;
        Variance effectiveContextVariance = contextCallSiteVariance.superpose(effectiveProjectionKind);

        TypeProjection projectionValue = substitutionContext.get(typeToSubstituteIn.getConstructor());
        if (projectionValue != null) {
            assert typeToSubstituteIn.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            JetType typeValue = projectionValue.getType();
            Variance projectionKindValue = projectionValue.getProjectionKind();

            if (!allows(parameterVariance, passedProjectionKind)) {
                return TypeUtils.makeStarProjection( correspondingTypeParameter);
            }

            if (!allows(effectiveContextVariance, projectionKindValue)) {
                throw new SubstitutionException(""); // TODO : error message
            }

            Variance effectiveProjectionKindValue = passedProjectionKind == Variance.INVARIANT ? projectionKindValue : passedProjectionKind;
            return new TypeProjection(effectiveProjectionKindValue,  specializeType(typeValue, substitutionContext, effectiveContextVariance));
        }
        return new TypeProjection(
                passedProjectionKind,
                specializeType(
                        typeToSubstituteIn,
                        substitutionContext,
                        effectiveContextVariance));
    }

    //    public Set<JetType> substituteInSet(Map<TypeConstructor, TypeProjection> substitutionContext, Set<JetType> types, Variance howTheseTypesWillBeUsed) {
//        Set<JetType> result = new HashSet<JetType>();
//        for (JetType type : types) {
//            result.add(safeSubstitute(substitutionContext, type, howTheseTypesWillBeUsed));
//        }
//        return result;
//    }

    private boolean allows(Variance declarationSiteVariance, Variance callSiteVariance) {
        switch (declarationSiteVariance) {
            case INVARIANT: return true;
            case IN_VARIANCE: return callSiteVariance != Variance.OUT_VARIANCE;
            case OUT_VARIANCE: return callSiteVariance != Variance.IN_VARIANCE;
        }
        throw new IllegalStateException(declarationSiteVariance.toString());
    }
}
