/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.calls.inference;

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.types.checker.TypingConstraints;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.UtilsPackage;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.EQUAL;
import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.SUB_TYPE;
import static org.jetbrains.jet.lang.resolve.calls.inference.TypeBounds.Bound;
import static org.jetbrains.jet.lang.resolve.calls.inference.TypeBounds.BoundKind.*;
import static org.jetbrains.jet.lang.types.TypeUtils.DONT_CARE;

public class ConstraintSystemImpl implements ConstraintSystem {

    public enum ConstraintKind {
        SUB_TYPE, EQUAL
    }

    private final Map<TypeParameterDescriptor, TypeBoundsImpl> typeParameterBounds =
            new LinkedHashMap<TypeParameterDescriptor, TypeBoundsImpl>();
    private final Set<ConstraintPosition> errorConstraintPositions = new HashSet<ConstraintPosition>();
    private boolean hasErrorInConstrainingTypes;

    private final ConstraintSystemStatus constraintSystemStatus = new ConstraintSystemStatus() {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        @Override
        public boolean isSuccessful() {
            return !hasContradiction() && !hasUnknownParameters();
        }

        @Override
        public boolean hasContradiction() {
            return hasTypeConstructorMismatch() || hasConflictingConstraints();
        }

        @Override
        public boolean hasViolatedUpperBound() {
            if (isSuccessful()) return false;
            return getSystemWithoutWeakConstraints().getStatus().isSuccessful();
        }

        @Override
        public boolean hasConflictingConstraints() {
            for (TypeBoundsImpl typeBounds : typeParameterBounds.values()) {
                if (typeBounds.getValues().size() > 1) return true;
            }
            return false;
        }

        @Override
        public boolean hasUnknownParameters() {
            for (TypeBoundsImpl typeBounds : typeParameterBounds.values()) {
                if (typeBounds.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean hasTypeConstructorMismatch() {
            return !errorConstraintPositions.isEmpty();
        }

        @Override
        public boolean hasTypeConstructorMismatchAt(@NotNull ConstraintPosition constraintPosition) {
            return errorConstraintPositions.contains(constraintPosition);
        }

        @Override
        public boolean hasOnlyErrorsFromPosition(ConstraintPosition constraintPosition) {
            if (isSuccessful()) return false;
            ConstraintSystem systemWithoutConstraintsFromPosition = filterConstraintsOut(constraintPosition);
            if (systemWithoutConstraintsFromPosition.getStatus().isSuccessful()) {
                return true;
            }
            if (errorConstraintPositions.size() == 1 && errorConstraintPositions.contains(constraintPosition)) {
                // e.g. if systemWithoutConstraintsFromPosition has unknown type parameters, it's not successful
                return true;
            }
            return false;
        }

        @Override
        public boolean hasErrorInConstrainingTypes() {
            return hasErrorInConstrainingTypes;
        }
    };

    @NotNull
    private static Map<TypeParameterDescriptor, TypeProjection> getParameterToInferredValueMap(
            @NotNull Map<TypeParameterDescriptor, TypeBoundsImpl> typeParameterBounds,
            @NotNull Function1<TypeParameterDescriptor, TypeProjection> getDefaultTypeProjection
    ) {
        Map<TypeParameterDescriptor, TypeProjection> substitutionContext =
                UtilsPackage.newHashMapWithExpectedSize(typeParameterBounds.size());
        for (Map.Entry<TypeParameterDescriptor, TypeBoundsImpl> entry : typeParameterBounds.entrySet()) {
            TypeParameterDescriptor typeParameter = entry.getKey();
            TypeBounds typeBounds = entry.getValue();

            TypeProjection typeProjection;
            JetType value = typeBounds.getValue();
            if (value != null && !TypeUtils.containsSpecialType(value, TypeUtils.DONT_CARE)) {
                typeProjection = new TypeProjectionImpl(value);
            }
            else {
                typeProjection = getDefaultTypeProjection.invoke(typeParameter);
            }
            substitutionContext.put(typeParameter, typeProjection);
        }
        return substitutionContext;
    }

    private TypeSubstitutor replaceUninferredBy(@NotNull Function1<TypeParameterDescriptor, TypeProjection> getDefaultValue) {
        return TypeUtils.makeSubstitutorForTypeParametersMap(getParameterToInferredValueMap(typeParameterBounds, getDefaultValue));
    }

    private TypeSubstitutor replaceUninferredBy(@NotNull final JetType defaultValue) {
        return replaceUninferredBy(
                new Function1<TypeParameterDescriptor, TypeProjection>() {
                    @Override
                    public TypeProjection invoke(TypeParameterDescriptor descriptor) {
                        return new TypeProjectionImpl(defaultValue);
                    }
                }
        );
    }

    private TypeSubstitutor replaceUninferredBySpecialErrorType() {
        return replaceUninferredBy(
                new Function1<TypeParameterDescriptor, TypeProjection>() {
                    @Override
                    public TypeProjection invoke(TypeParameterDescriptor descriptor) {
                        return new TypeProjectionImpl(ErrorUtils.createUninferredParameterType(descriptor));
                    }
                }
        );
    }

    @NotNull
    @Override
    public ConstraintSystemStatus getStatus() {
        return constraintSystemStatus;
    }

    @Override
    public void registerTypeVariables(@NotNull Map<TypeParameterDescriptor, Variance> typeVariables) {
        for (Map.Entry<TypeParameterDescriptor, Variance> entry : typeVariables.entrySet()) {
            TypeParameterDescriptor typeVariable = entry.getKey();
            Variance positionVariance = entry.getValue();
            typeParameterBounds.put(typeVariable, new TypeBoundsImpl(typeVariable, positionVariance));
        }
        TypeSubstitutor constantSubstitutor = TypeUtils.makeConstantSubstitutor(typeParameterBounds.keySet(), DONT_CARE);
        for (Map.Entry<TypeParameterDescriptor, TypeBoundsImpl> entry : typeParameterBounds.entrySet()) {
            TypeParameterDescriptor typeVariable = entry.getKey();
            TypeBoundsImpl typeBounds = entry.getValue();

            for (JetType declaredUpperBound : typeVariable.getUpperBounds()) {
                if (KotlinBuiltIns.getInstance().getNullableAnyType().equals(declaredUpperBound)) continue; //todo remove this line (?)
                JetType substitutedBound = constantSubstitutor.substitute(declaredUpperBound, Variance.INVARIANT);
                if (substitutedBound != null) {
                    typeBounds.addBound(UPPER_BOUND, substitutedBound, ConstraintPosition.getTypeBoundPosition(typeVariable.getIndex()));
                }
            }
        }
    }

    @Override
    @NotNull
    public ConstraintSystem copy() {
        return createNewConstraintSystemFromThis(
                UtilsPackage.<TypeParameterDescriptor>identity(),
                new Function1<TypeBoundsImpl, TypeBoundsImpl>() {
                    @Override
                    public TypeBoundsImpl invoke(TypeBoundsImpl typeBounds) {
                        return typeBounds.copy();
                    }
                },
                UtilsPackage.<ConstraintPosition>alwaysTrue()
        );
    }

    @NotNull
    public ConstraintSystem substituteTypeVariables(@NotNull Function1<TypeParameterDescriptor, TypeParameterDescriptor> typeVariablesMap) {
        return createNewConstraintSystemFromThis(
                typeVariablesMap,
                // type bounds are proper types and don't contain other variables
                UtilsPackage.<TypeBoundsImpl>identity(),
                UtilsPackage.<ConstraintPosition>alwaysTrue()
        );
    }

    @NotNull
    public ConstraintSystem filterConstraintsOut(@NotNull final ConstraintPosition excludePosition) {
        return filterConstraints(new Function1<ConstraintPosition, Boolean>() {
            @Override
            public Boolean invoke(ConstraintPosition constraintPosition) {
                return !excludePosition.equals(constraintPosition);
            }
        });
    }

    @NotNull
    private ConstraintSystem filterConstraints(@NotNull final Function1<ConstraintPosition, Boolean> condition) {
        return createNewConstraintSystemFromThis(
                UtilsPackage.<TypeParameterDescriptor>identity(),
                new Function1<TypeBoundsImpl, TypeBoundsImpl>() {
                    @Override
                    public TypeBoundsImpl invoke(TypeBoundsImpl typeBounds) {
                        return typeBounds.filter(condition);
                    }
                },
                condition
        );
    }

    @NotNull
    public ConstraintSystem getSystemWithoutWeakConstraints() {
        return filterConstraints(new Function1<ConstraintPosition, Boolean>() {
            @Override
            public Boolean invoke(ConstraintPosition constraintPosition) {
                // 'isStrong' for compound means 'has some strong constraints'
                // but for testing absence of weak constraints we need 'has only strong constraints' here
                if (constraintPosition instanceof ConstraintPosition.CompoundConstraintPosition) {
                    ConstraintPosition.CompoundConstraintPosition position =
                            (ConstraintPosition.CompoundConstraintPosition) constraintPosition;
                    return position.consistsOfOnlyStrongConstraints();
                }
                return constraintPosition.isStrong();
            }
        });
    }

    @NotNull
    private ConstraintSystem createNewConstraintSystemFromThis(
            @NotNull Function1<TypeParameterDescriptor, TypeParameterDescriptor> substituteTypeVariable,
            @NotNull Function1<TypeBoundsImpl, TypeBoundsImpl> replaceTypeBounds,
            @NotNull Function1<ConstraintPosition, Boolean> filterConstraintPosition
    ) {
        ConstraintSystemImpl newSystem = new ConstraintSystemImpl();
        for (Map.Entry<TypeParameterDescriptor, TypeBoundsImpl> entry : typeParameterBounds.entrySet()) {
            TypeParameterDescriptor typeParameter = entry.getKey();
            TypeBoundsImpl typeBounds = entry.getValue();

            TypeParameterDescriptor newTypeParameter = substituteTypeVariable.invoke(typeParameter);
            assert newTypeParameter != null;
            newSystem.typeParameterBounds.put(newTypeParameter, replaceTypeBounds.invoke(typeBounds));
        }
        newSystem.errorConstraintPositions.addAll(KotlinPackage.filter(errorConstraintPositions, filterConstraintPosition));
        //todo if 'filterConstraintPosition' is not trivial, it's incorrect to just copy 'hasErrorInConstrainingTypes'
        newSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes;
        return newSystem;
    }

    @Override
    public void addSupertypeConstraint(
            @Nullable JetType constrainingType,
            @NotNull JetType subjectType,
            @NotNull ConstraintPosition constraintPosition
    ) {
        if (constrainingType != null && TypeUtils.noExpectedType(constrainingType)) return;

        addConstraint(SUB_TYPE, subjectType, constrainingType, constraintPosition);
    }

    @Override
    public void addSubtypeConstraint(
            @Nullable JetType constrainingType,
            @NotNull JetType subjectType,
            @NotNull ConstraintPosition constraintPosition
    ) {
        addConstraint(SUB_TYPE, constrainingType, subjectType, constraintPosition);
    }

    private void addConstraint(
            @NotNull ConstraintKind constraintKind,
            @Nullable JetType subType,
            @Nullable JetType superType,
            @NotNull final ConstraintPosition constraintPosition
    ) {
        TypeCheckingProcedure typeCheckingProcedure = new TypeCheckingProcedure(new TypingConstraints() {
            @Override
            public boolean assertEqualTypes(
                    @NotNull JetType a, @NotNull JetType b, @NotNull TypeCheckingProcedure typeCheckingProcedure
            ) {
                doAddConstraint(EQUAL, a, b, constraintPosition, typeCheckingProcedure);
                return true;

            }

            @Override
            public boolean assertEqualTypeConstructors(
                    @NotNull TypeConstructor a, @NotNull TypeConstructor b
            ) {
                return a.equals(b);
            }

            @Override
            public boolean assertSubtype(
                    @NotNull JetType subtype, @NotNull JetType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure
            ) {
                doAddConstraint(SUB_TYPE, subtype, supertype, constraintPosition, typeCheckingProcedure);
                return true;
            }

            @Override
            public boolean noCorrespondingSupertype(
                    @NotNull JetType subtype, @NotNull JetType supertype
            ) {
                errorConstraintPositions.add(constraintPosition);
                return true;
            }
        });
        doAddConstraint(constraintKind, subType, superType, constraintPosition, typeCheckingProcedure);
    }

    private boolean isErrorOrSpecialType(@Nullable JetType type) {
        if (type == DONT_CARE || ErrorUtils.isUninferredParameter(type)) {
            return true;
        }

        if (type == null || (type.isError() && type != TypeUtils.PLACEHOLDER_FUNCTION_TYPE)) {
            hasErrorInConstrainingTypes = true;
            return true;
        }
        return false;
    }

    private void doAddConstraint(
            @NotNull ConstraintKind constraintKind,
            @Nullable JetType subType,
            @Nullable JetType superType,
            @NotNull ConstraintPosition constraintPosition,
            @NotNull TypeCheckingProcedure typeCheckingProcedure
    ) {

        if (isErrorOrSpecialType(subType) || isErrorOrSpecialType(superType)) return;
        assert subType != null && superType != null;

        assert superType != TypeUtils.PLACEHOLDER_FUNCTION_TYPE : "The type for " + constraintPosition + " shouldn't be a placeholder for function type";

        KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();
        if (subType == TypeUtils.PLACEHOLDER_FUNCTION_TYPE) {
            if (!kotlinBuiltIns.isFunctionOrExtensionFunctionType(superType)) {
                if (isMyTypeVariable(superType)) {
                    // a constraint binds type parameter and any function type, so there is no new info and no error
                    return;
                }
                errorConstraintPositions.add(constraintPosition);
            }
            return;
        }

        // todo temporary hack
        // function literal without declaring receiver type { x -> ... }
        // can be considered as extension function if one is expected
        // (special type constructor for function/ extension function should be introduced like PLACEHOLDER_FUNCTION_TYPE)
        if (constraintKind == SUB_TYPE && kotlinBuiltIns.isFunctionType(subType) && kotlinBuiltIns.isExtensionFunctionType(superType)) {
            subType = createCorrespondingExtensionFunctionType(subType, DONT_CARE);
        }

        // can be equal for the recursive invocations:
        // fun <T> foo(i: Int) : T { ... return foo(i); } => T <: T
        if (subType.equals(superType)) return;

        assert !isMyTypeVariable(subType) || !isMyTypeVariable(superType) :
                "The constraint shouldn't contain different type variables on both sides: " + subType + " <: " + superType;


        if (isMyTypeVariable(subType)) {
            generateTypeParameterConstraint(subType, superType, constraintKind == SUB_TYPE ? UPPER_BOUND : EXACT_BOUND, constraintPosition);
            return;
        }
        if (isMyTypeVariable(superType)) {
            generateTypeParameterConstraint(superType, subType, constraintKind == SUB_TYPE ? LOWER_BOUND : EXACT_BOUND, constraintPosition);
            return;
        }
        // if superType is nullable and subType is not nullable, unsafe call error will be generated later,
        // but constraint system should be solved anyway
        typeCheckingProcedure.isSubtypeOf(TypeUtils.makeNotNullable(subType), TypeUtils.makeNotNullable(superType));
    }

    private void generateTypeParameterConstraint(
            @NotNull JetType parameterType,
            @NotNull JetType constrainingType,
            @NotNull TypeBoundsImpl.BoundKind boundKind,
            @NotNull ConstraintPosition constraintPosition
    ) {
        TypeBoundsImpl typeBounds = getTypeBounds(parameterType);
        assert typeBounds != null : "constraint should be generated only for type variables";

        if (!parameterType.isNullable() || !constrainingType.isNullable()) {
            typeBounds.addBound(boundKind, constrainingType, constraintPosition);
            return;
        }
        // For parameter type T:
        // constraint T? =  Int? should transform to T >: Int and T <: Int?
        // constraint T? >: Int? should transform to T >: Int
        JetType notNullConstrainingType = TypeUtils.makeNotNullable(constrainingType);
        if (boundKind == EXACT_BOUND || boundKind == LOWER_BOUND) {
            typeBounds.addBound(LOWER_BOUND, notNullConstrainingType, constraintPosition);
        }
        // constraint T? <: Int? should transform to T <: Int?
        if (boundKind == EXACT_BOUND || boundKind == UPPER_BOUND) {
            typeBounds.addBound(UPPER_BOUND, constrainingType, constraintPosition);
        }
    }

    public void processDeclaredBoundConstraints() {
        for (Map.Entry<TypeParameterDescriptor, TypeBoundsImpl> entry : typeParameterBounds.entrySet()) {
            TypeParameterDescriptor typeParameterDescriptor = entry.getKey();
            TypeBoundsImpl typeBounds = entry.getValue();
            for (JetType declaredUpperBound : typeParameterDescriptor.getUpperBounds()) {
                //todo order matters here
                Collection<Bound> bounds = new ArrayList<Bound>(typeBounds.getBounds());
                for (Bound bound : bounds) {
                    if (bound.kind == LOWER_BOUND || bound.kind == EXACT_BOUND) {
                        ConstraintPosition position = ConstraintPosition.getCompoundConstraintPosition(
                                ConstraintPosition.getTypeBoundPosition(typeParameterDescriptor.getIndex()), bound.position);
                        addSubtypeConstraint(bound.type, declaredUpperBound, position);
                    }
                }
                ClassifierDescriptor declarationDescriptor = declaredUpperBound.getConstructor().getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor && typeParameterBounds.containsKey(declarationDescriptor)) {
                    TypeBoundsImpl typeBoundsForUpperBound = typeParameterBounds.get(declarationDescriptor);
                    for (Bound bound : typeBoundsForUpperBound.getBounds()) {
                        if (bound.kind == UPPER_BOUND || bound.kind == EXACT_BOUND) {
                            ConstraintPosition position = ConstraintPosition.getCompoundConstraintPosition(
                                    ConstraintPosition.getTypeBoundPosition(typeParameterDescriptor.getIndex()), bound.position);
                            typeBounds.addBound(UPPER_BOUND, bound.type, position);
                        }
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public Set<TypeParameterDescriptor> getTypeVariables() {
        return typeParameterBounds.keySet();
    }

    @Override
    @NotNull
    public TypeBounds getTypeBounds(@NotNull TypeParameterDescriptor typeVariable) {
        TypeBoundsImpl typeBounds = typeParameterBounds.get(typeVariable);
        assert typeBounds != null : "TypeParameterDescriptor is not a type variable for constraint system: " + typeVariable;
        return typeBounds;
    }

    @Nullable
    private TypeBoundsImpl getTypeBounds(@NotNull JetType type) {
        ClassifierDescriptor parameterDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (parameterDescriptor instanceof TypeParameterDescriptor) {
            return typeParameterBounds.get(parameterDescriptor);
        }
        return null;
    }

    private boolean isMyTypeVariable(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor instanceof TypeParameterDescriptor && typeParameterBounds.get(descriptor) != null;
    }

    @NotNull
    @Override
    public TypeSubstitutor getResultingSubstitutor() {
        return replaceUninferredBySpecialErrorType();
    }

    @NotNull
    @Override
    public TypeSubstitutor getCurrentSubstitutor() {
        return replaceUninferredBy(TypeUtils.DONT_CARE);
    }

    @NotNull
    public static JetType createCorrespondingExtensionFunctionType(@NotNull JetType functionType, @NotNull JetType receiverType) {
        assert KotlinBuiltIns.getInstance().isFunctionType(functionType);

        List<TypeProjection> typeArguments = functionType.getArguments();
        assert !typeArguments.isEmpty();

        // excluding the last type argument of the function type, which is the return type
        int index = 0;
        int lastIndex = typeArguments.size() - 1;
        List<JetType> arguments = new ArrayList<JetType>(lastIndex);
        for (TypeProjection typeArgument : typeArguments) {
            if (index < lastIndex) {
                arguments.add(typeArgument.getType());
            }
            index++;
        }
        JetType returnType = typeArguments.get(lastIndex).getType();
        return KotlinBuiltIns.getInstance().getFunctionType(functionType.getAnnotations(), receiverType, arguments, returnType);
    }
}
