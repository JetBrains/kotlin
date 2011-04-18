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

    public static TypeSubstitutor create(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        return new TypeSubstitutor(substitutionContext);
    }

    public static TypeSubstitutor create(@NotNull JetType context) {
        return create(TypeUtils.buildSubstitutionContext(context));
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final @NotNull Map<TypeConstructor, TypeProjection> substitutionContext;

    private TypeSubstitutor(@NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        this.substitutionContext = substitutionContext;
    }

    public boolean isEmpty() {
        return substitutionContext.isEmpty();
    }

    @NotNull
    public JetType safeSubstitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        if (isEmpty()) {
            return type;
        }

        try {
            return unsafeSubstitute(type, howThisTypeIsUsed);
        } catch (SubstitutionException e) {
            return ErrorUtils.createErrorType(e.getMessage());
        }
    }

    @Nullable
    public JetType substitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) {
        if (isEmpty()) {
            return type;
        }

        try {
            return unsafeSubstitute(type, howThisTypeIsUsed);
        } catch (SubstitutionException e) {
            return null;
        }
    }

    @NotNull
    private JetType unsafeSubstitute(@NotNull JetType type, @NotNull Variance howThisTypeIsUsed) throws SubstitutionException {
        TypeConstructor constructor = type.getConstructor();
        TypeProjection value = substitutionContext.get(constructor);
        if (value != null) {
            assert constructor.getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            return substitutionResult((TypeParameterDescriptor) constructor.getDeclarationDescriptor(), howThisTypeIsUsed, Variance.INVARIANT, value).getType();

//            if (!allows(howThisTypeIsUsed, value.getProjectionKind())) {
//                throw new SubstitutionException("!!" + value.toString());
//            }
//            return value.getType();
        }

        return specializeType(type, howThisTypeIsUsed);
    }

    private JetType specializeType(JetType subjectType, Variance callSiteVariance) throws SubstitutionException {
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
                new SubstitutingScope(subjectType.getMemberScope(), this));
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

        Variance effectiveProjectionKind = asymmetricOr(passedProjectionKind, parameterVariance);
        Variance effectiveContextVariance = contextCallSiteVariance.superpose(effectiveProjectionKind);

        TypeProjection projectionValue = substitutionContext.get(typeToSubstituteIn.getConstructor());
        if (projectionValue != null) {
            assert typeToSubstituteIn.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            if (!allows(parameterVariance, passedProjectionKind)) {
                return TypeUtils.makeStarProjection(correspondingTypeParameter);
            }

            return substitutionResult(correspondingTypeParameter, effectiveContextVariance, passedProjectionKind, projectionValue);
        }
        return new TypeProjection(
                passedProjectionKind,
                specializeType(
                        typeToSubstituteIn,
                        effectiveContextVariance));
    }

    private TypeProjection substitutionResult(
            TypeParameterDescriptor correspondingTypeParameter,
            Variance effectiveContextVariance,
            Variance passedProjectionKind,
            TypeProjection value) throws SubstitutionException {
        Variance projectionKindValue = value.getProjectionKind();
        JetType typeValue = value.getType();
        Variance effectiveProjectionKindValue = asymmetricOr(passedProjectionKind, projectionKindValue);
        JetType effectiveTypeValue;
        switch (effectiveContextVariance) {
            case INVARIANT:
                effectiveProjectionKindValue = projectionKindValue;
                effectiveTypeValue = typeValue;
                break;
            case IN_VARIANCE:
                if (projectionKindValue == Variance.OUT_VARIANCE) {
                    throw new SubstitutionException(""); // TODO
//                    effectiveProjectionKindValue = Variance.INVARIANT;
//                    effectiveTypeValue = JetStandardClasses.getNothingType();
                }
                else {
                    effectiveTypeValue = typeValue;
                }
                break;
            case OUT_VARIANCE:
                if (projectionKindValue == Variance.IN_VARIANCE) {
                    effectiveProjectionKindValue = Variance.INVARIANT;
                    effectiveTypeValue = correspondingTypeParameter.getBoundsAsType();
                }
                else {
                    effectiveTypeValue = typeValue;
                }
                break;
            default:
                throw new IllegalStateException(effectiveContextVariance.toString());
        }

//            if (!allows(effectiveContextVariance, projectionKindValue)) {
//                throw new SubstitutionException(""); // TODO : error message
//            }
//
        return new TypeProjection(effectiveProjectionKindValue,  specializeType(effectiveTypeValue, effectiveContextVariance));
    }

    private static Variance asymmetricOr(Variance a, Variance b) {
        return a == Variance.INVARIANT ? b : a;
    }

    private static boolean allows(Variance declarationSiteVariance, Variance callSiteVariance) {
        switch (declarationSiteVariance) {
            case INVARIANT: return true;
            case IN_VARIANCE: return callSiteVariance != Variance.OUT_VARIANCE;
            case OUT_VARIANCE: return callSiteVariance != Variance.IN_VARIANCE;
        }
        throw new IllegalStateException(declarationSiteVariance.toString());
    }
}
