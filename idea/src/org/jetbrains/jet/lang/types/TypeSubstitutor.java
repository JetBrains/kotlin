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

    public static final TypeSubstitutor INSTANCE_FOR_CONSTRUCTORS = new TypeSubstitutor() {
        @Override
        protected boolean errorCondition(Variance ve, Variance p1) {
            return false;
        }
    };

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

            if (errorCondition(howThisTypeIsUsed, value.getProjectionKind())) {
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
            @NotNull TypeProjection p0_E,
            @NotNull TypeParameterDescriptor d_T, // variance of the parameter this projection is substituted for
            @NotNull Variance v) throws SubstitutionException {
        JetType E = p0_E.getType();
        Variance p0 = p0_E.getProjectionKind();
        Variance d = d_T.getVariance();

        Variance p01 = (p0 == Variance.INVARIANT) ? d : p0;
        Variance ve = v.superpose(p01);

        TypeProjection p1_A = substitutionContext.get(E.getConstructor());
        if (p1_A != null) {
            assert E.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;

            JetType A = p1_A.getType();
            Variance p1 = p1_A.getProjectionKind();

            if (!allows(d, p0)) {
                return TypeUtils.makeStarProjection(d_T);
            }

            if (errorCondition(ve, p1)) {
                throw new SubstitutionException(""); // TODO : error message
            }

            return new TypeProjection(p0 == Variance.INVARIANT ? p1 : p0,  specializeType(A, substitutionContext, ve));
        }
        return new TypeProjection(
                p0,
                specializeType(E, substitutionContext, ve));
    }

    protected boolean errorCondition(Variance ve, Variance p1) {
        return !allows(ve, p1);
    }

    public Set<JetType> substituteInSet(Map<TypeConstructor, TypeProjection> substitutionContext, Set<JetType> types, Variance howTheseTypesWillBeUsed) {
        Set<JetType> result = new HashSet<JetType>();
        for (JetType type : types) {
            result.add(safeSubstitute(substitutionContext, type, howTheseTypesWillBeUsed));
        }
        return result;
    }

    private boolean allows(Variance declarationSiteVariance, Variance callSiteVariance) {
        switch (declarationSiteVariance) {
            case INVARIANT: return true;
            case IN_VARIANCE: return callSiteVariance != Variance.OUT_VARIANCE;
            case OUT_VARIANCE: return callSiteVariance != Variance.IN_VARIANCE;
        }
        throw new IllegalStateException(declarationSiteVariance.toString());
    }
}
