package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.*;

import static org.jetbrains.jet.lang.types.Variance.IN_VARIANCE;
import static org.jetbrains.jet.lang.types.Variance.OUT_VARIANCE;

/**
 * @author abreslav
 */
public class JetTypeChecker {

    public static final JetTypeChecker INSTANCE = new JetTypeChecker(null);

    private final Map<TypeConstructor, Set<TypeConstructor>> conversionMap = new HashMap<TypeConstructor, Set<TypeConstructor>>();
    private final JetStandardLibrary standardLibrary;

    public JetTypeChecker(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
    }

    @NotNull
    private Map<TypeConstructor, Set<TypeConstructor>> getConversionMap() {
//        if (conversionMap.size() == 0) {
//            addConversion(standardLibrary.getByte(),
//                    standardLibrary.getShort(),
//                    standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getShort(),
//                    standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getChar(),
//                    standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getInt(),
//                    standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getLong(),
//                    standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//
//            addConversion(standardLibrary.getFloat(),
//                    standardLibrary.getDouble());
//        }
        return conversionMap;
    }

//    private void addConversion(ClassDescriptor actual, ClassDescriptor... convertedTo) {
//        TypeConstructor[] constructors = new TypeConstructor[convertedTo.length];
//        for (int i = 0, convertedToLength = convertedTo.length; i < convertedToLength; i++) {
//            ClassDescriptor classDescriptor = convertedTo[i];
//            constructors[i] = classDescriptor.getTypeConstructor();
//        }
//        conversionMap.put(actual.getTypeConstructor(), new HashSet<TypeConstructor>(Arrays.asList(constructors)));
//    }
//
    @NotNull
    public JetType commonSupertype(@NotNull JetType... types) {
        return commonSupertype(Arrays.asList(types));
    }

    @NotNull
    public JetType commonSupertype(@NotNull Collection<JetType> types) {
        Collection<JetType> typeSet = new HashSet<JetType>(types);
        assert !typeSet.isEmpty();

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        boolean nullable = false;
        for (Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();
            assert type != null;
            if (JetStandardClasses.isNothingOrNullableNothing(type)) {
                iterator.remove();
            }
            nullable |= type.isNullable();
        }

        // Everything deleted => it's Nothing or Nothing?
        if (typeSet.isEmpty()) {
            // TODO : attributes
            return nullable ? JetStandardClasses.getNullableNothingType() : JetStandardClasses.getNothingType();
        }

        if (typeSet.size() == 1) {
            return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map<TypeConstructor, Set<JetType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            Set<JetType> merge = new HashSet<JetType>();
            for (Set<JetType> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map.Entry<TypeConstructor, Set<JetType>> entry = commonSupertypes.entrySet().iterator().next();

        // Reconstructing type arguments if possible
        JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue());
        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    // Raw supertypes are superclasses w/o type arguments
    // @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
    @NotNull
    private Map<TypeConstructor, Set<JetType>> computeCommonRawSupertypes(@NotNull Collection<JetType> types) {
        assert !types.isEmpty();

        final Map<TypeConstructor, Set<JetType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<JetType>>();
        Set<TypeConstructor> commonSuperclasses = null;

        List<TypeConstructor> order = null;
        for (JetType type : types) {
            Set<TypeConstructor> visited = new HashSet<TypeConstructor>();

            order = dfs(type, visited, new DfsNodeHandler<List<TypeConstructor>>() {
                public LinkedList<TypeConstructor> list = new LinkedList<TypeConstructor>();

                @Override
                public void beforeChildren(JetType current) {
                    TypeConstructor constructor = current.getConstructor();

                    Set<JetType> instances = constructorToAllInstances.get(constructor);
                    if (instances == null) {
                        instances = new HashSet<JetType>();
                        constructorToAllInstances.put(constructor, instances);
                    }
                    instances.add(current);
                }

                @Override
                public void afterChildren(JetType current) {
                    list.addFirst(current.getConstructor());
                }

                @Override
                public List<TypeConstructor> result() {
                    return list;
                }
            });

            if (commonSuperclasses == null) {
                commonSuperclasses = visited;
            }
            else {
                commonSuperclasses.retainAll(visited);
            }
        }
        assert order != null;

        Set<TypeConstructor> notSource = new HashSet<TypeConstructor>();
        Map<TypeConstructor, Set<JetType>> result = new HashMap<TypeConstructor, Set<JetType>>();
        for (TypeConstructor superConstructor : order) {
            if (!commonSuperclasses.contains(superConstructor)) {
                continue;
            }

            if (!notSource.contains(superConstructor)) {
                result.put(superConstructor, constructorToAllInstances.get(superConstructor));
                markAll(superConstructor, notSource);
            }
        }

        return result;
    }

    // constructor - type constructor of a supertype to be instantiated
    // types - instantiations of constructor occurring as supertypes of classes we are trying to intersect
    @NotNull
    private JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types) {
        // we assume that all the given types are applications of the same type constructor

        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        List<TypeProjection> newProjections = new ArrayList<TypeProjection>();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameterDescriptor = parameters.get(i);
            Set<TypeProjection> typeProjections = new HashSet<TypeProjection>();
            for (JetType type : types) {
                typeProjections.add(type.getArguments().get(i));
            }
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
        }

        boolean nullable = false;
        for (JetType type : types) {
            nullable |= type.isNullable();
        }

        // TODO : attributes?
        return new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), constructor, nullable, newProjections, JetStandardClasses.STUB); // TODO : scope
    }

    @NotNull
    private TypeProjection computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<TypeProjection> typeProjections) {
        if (typeProjections.size() == 1) {
            return typeProjections.iterator().next();
        }

        Set<JetType> ins = new HashSet<JetType>();
        Set<JetType> outs = new HashSet<JetType>();

        Variance variance = parameterDescriptor.getVariance();
        switch (variance) {
            case INVARIANT:
                // Nothing
                break;
            case IN_VARIANCE:
                outs = null;
                break;
            case OUT_VARIANCE:
                ins = null;
                break;
        }

        for (TypeProjection projection : typeProjections) {
            Variance projectionKind = projection.getProjectionKind();
            if (projectionKind.allowsInPosition()) {
                if (ins != null) {
                    ins.add(projection.getType());
                }
            } else {
                ins = null;
            }

            if (projectionKind.allowsOutPosition()) {
                if (outs != null) {
                    outs.add(projection.getType());
                }
            } else {
                outs = null;
            }
        }

        if (ins != null) {
            JetType intersection = TypeUtils.intersect(this, ins);
            if (intersection == null) {
                if (outs != null) {
                    return new TypeProjection(OUT_VARIANCE, commonSupertype(outs));
                }
                return new TypeProjection(OUT_VARIANCE, commonSupertype(parameterDescriptor.getUpperBounds()));
            }
            Variance projectionKind = variance == IN_VARIANCE ? Variance.INVARIANT : IN_VARIANCE;
            return new TypeProjection(projectionKind, intersection);
        } else if (outs != null) {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(outs));
        } else {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(parameterDescriptor.getUpperBounds()));
        }
    }

    private void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (JetType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }

    private <R> R dfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<R> handler) {
        doDfs(current, visited, handler);
        return handler.result();
    }

    private void doDfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<?> handler) {
        if (!visited.add(current.getConstructor())) {
            return;
        }
        handler.beforeChildren(current);
//        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(current);
        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
        for (JetType supertype : current.getConstructor().getSupertypes()) {
            TypeConstructor supertypeConstructor = supertype.getConstructor();
            if (visited.contains(supertypeConstructor)) {
                continue;
            }
            JetType substitutedSupertype = substitutor.safeSubstitute(supertype, Variance.INVARIANT);
            dfs(substitutedSupertype, visited, handler);
        }
        handler.afterChildren(current);
    }

    public boolean isConvertibleTo(@NotNull JetType actual, @NotNull JetType expected) {
        return isSubtypeOf(actual, expected) ||
               isConvertibleBySpecialConversion(actual, expected);
    }

    public boolean isConvertibleBySpecialConversion(@NotNull JetType actual, @NotNull JetType expected) {
        if (expected.getConstructor().equals(JetStandardClasses.getTuple(0).getTypeConstructor())) {
            return true;
        }
//        if (actual.getValueArguments().isEmpty()) {
//            TypeConstructor actualConstructor = actual.getConstructor();
//            TypeConstructor constructor = expected.getConstructor();
//            Set<TypeConstructor> convertibleTo = getConversionMap().get(actualConstructor);
//            if (convertibleTo != null) {
//                return convertibleTo.contains(constructor);
//            }
//        }
        return false;
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

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
//        return new TypeCheckingProcedure().run(subtype, supertype);
        return new ExplicitInOutTypeCheckingProcedure().run(subtype, supertype);
    }

    public boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return isSubtypeOf(a, b) && isSubtypeOf(b, a);
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

        private JetType getOutType(TypeParameterDescriptor parameter, TypeProjection subArgument) {
            boolean isOutProjected = subArgument.getProjectionKind() == IN_VARIANCE || parameter.getVariance() == IN_VARIANCE;
            return isOutProjected ? parameter.getBoundsAsType() : subArgument.getType();
        }

        private JetType getInType(TypeParameterDescriptor parameter, TypeProjection subArgument) {
            boolean isOutProjected = subArgument.getProjectionKind() == OUT_VARIANCE || parameter.getVariance() == OUT_VARIANCE;
            return isOutProjected ? JetStandardClasses.getNothingType() : subArgument.getType();
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

    private static class DfsNodeHandler<R> {

        public void beforeChildren(JetType current) {

        }

        public void afterChildren(JetType current) {

        }

        public R result() {
            return null;
        }
    }

}
