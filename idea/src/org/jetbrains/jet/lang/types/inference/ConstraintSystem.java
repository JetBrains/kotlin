package org.jetbrains.jet.lang.types.inference;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class ConstraintSystem {

//    private static final Supplier<Set<TypeValue>> SET_SUPPLIER = new Supplier<Set<TypeValue>>() {
//        @Override
//        public Set<TypeValue> get() {
//            return Sets.newHashSet();
//        }
//    };

    private static class LoopInTypeVariableConstraintsException extends RuntimeException {
        private LoopInTypeVariableConstraintsException() {
        }

        private LoopInTypeVariableConstraintsException(String message) {
            super(message);
        }

        private LoopInTypeVariableConstraintsException(String message, Throwable cause) {
            super(message, cause);
        }

        private LoopInTypeVariableConstraintsException(Throwable cause) {
            super(cause);
        }
    }

    public static abstract class TypeValue {
        private final Set<TypeValue> upperBounds = Sets.newHashSet();
        private final Set<TypeValue> lowerBounds = Sets.newHashSet();

        @NotNull
        public Set<TypeValue> getUpperBounds() {
            return upperBounds;
        }

        @NotNull
        public Set<TypeValue> getLowerBounds() {
            return lowerBounds;
        }

        @Nullable
        public abstract KnownType getValue();
    }

    private static class UnknownType extends TypeValue {

        private final TypeParameterDescriptor typeParameterDescriptor;
        private final Variance positionVariance;
        private KnownType value;
        private boolean beingComputed = false;

        private UnknownType(TypeParameterDescriptor typeParameterDescriptor, Variance positionVariance) {
            this.typeParameterDescriptor = typeParameterDescriptor;
            this.positionVariance = positionVariance;
        }

        @NotNull
        public TypeParameterDescriptor getTypeParameterDescriptor() {
            return typeParameterDescriptor;
        }

        @Override
        public KnownType getValue() {
            if (beingComputed) {
                throw new LoopInTypeVariableConstraintsException();
            }
            if (value == null) {
                JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
                beingComputed = true;
                try {
                    if (positionVariance == Variance.IN_VARIANCE) {
                        // maximal solution
                        throw new UnsupportedOperationException();
                    }
                    else {
                        // minimal solution

                        Set<TypeValue> lowerBounds = getLowerBounds();
                        if (!lowerBounds.isEmpty()) {
                            Set<JetType> types = getTypes(lowerBounds);

                            JetType commonSupertype = typeChecker.commonSupertype(types);
                            for (TypeValue upperBound : getUpperBounds()) {
                                if (!typeChecker.isSubtypeOf(commonSupertype, upperBound.getValue().getType())) {
                                    value = null;
                                }
                            }

                            System.out.println("minimal solution from lowerbounds for " + this + " is " + commonSupertype);
                            value = new KnownType(commonSupertype);
                        }
                        else {
                            Set<TypeValue> upperBounds = getUpperBounds();
                            Set<JetType> types = getTypes(upperBounds);
                            JetType intersect = TypeUtils.intersect(typeChecker, types);

                            value = new KnownType(intersect);
                        }
                    }
                }
                finally {
                    beingComputed = false;
                }
            }

            return value;
        }
        
        private Set<JetType> getTypes(Set<TypeValue> lowerBounds) {
            Set<JetType> types = Sets.newHashSet();
            for (TypeValue lowerBound : lowerBounds) {
                types.add(lowerBound.getValue().getType());
            }
            return types;
        }

        @Override
        public String toString() {
            return "?" + typeParameterDescriptor;
        }

    }

    private static class KnownType extends TypeValue {

        private final JetType type;

        public KnownType(@NotNull JetType type) {
            this.type = type;
        }

        @NotNull
        public JetType getType() {
            return type;
        }

        @Override
        public KnownType getValue() {
            return this;
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }

    private final Map<JetType, KnownType> knownTypes = Maps.newHashMap();
    private final Map<TypeParameterDescriptor, UnknownType> unknownTypes = Maps.newHashMap();
    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;

    @NotNull
    private TypeValue getTypeValueFor(@NotNull JetType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;
            UnknownType unknownType = unknownTypes.get(typeParameterDescriptor);
            if (unknownType != null) {
                return unknownType;
            }
        }

        KnownType typeValue = knownTypes.get(type);
        if (typeValue == null) {
            typeValue = new KnownType(type);
            knownTypes.put(type, typeValue);
        }
        return typeValue;
    }

    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        assert !unknownTypes.containsKey(typeParameterDescriptor);
        UnknownType typeValue = new UnknownType(typeParameterDescriptor, positionVariance);
        unknownTypes.put(typeParameterDescriptor, typeValue);
    }

    @NotNull
    private UnknownType getTypeVariable(TypeParameterDescriptor typeParameterDescriptor) {
        UnknownType unknownType = unknownTypes.get(typeParameterDescriptor);
        if (unknownType == null) {
            throw new IllegalArgumentException("This type parameter is not an unknown in this constraint system");
        }
        return unknownType;
    }

    public void addSubtypingConstraint(JetType lower, JetType upper) {
        TypeValue typeValueForLower = getTypeValueFor(lower);
        TypeValue typeValueForUpper = getTypeValueFor(upper);
        addSubtypingConstraintOnTypeValues(typeValueForLower, typeValueForUpper);
    }

    private void addSubtypingConstraintOnTypeValues(TypeValue typeValueForLower, TypeValue typeValueForUpper) {
        System.out.println(typeValueForLower + " :< " + typeValueForUpper);
        typeValueForLower.getUpperBounds().add(typeValueForUpper);
        typeValueForUpper.getLowerBounds().add(typeValueForLower);
    }

    @NotNull
    public Solution solve() {
        // Expand custom bounds, e.g. List<T> <: List<Int>
        for (Map.Entry<JetType, KnownType> entry : Sets.newHashSet(knownTypes.entrySet())) {
            JetType jetType = entry.getKey();
            KnownType typeValue = entry.getValue();

            for (TypeValue upperBound : typeValue.getUpperBounds()) {
                if (upperBound instanceof KnownType) {
                    KnownType knownBoundType = (KnownType) upperBound;
                    boolean ok = new TypeConstraintExpander().run(jetType, knownBoundType.getType());
                    if (!ok) {
                        return new Solution(true);
                    }
                }
            }

            // Lower bounds?

        }

        // Fill in upper bounds from type parameter bounds
        for (Map.Entry<TypeParameterDescriptor, UnknownType> entry : Sets.newHashSet(unknownTypes.entrySet())) {
            TypeParameterDescriptor typeParameterDescriptor = entry.getKey();
            UnknownType typeValue = entry.getValue();
            for (JetType upperBound : typeParameterDescriptor.getUpperBounds()) {
                addSubtypingConstraintOnTypeValues(typeValue, getTypeValueFor(upperBound));
            }
        }

        // effective bounds for each node
        Set<TypeValue> visited = Sets.newHashSet();
        for (KnownType knownType : knownTypes.values()) {
            transitiveClosure(knownType, visited);
        }
        for (UnknownType unknownType : unknownTypes.values()) {
            transitiveClosure(unknownType, visited);
        }

        // Find inconsistencies
        Solution solution = new Solution(false);

        for (UnknownType unknownType : unknownTypes.values()) {
            check(unknownType, solution);
        }
        for (KnownType knownType : knownTypes.values()) {
            check(knownType, solution);
        }

        return solution;
    }

    private void check(TypeValue typeValue, Solution solution) {
        try {
            KnownType resultingValue = typeValue.getValue();
            JetType type = solution.getSubstitutor().substitute(resultingValue.getType(), Variance.INVARIANT); // TODO
            for (TypeValue upperBound : typeValue.getUpperBounds()) {
                JetType boundingType = solution.getSubstitutor().substitute(upperBound.getValue().getType(), Variance.INVARIANT);
                if (!typeChecker.isSubtypeOf(type, boundingType)) { // TODO
                    solution.registerError();
                    System.out.println("Constraint violation: " + type + " :< " + boundingType);
                }
            }
            for (TypeValue lowerBound : typeValue.getLowerBounds()) {
                JetType boundingType = solution.getSubstitutor().substitute(lowerBound.getValue().getType(), Variance.INVARIANT);
                if (!typeChecker.isSubtypeOf(boundingType, type)) {
                    solution.registerError();
                    System.out.println("Constraint violation: " + boundingType + " :< " + type);
                }
            }
        }
        catch (LoopInTypeVariableConstraintsException e) {
            solution.registerError();
            e.printStackTrace();
        }
    }

    private void transitiveClosure(TypeValue current, Set<TypeValue> visited) {
        if (!visited.add(current)) {
            return;
        }

        for (TypeValue upperBound : Sets.newHashSet(current.getUpperBounds())) {
            transitiveClosure(upperBound, visited);
            Set<TypeValue> upperBounds = upperBound.getUpperBounds();
            for (TypeValue transitiveBound : upperBounds) {
                addSubtypingConstraintOnTypeValues(current, transitiveBound);
            }
        }
    }

    public class Solution {
        private final TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(new TypeSubstitutor.TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor) {
                    TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;
                    System.out.println(descriptor + " |-> " + getValue(descriptor));
                    return new TypeProjection(getValue(descriptor));
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
        private boolean failed;

        public Solution(boolean failed) {
            this.failed = failed;
        }

        public void registerError() {
            failed = true;
        }

        public boolean isSuccessful() {
            return !failed;
        }

        @Nullable
        public JetType getValue(TypeParameterDescriptor typeParameterDescriptor) {
            KnownType value = getTypeVariable(typeParameterDescriptor).getValue();
            return value == null ? null : value.getType();
        }

        public TypeSubstitutor getSubstitutor() {
            return typeSubstitutor;
        }

    }

    private class TypeConstraintExpander extends JetTypeChecker.AbstractTypeCheckingProcedure<Boolean> {
        
        private boolean error = false;

        private StatusAction fail() {
            error = true;
            return StatusAction.ABORT_ALL;
        }

        @Override
        protected StatusAction startForPairOfTypes(@NotNull JetType subtype, @NotNull JetType supertype) {
            return tryToAddConstraint(subtype, supertype);
        }

        private StatusAction tryToAddConstraint(@NotNull JetType subtype, @NotNull JetType supertype) {
            TypeValue subtypeValue = getTypeValueFor(subtype);
            TypeValue supertypeValue = getTypeValueFor(supertype);

            if (someUnknown(subtypeValue, supertypeValue)) {
                addSubtypingConstraintOnTypeValues(subtypeValue, supertypeValue);
            }
            return StatusAction.PROCEED;

//            // both types are known
//            if (typeChecker.isSubtypeOf(subtype, supertype)) {
//                return StatusAction.PROCEED;
//            }
//            return fail();
        }

        private boolean someUnknown(TypeValue subtypeValue, TypeValue supertypeValue) {
            return subtypeValue instanceof UnknownType || supertypeValue instanceof UnknownType;
        }

        @Override
        protected StatusAction noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
            if (someUnknown(getTypeValueFor(subtype), getTypeValueFor(supertype))) {
                return StatusAction.PROCEED;
            }
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
            return !error;
        }
    }

}