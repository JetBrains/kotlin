package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.*;

import static org.jetbrains.jet.lang.types.Variance.IN_VARIANCE;
import static org.jetbrains.jet.lang.types.Variance.OUT_VARIANCE;

/**
 * @author abreslav
 */
public class JetTypeChecker {

    public static final JetTypeChecker INSTANCE = new JetTypeChecker();

    private JetTypeChecker() {
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
//        return new TypeCheckingProcedure().run(subtype, supertype);
        return new ExplicitInOutTypeCheckingProcedure().run(subtype, supertype);
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

    private static class ExplicitInOutTypeCheckingProcedure {

        public boolean run(@NotNull JetType subtype, @NotNull JetType supertype) {
            return isSubtypeOf(subtype, supertype);
        }

        public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
            if (ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype)) {
                return true;
            }
            if (!supertype.isNullable() && subtype.isNullable()) {
                return false;
            }
            if (JetStandardClasses.isNothingOrNullableNothing(subtype)) {
                return true;
            }
            @Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
            if (closestSupertype == null) {
                return false;
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

                if (!isSubtypeOf(subOut, superOut) || !isSubtypeOf(superIn, subIn)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static abstract class AbstractTypeCheckingProcedure<T> {

        protected enum StatusAction {
            PROCEED(false),
            DONE_WITH_CURRENT_TYPE(true),
            ABORT_ALL(true);

            private final boolean abort;

            private StatusAction(boolean abort) {
                this.abort = abort;
            }

            public boolean isAbort() {
                return abort;
            }
        }

        public final T run(@NotNull JetType subtype, @NotNull JetType supertype) {
            proceedOrStop(subtype, supertype);
            return result();
        }

        protected abstract StatusAction startForPairOfTypes(@NotNull JetType subtype, @NotNull JetType supertype);
        
        protected abstract StatusAction noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype);

        protected abstract StatusAction equalTypesRequired(@NotNull JetType subArgumentType, @NotNull JetType superArgumentType);

        protected abstract StatusAction varianceConflictFound(@NotNull TypeProjection subArgument, @NotNull TypeProjection superArgument);

        protected abstract StatusAction doneForPairOfTypes(@NotNull JetType subtype, @NotNull JetType supertype);

        protected abstract T result();

        private StatusAction proceedOrStop(@NotNull JetType subtype, @NotNull JetType supertype) {
            StatusAction statusAction = startForPairOfTypes(subtype, supertype);
            if (statusAction.isAbort()) {
                return statusAction;
            }

            JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
            if (closestSupertype == null) {
                return noCorrespondingSupertype(subtype, supertype);
            }

            proceed(closestSupertype, supertype);
            return doneForPairOfTypes(subtype, supertype);
        }

        private void proceed(@NotNull JetType subtype, @NotNull JetType supertype) {
            TypeConstructor constructor = subtype.getConstructor();
            assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

            List<TypeProjection> subArguments = subtype.getArguments();
            List<TypeProjection> superArguments = supertype.getArguments();
            List<TypeParameterDescriptor> parameters = constructor.getParameters();

            loop:
            for (int i = 0; i < parameters.size(); i++) {
                TypeParameterDescriptor parameter = parameters.get(i);
                TypeProjection subArgument = subArguments.get(i);
                TypeProjection superArgument = superArguments.get(i);

                JetType subArgumentType = subArgument.getType();
                JetType superArgumentType = superArgument.getType();

                StatusAction action = null;
                switch (parameter.getVariance()) {
                    case INVARIANT:
                        switch (superArgument.getProjectionKind()) {
                            case INVARIANT:
                                action = equalTypesRequired(subArgumentType, superArgumentType);
                                break;
                            case OUT_VARIANCE:
                                if (!subArgument.getProjectionKind().allowsOutPosition()) {
                                    action = varianceConflictFound(subArgument, superArgument);
                                }
                                else {
                                    action = proceedOrStop(subArgumentType, superArgumentType);
                                }
                                break;
                            case IN_VARIANCE:
                                if (!subArgument.getProjectionKind().allowsInPosition()) {
                                    action = varianceConflictFound(subArgument, superArgument);
                                }
                                else {
                                    action = proceedOrStop(superArgumentType, subArgumentType);
                                }
                                break;
                        }
                        break;
                    case IN_VARIANCE:
                        switch (superArgument.getProjectionKind()) {
                            case INVARIANT:
                            case IN_VARIANCE:
                                action = proceedOrStop(superArgumentType, subArgumentType);
                                break;
                            case OUT_VARIANCE:
                                action = proceedOrStop(subArgumentType, superArgumentType);
                                break;
                        }
                        break;
                    case OUT_VARIANCE:
                        switch (superArgument.getProjectionKind()) {
                            case INVARIANT:
                            case OUT_VARIANCE:
                            case IN_VARIANCE:
                                action = proceedOrStop(subArgumentType, superArgumentType);
                                break;
                        }
                        break;
                }
                switch (action) {
                    case ABORT_ALL: break loop;
                    case DONE_WITH_CURRENT_TYPE:
                    default:
                }
            }
        }

    }


    private static class TypeCheckingProcedure extends AbstractTypeCheckingProcedure<Boolean> {

        private boolean result = true;

        private StatusAction fail() {
            result = false;
            return StatusAction.ABORT_ALL;
        }

        @Override
        public StatusAction startForPairOfTypes(@NotNull JetType subtype, @NotNull JetType supertype) {
            if (ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype)) {
                return StatusAction.DONE_WITH_CURRENT_TYPE;
            }
            if (!supertype.isNullable() && subtype.isNullable()) {
                return fail();
            }
            if (JetStandardClasses.isNothingOrNullableNothing(subtype)) {
                return StatusAction.DONE_WITH_CURRENT_TYPE;
            }
            return StatusAction.PROCEED;
        }

        @Override
        protected StatusAction noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
            return fail();
        }

        @Override
        protected StatusAction equalTypesRequired(@NotNull JetType subArgumentType, @NotNull JetType superArgumentType) {
            if (!subArgumentType.equals(superArgumentType)) {
                return fail();
            }
            return StatusAction.PROCEED;
        }

        @Override
        protected StatusAction varianceConflictFound(@NotNull TypeProjection subArgument, @NotNull TypeProjection superArgument) {
            return fail();
        }

        @Override
        protected StatusAction doneForPairOfTypes(@NotNull JetType subtype, @NotNull JetType supertype) {
            return StatusAction.PROCEED;
        }

        @Override
        protected Boolean result() {
            return result;
        }
    }


}
