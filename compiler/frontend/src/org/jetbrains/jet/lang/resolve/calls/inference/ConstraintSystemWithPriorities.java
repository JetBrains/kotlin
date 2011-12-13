package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.types.checker.TypingConstraints;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintType.PARAMETER_BOUND;

/**
 * @author abreslav
 */
public class ConstraintSystemWithPriorities implements ConstraintSystem {

    public static final Comparator<SubtypingConstraint> SUBTYPING_CONSTRAINT_ORDER = new Comparator<SubtypingConstraint>() {
        @Override
        public int compare(SubtypingConstraint o1, SubtypingConstraint o2) {
            return o1.getType().compareTo(o2.getType());
        }
    };

    private static class LoopInTypeVariableConstraintsException extends RuntimeException {
        public LoopInTypeVariableConstraintsException() {}
    }

    //==========================================================================================================================================================

    private final Map<JetType, TypeValue> knownTypes = Maps.newLinkedHashMap(); // linked - for easier debugging
    private final Map<TypeParameterDescriptor, TypeValue> unknownTypes = Maps.newLinkedHashMap(); // linked - for easier debugging
    private final Set<TypeValue> unsolvedUnknowns = Sets.newLinkedHashSet(); // linked - for easier debugging
    private final PriorityQueue<SubtypingConstraint> constraintQueue = new PriorityQueue<SubtypingConstraint>(10, SUBTYPING_CONSTRAINT_ORDER);

    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
    private final TypeCheckingProcedure constraintExpander;
    private final ConstraintResolutionListener listener;

    public ConstraintSystemWithPriorities(ConstraintResolutionListener listener) {
        this.listener = listener;
        this.constraintExpander = createConstraintExpander();
    }

    @NotNull
    private TypeValue getTypeValueFor(@NotNull JetType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;
            // Checking that this is not a T?, but exactly T
            if (typeParameterDescriptor.getDefaultType().isNullable() == type.isNullable()) {
                TypeValue unknownType = unknownTypes.get(typeParameterDescriptor);
                if (unknownType != null) {
                    return unknownType;
                }
            }
        }

        TypeValue typeValue = knownTypes.get(type);
        if (typeValue == null) {
            typeValue = new TypeValue(type);
            knownTypes.put(type, typeValue);
        }
        return typeValue;
    }

    @Override
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        assert !unknownTypes.containsKey(typeParameterDescriptor);
        TypeValue typeValue = new TypeValue(typeParameterDescriptor, positionVariance);
        unknownTypes.put(typeParameterDescriptor, typeValue);
        unsolvedUnknowns.add(typeValue);
    }

    @NotNull
    private TypeValue getTypeVariable(TypeParameterDescriptor typeParameterDescriptor) {
        TypeValue unknownType = unknownTypes.get(typeParameterDescriptor);
        if (unknownType == null) {
            throw new IllegalArgumentException("This type parameter is not an unknown in this constraint system: " + typeParameterDescriptor);
        }
        return unknownType;
    }

    @Override
    public void addSubtypingConstraint(@NotNull SubtypingConstraint constraint) {
        constraintQueue.add(constraint);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constraint expansion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TypeCheckingProcedure createConstraintExpander() {
        return new TypeCheckingProcedure(new TypeConstraintBuilderAdapter(new TypingConstraints() {
            @Override
            public boolean assertEqualTypes(@NotNull JetType a, @NotNull JetType b, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
                TypeValue aValue = getTypeValueFor(a);
                TypeValue bValue = getTypeValueFor(b);

                return expandEqualityConstraint(aValue, bValue);
            }

            @Override
            public boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
                return a.equals(b)
                       || unknownTypes.containsKey(a.getDeclarationDescriptor())
                       || unknownTypes.containsKey(b.getDeclarationDescriptor());
            }

            @Override
            public boolean assertSubtype(@NotNull JetType subtype, @NotNull JetType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
                TypeValue subtypeValue = getTypeValueFor(subtype);
                TypeValue supertypeValue = getTypeValueFor(supertype);

                if (someUnknown(subtypeValue, supertypeValue)) {
                    expandSubtypingConstraint(subtypeValue, supertypeValue);
                }
                return true; // For known types further expansion happens automatically
            }

            @Override
            public boolean noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
                // If some of the types is an unknown, the constraint must be generated, and we should carry on
                // otherwise there can be no solution, and we should fail
                TypeValue subTypeValue = getTypeValueFor(subtype);
                TypeValue superTypeValue = getTypeValueFor(supertype);
                boolean someUnknown = someUnknown(subTypeValue, superTypeValue);
                if (someUnknown) {
                    expandSubtypingConstraint(subTypeValue, superTypeValue);
                }
                return someUnknown;
            }

            private boolean someUnknown(TypeValue subtypeValue, TypeValue supertypeValue) {
                return !subtypeValue.isKnown() || !supertypeValue.isKnown();
            }

        }, listener));
    }

    private boolean assignValueTo(TypeValue unknown, JetType value) {
        if (unknown.hasValue()) {
            // If we have already assigned a value to this unknown,
            // it is a conflict to assign another one, unless this new one is equal to the previous
            return TypeUtils.equalTypes(unknown.getType(), value);
        }
        unsolvedUnknowns.remove(unknown);
        unknown.setValue(value);
        return true;
    }

    private boolean mergeUnknowns(@NotNull TypeValue a, @NotNull TypeValue b) {
        assert !a.isKnown() && !b.isKnown();
        listener.error("!!!mergeUnknowns() is not implemented!!!");
        return false;
    }

    public boolean expandEqualityConstraint(TypeValue a, TypeValue b) {
        if (a.isKnown() && b.isKnown()) {
            return constraintExpander.equalTypes(a.getType(), b.getType());            
        }

        // At least one of them is unknown
        if (a.isKnown()) {
            TypeValue tmp = a;
            a = b;
            b = tmp;
        }

        // Now a is definitely unknown
        if (b.isKnown()) {
            return assignValueTo(a, b.getType());
        }

        // They are both unknown
        return mergeUnknowns(a, b);
    }

    private boolean expandSubtypingConstraint(TypeValue lower, TypeValue upper) {
        listener.log("Constraint added: ", lower, " :< ", upper);

        if (lower == upper) return true;

        // Remember for a later check
        lower.addUpperBound(upper);
        upper.addLowerBound(lower);

        if (lower.isKnown() && upper.isKnown()) {
            // Two known types: expand constraints
            return constraintExpander.isSubtypeOf(lower.getType(), upper.getType());
        }
        else if (!lower.isKnown() && !upper.isKnown()) {
            // Two unknown types: merge them into one variable
            return mergeUnknowns(lower, upper);
        }
        else {
            // One unknown and one known
            if (upper.isKnown()) {
                if (TypeUtils.canHaveSubtypes(typeChecker, upper.getType())) {
                    // Upper bound is final -> we have to equate the lower bounds to it
                    return expandEqualityConstraint(lower, upper);
                }
                if (lower.getLowerBounds().contains(upper)) {
                    // upper :< lower :< upper
                    return expandEqualityConstraint(lower, upper);
                }
            }
            else {
                if (upper.getUpperBounds().contains(lower)) {
                    // lower :< upper :< lower
                    return expandEqualityConstraint(lower, upper);
                }
            }
        }
        return true;
    }

    @Override
    @NotNull
    public ConstraintSystemSolution solve() {
        Solution solution = new Solution();
        // At this point we only have type values, no bounds added for them, no values computed for unknown types

        // Generate low-priority constraints from bounds
        for (Map.Entry<TypeParameterDescriptor, TypeValue> entry : Sets.newHashSet(unknownTypes.entrySet())) {
            TypeParameterDescriptor typeParameterDescriptor = entry.getKey();
            TypeValue typeValue = entry.getValue();
            for (JetType upperBound : typeParameterDescriptor.getUpperBounds()) {
                addSubtypingConstraint(PARAMETER_BOUND.assertSubtyping(typeValue.getOriginalType(), getTypeValueFor(upperBound).getOriginalType()));
            }
            for (JetType lowerBound : typeParameterDescriptor.getLowerBounds()) {
                addSubtypingConstraint(PARAMETER_BOUND.assertSubtyping(getTypeValueFor(lowerBound).getOriginalType(), typeValue.getOriginalType()));
            }
        }

        // Expand and solve constraints
        while (!constraintQueue.isEmpty()) {
            SubtypingConstraint constraint = constraintQueue.poll();

            // Apply constraint
            TypeValue lower = getTypeValueFor(constraint.getSubtype());
            TypeValue upper = getTypeValueFor(constraint.getSupertype());
            boolean success = expandSubtypingConstraint(lower, upper);
            if (!success) {
                solution.registerError(constraint.getErrorMessage());
                break;
            }

            // (???) Propagate info

            // Any unknowns left?
            if (unsolvedUnknowns.isEmpty()) break;
        }

        // Now, let's check the rest of the constraints

        // Expand custom bounds, e.g. List<T> <: List<Int>
//        for (Map.Entry<JetType, TypeValue> entry : Sets.newHashSet(knownTypes.entrySet())) {
//            JetType jetType = entry.getKey();
//            TypeValue typeValue = entry.getValue();
//
//            for (TypeValue upperBound : typeValue.getUpperBounds()) {
//                if (upperBound.isKnown()) {
//                    boolean ok = constraintExpander.isSubtypeOf(jetType, upperBound.getType());
//                    if (!ok) {
//                        listener.error("Error while expanding '" + jetType + " :< " + upperBound.getType() + "'");
//                        return new Solution().registerError("Mismatch while expanding constraints");
//                    }
//                }
//            }
//
//            // Lower bounds. Probably not needed for known types, as everything is symmetrical anyway
//        }

        // effective bounds for each node
//        Set<TypeValue> visited = Sets.newHashSet();
//        for (TypeValue unknownType : unknownTypes.values()) {
//            transitiveClosure(unknownType, visited);
//        }

        for (TypeValue unknownType : unknownTypes.values()) {
            listener.constraintsForUnknown(unknownType.getTypeParameterDescriptor(), unknownType);
        }
        for (TypeValue knownType : knownTypes.values()) {
            listener.constraintsForKnownType(knownType.getType(), knownType);
        }

        // Find inconsistencies

        // Compute values and check that all bounds are respected by solutions:
        // we have set some of them from equality constraints with known types
        // and thus the bounds may be violated if some of the constraints conflict
        for (TypeValue unknownType : unknownTypes.values()) {
            check(unknownType, solution);
        }
        for (TypeValue knownType : knownTypes.values()) {
            check(knownType, solution);
        }

        listener.done(solution, unknownTypes.keySet());

        return solution;
    }

    private void transitiveClosure(TypeValue current, Set<TypeValue> visited) {
        if (!visited.add(current)) {
            return;
        }

        for (TypeValue upperBound : Sets.newHashSet(current.getUpperBounds())) {
            if (upperBound.isKnown()) {
                continue;
            }
            transitiveClosure(upperBound, visited);
            Set<TypeValue> upperBounds = upperBound.getUpperBounds();
            for (TypeValue transitiveBound : upperBounds) {
                expandSubtypingConstraint(current, transitiveBound);
            }
        }
    }

    private void check(TypeValue typeValue, Solution solution) {
        try {
            JetType resultingType = typeValue.getType();
            JetType type = solution.getSubstitutor().substitute(resultingType, Variance.INVARIANT); // TODO
            for (TypeValue upperBound : typeValue.getUpperBounds()) {
                JetType boundingType = solution.getSubstitutor().substitute(upperBound.getType(), Variance.INVARIANT);
                if (!typeChecker.isSubtypeOf(type, boundingType)) { // TODO
                    solution.registerError("Constraint violation: " + type + " is not a subtype of " + boundingType);
                    listener.error("Constraint violation: ", type, " :< ", boundingType);
                }
            }
            for (TypeValue lowerBound : typeValue.getLowerBounds()) {
                JetType boundingType = solution.getSubstitutor().substitute(lowerBound.getType(), Variance.INVARIANT);
                if (!typeChecker.isSubtypeOf(boundingType, type)) {
                    solution.registerError("Constraint violation: " + boundingType + " is not a subtype of " + type);
                    listener.error("Constraint violation: ", boundingType, " :< ", type);
                }
            }
        }
        catch (LoopInTypeVariableConstraintsException e) {
            listener.error("Loop detected");
            solution.registerError("[TODO] Loop in constraints");
        }
    }

    private static class Error implements SolutionStatus {

        private final String message;

        private Error(String message) {
            this.message = message;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public class Solution implements ConstraintSystemSolution {
        private final TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(new TypeSubstitutor.TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor) {
                    TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;

                    if (!unknownTypes.containsKey(descriptor)) return null;

                    TypeProjection typeProjection = new TypeProjection(getValue(descriptor));

                    listener.log(descriptor, " |-> ", typeProjection);

                    return typeProjection;
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });

        private SolutionStatus status;

        public Solution() {
            this.status = SolutionStatus.SUCCESS;
        }

        private Solution registerError(String message) {
            status = new Error(message);
            return this;
        }

        @NotNull
        @Override
        public SolutionStatus getStatus() {
            return status;
        }

        @Override
        public JetType getValue(TypeParameterDescriptor typeParameterDescriptor) {
            return getTypeVariable(typeParameterDescriptor).getType();
        }

        @NotNull
        @Override
        public TypeSubstitutor getSubstitutor() {
            return typeSubstitutor;
        }

    }
    private static final class TypeConstraintBuilderAdapter implements TypingConstraints {
        private final TypingConstraints delegate;
        private final ConstraintResolutionListener listener;

        private TypeConstraintBuilderAdapter(TypingConstraints delegate, ConstraintResolutionListener listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public boolean assertEqualTypes(@NotNull JetType a, @NotNull JetType b, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
            boolean result = delegate.assertEqualTypes(a, b, typeCheckingProcedure);
            if (!result) {
                listener.error("-- Failed to equate ", a, " and ", b);
            }
            return result;
        }

        @Override
        public boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
            boolean result = delegate.assertEqualTypeConstructors(a, b);
            if (!result) {
                listener.error("-- Type constructors are not equal: ", a, " and ", b);
            }
            return result;
        }

        @Override
        public boolean assertSubtype(@NotNull JetType subtype, @NotNull JetType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
            boolean result = delegate.assertSubtype(subtype, supertype, typeCheckingProcedure);
            if (!result) {
                listener.error("-- " + subtype + " can't be a subtype of " + supertype);
            }
            return result;
        }

        @Override
        public boolean noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
            boolean result = delegate.noCorrespondingSupertype(subtype, supertype);
            if (!result) {
                listener.error("-- " + subtype + " has no supertype corresponding to " + supertype);
            }
            return result;
        }
    }

}
