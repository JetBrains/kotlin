package org.jetbrains.jet.lang.types;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;

import static org.jetbrains.jet.lang.types.Variance.INVARIANT;
import static org.jetbrains.jet.lang.types.Variance.IN_VARIANCE;
import static org.jetbrains.jet.lang.types.Variance.OUT_VARIANCE;

/**
 * @author abreslav
 */
public class JetTypeChecker {

    public static final JetTypeChecker INSTANCE = new JetTypeChecker();
    public static final HashBiMap<TypeConstructor, TypeConstructor> EMPTY_AXIOMS = HashBiMap.create();

    private JetTypeChecker() {
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
//        return new TypeCheckingProcedure().run(subtype, supertype);
        return TYPE_CHECKER.isSubtypeOf(subtype, supertype);
    }

    public boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return equalTypes(a, b, EMPTY_AXIOMS);
    }

    public boolean equalTypes(@NotNull JetType a, @NotNull JetType b, @NotNull BiMap<TypeConstructor, TypeConstructor> equalityAxioms) {
        return TYPE_CHECKER.equalTypes(a, b, equalityAxioms);
    }

    // This method returns the supertype of the first parameter that has the same constructor
    // as the second parameter, applying the substitution of type arguments to it
    @Nullable
    private static JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
        TypeConstructor constructor = subtype.getConstructor();
        if (constructor.equals(supertype.getConstructor())) {
            return subtype;
        }
        for (JetType immediateSupertype : constructor.getSupertypes()) {
            JetType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
            if (correspondingSupertype != null) {
                return TypeSubstitutor.create(subtype).safeSubstitute(correspondingSupertype, Variance.INVARIANT);
            }
        }
        return null;
    }

    private static JetType getOutType(TypeParameterDescriptor parameter, TypeProjection argument) {
        boolean isOutProjected = argument.getProjectionKind() == IN_VARIANCE || parameter.getVariance() == IN_VARIANCE;
        return isOutProjected ? parameter.getUpperBoundsAsType() : argument.getType();
    }

    private static JetType getInType(TypeParameterDescriptor parameter, TypeProjection argument) {
        boolean isOutProjected = argument.getProjectionKind() == OUT_VARIANCE || parameter.getVariance() == OUT_VARIANCE;
        return isOutProjected ? JetStandardClasses.getNothingType() : argument.getType();
    }

    /**
     * Methods of this class return true to continue type checking and false to fail
     */
    public interface TypingConstraintBuilder {
        boolean assertEqualTypes(@NotNull JetType a, @NotNull JetType b);
        boolean assertSubtype(@NotNull JetType subtype, @NotNull JetType supertype);
        boolean noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype);
    }

    private static final TypeCheckingProcedure TYPE_CHECKER = new TypeCheckingProcedure(new TypingConstraintBuilder() {
        @Override
        public boolean assertEqualTypes(@NotNull JetType a, @NotNull JetType b) {
            return TypeUtils.equalTypes(a, b);
        }

        @Override
        public boolean assertSubtype(@NotNull JetType subtype, @NotNull JetType supertype) {
            return INSTANCE.isSubtypeOf(subtype, supertype);
        }

        @Override
        public boolean noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
            return false; // type checking fails
        }
    });

    public static class TypeCheckingProcedure {

        private final TypingConstraintBuilder constraintBuilder;

        public TypeCheckingProcedure(TypingConstraintBuilder constraintBuilder) {
            this.constraintBuilder = constraintBuilder;
        }

        public boolean equalTypes(@NotNull JetType type1, @NotNull JetType type2, @NotNull BiMap<TypeConstructor, TypeConstructor> equalityAxioms) {
            if (type1.isNullable() != type2.isNullable()) {
                return false;
            }
            TypeConstructor constructor1 = type1.getConstructor();
            TypeConstructor constructor2 = type2.getConstructor();
            if (!constructor1.equals(constructor2)) {
                TypeConstructor img1 = equalityAxioms.get(constructor1);
                TypeConstructor img2 = equalityAxioms.get(constructor2);
                if (!(img1 != null && img1.equals(constructor2)) &&
                        !(img2 != null && img2.equals(constructor1))) {
                    return false;
                }
            }
            List<TypeProjection> type1Arguments = type1.getArguments();
            List<TypeProjection> type2Arguments = type2.getArguments();
            if (type1Arguments.size() != type2Arguments.size()) {
                return false;
            }

            for (int i = 0; i < type1Arguments.size(); i++) {
                TypeProjection typeProjection1 = type1Arguments.get(i);
                TypeProjection typeProjection2 = type2Arguments.get(i);
                if (typeProjection1.getProjectionKind() != typeProjection2.getProjectionKind()) {
                    return false;
                }
                if (!equalTypes(typeProjection1.getType(), typeProjection2.getType(), equalityAxioms)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
            if (ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype)) {
                return true;
            }
            if (!supertype.isNullable() && subtype.isNullable()) {
                return false;
            }
            subtype = TypeUtils.makeNotNullable(subtype);
            supertype = TypeUtils.makeNotNullable(supertype);
            if (JetStandardClasses.isNothingOrNullableNothing(subtype)) {
                return true;
            }
            @Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
            if (closestSupertype == null) {
                return constraintBuilder.noCorrespondingSupertype(subtype, supertype); // if this returns true, there still isn't any supertype to continue with
            }

            return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
        }

        private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype) {
            TypeConstructor constructor = subtype.getConstructor();
            assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

            List<TypeProjection> subArguments = subtype.getArguments();
            List<TypeProjection> superArguments = supertype.getArguments();
            List<TypeParameterDescriptor> parameters = constructor.getParameters();
            for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
                TypeParameterDescriptor parameter = parameters.get(i);
                

                TypeProjection subArgument = subArguments.get(i);
                JetType subIn = getInType(parameter, subArgument);
                JetType subOut = getOutType(parameter, subArgument);

                TypeProjection superArgument = superArguments.get(i);
                JetType superIn = getInType(parameter, superArgument);
                JetType superOut = getOutType(parameter, superArgument);

                if (parameter.getVariance() == INVARIANT && subArgument.getProjectionKind() == INVARIANT && superArgument.getProjectionKind() == INVARIANT) {
                    if (!constraintBuilder.assertEqualTypes(subArgument.getType(), superArgument.getType())) return false;
                }
                else {
                    if (!constraintBuilder.assertSubtype(subOut, superOut)) return false;
                    if (!constraintBuilder.assertSubtype(superIn, subIn)) return false;
                }
            }
            return true;
        }
    }

}
