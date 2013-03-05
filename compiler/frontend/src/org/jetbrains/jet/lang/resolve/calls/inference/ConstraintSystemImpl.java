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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.types.checker.TypingConstraints;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.*;
import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.EQUAL;
import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.SUB_TYPE;
import static org.jetbrains.jet.lang.resolve.calls.inference.TypeConstraintsImpl.BoundKind;
import static org.jetbrains.jet.lang.resolve.calls.inference.TypeConstraintsImpl.BoundKind.*;

public class ConstraintSystemImpl implements ConstraintSystem {

    public enum ConstraintKind {
        SUB_TYPE, EQUAL
    }

    private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
    private final Set<ConstraintPosition> errorConstraintPositions = Sets.newHashSet();
    private final TypeSubstitutor resultingSubstitutor;
    private final TypeSubstitutor currentSubstitutor;
    private boolean hasErrorInConstrainingTypes;
    private boolean hasExpectedTypeMismatch;

    public ConstraintSystemImpl() {
        this.resultingSubstitutor = createTypeSubstitutorWithDefaultForUnknownTypeParameter(new TypeProjection(CANT_INFER_TYPE_PARAMETER));
        this.currentSubstitutor = createTypeSubstitutorWithDefaultForUnknownTypeParameter(new TypeProjection(DONT_CARE));
    }

    private TypeSubstitutor createTypeSubstitutorWithDefaultForUnknownTypeParameter(@Nullable final TypeProjection defaultTypeProjection) {
        return TypeSubstitutor.create(new TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor) {
                    TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;

                    JetType value = ConstraintsUtil.getValue(getTypeConstraints(descriptor));
                    if (value != null && !TypeUtils.dependsOnTypeConstructors(value, Collections.singleton(
                            DONT_CARE.getConstructor()))) {
                        return new TypeProjection(value);
                    }
                    if (typeParameterConstraints.containsKey(descriptor)) {
                        return defaultTypeProjection;
                    }
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public String toString() {
                return typeParameterConstraints.toString();
            }
        });
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
    public boolean hasExpectedTypeMismatch() {
        return hasExpectedTypeMismatch
               || (errorConstraintPositions.size() == 1 && errorConstraintPositions.contains(ConstraintPosition.EXPECTED_TYPE_POSITION));
    }

    public void setHasExpectedTypeMismatch() {
        hasExpectedTypeMismatch = true;
    }

    @Override
    public boolean hasErrorInConstrainingTypes() {
        return hasErrorInConstrainingTypes;
    }

    @Override
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeVariable, @NotNull Variance positionVariance) {
        typeParameterConstraints.put(typeVariable, new TypeConstraintsImpl(positionVariance));
    }

    @Override
    @NotNull
    public ConstraintSystem copy() {
        ConstraintSystemImpl newConstraintSystem = new ConstraintSystemImpl();
        for (Map.Entry<TypeParameterDescriptor, TypeConstraintsImpl> entry : typeParameterConstraints.entrySet()) {
            TypeParameterDescriptor typeParameter = entry.getKey();
            TypeConstraintsImpl typeConstraints = entry.getValue();
            newConstraintSystem.typeParameterConstraints.put(typeParameter, typeConstraints.copy());
        }
        newConstraintSystem.errorConstraintPositions.addAll(errorConstraintPositions);
        newConstraintSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes;
        return newConstraintSystem;
    }

    @NotNull
    public ConstraintSystem replaceTypeVariables(@NotNull Function<TypeParameterDescriptor, TypeParameterDescriptor> typeVariablesMap) {
        ConstraintSystemImpl newConstraintSystem = new ConstraintSystemImpl();
        for (Map.Entry<TypeParameterDescriptor, TypeConstraintsImpl> entry : typeParameterConstraints.entrySet()) {
            TypeParameterDescriptor typeParameter = entry.getKey();
            TypeConstraintsImpl typeConstraints = entry.getValue();

            TypeParameterDescriptor newTypeParameter = typeVariablesMap.apply(typeParameter);
            assert newTypeParameter != null;
            newConstraintSystem.typeParameterConstraints.put(newTypeParameter, typeConstraints);
        }
        newConstraintSystem.errorConstraintPositions.addAll(errorConstraintPositions);
        newConstraintSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes;
        return newConstraintSystem;
    }

    @Override
    public void addSupertypeConstraint(
            @Nullable JetType constrainingType,
            @NotNull JetType subjectType,
            @NotNull ConstraintPosition constraintPosition
    ) {
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
                throw new IllegalStateException("'assertEqualTypeConstructors' shouldn't be invoked inside 'isSubtypeOf'");
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
        if (type == TypeUtils.NO_EXPECTED_TYPE
            || type == DONT_CARE
            || type == CANT_INFER_TYPE_PARAMETER) {
            return true;
        }

        if (type == null || ((ErrorUtils.isErrorType(type) && type != PLACEHOLDER_FUNCTION_TYPE))) {
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

        assert superType != PLACEHOLDER_FUNCTION_TYPE : "The type for " + constraintPosition + " shouldn't be a placeholder for function type";

        KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();
        if (subType == PLACEHOLDER_FUNCTION_TYPE) {
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
            generateTypeParameterConstraint(subType, superType, constraintKind == SUB_TYPE ? UPPER_BOUND : EXACT_BOUND);
            return;
        }
        if (isMyTypeVariable(superType)) {
            generateTypeParameterConstraint(superType, subType, constraintKind == SUB_TYPE ? LOWER_BOUND : EXACT_BOUND);
            return;
        }
        // if superType is nullable and subType is not nullable, unsafe call error will be generated later,
        // but constraint system should be solved anyway
        typeCheckingProcedure.isSubtypeOf(TypeUtils.makeNotNullable(subType), TypeUtils.makeNotNullable(superType));
    }

    private void generateTypeParameterConstraint(
            @NotNull JetType parameterType,
            @NotNull JetType constrainingType,
            @NotNull BoundKind boundKind
    ) {
        TypeConstraintsImpl typeConstraints = getTypeConstraints(parameterType);
        assert typeConstraints != null : "constraint should be generated only for type variables";

        if (parameterType.isNullable()) {
            // For parameter type T constraint T? <: Int? should transform to T <: Int
            constrainingType = TypeUtils.makeNotNullable(constrainingType);
        }
        typeConstraints.addBound(boundKind, constrainingType);
    }

    public void processDeclaredBoundConstraints() {
        for (Map.Entry<TypeParameterDescriptor, TypeConstraintsImpl> entry : typeParameterConstraints.entrySet()) {
            TypeParameterDescriptor typeParameterDescriptor = entry.getKey();
            TypeConstraintsImpl typeConstraints = entry.getValue();
            for (JetType declaredUpperBound : typeParameterDescriptor.getUpperBounds()) {
                //todo order matters here
                for (JetType lowerOrExactBound : Sets.union(typeConstraints.getLowerBounds(), typeConstraints.getExactBounds())) {
                    addSubtypeConstraint(lowerOrExactBound, declaredUpperBound, ConstraintPosition.BOUND_CONSTRAINT_POSITION);
                }
            }
        }
    }

    @NotNull
    @Override
    public Set<TypeParameterDescriptor> getTypeVariables() {
        return typeParameterConstraints.keySet();
    }

    @Override
    @Nullable
    public TypeConstraints getTypeConstraints(@NotNull TypeParameterDescriptor typeVariable) {
        return typeParameterConstraints.get(typeVariable);
    }

    @Nullable
    private TypeConstraintsImpl getTypeConstraints(@NotNull JetType type) {
        ClassifierDescriptor parameterDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (parameterDescriptor instanceof TypeParameterDescriptor) {
            return typeParameterConstraints.get(parameterDescriptor);
        }
        return null;
    }

    private boolean isMyTypeVariable(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        return descriptor instanceof TypeParameterDescriptor && typeParameterConstraints.get(descriptor) != null;
    }

    @Override
    public boolean isSuccessful() {
        return !hasContradiction() && !hasUnknownParameters();
    }

    @Override
    public boolean hasContradiction() {
        return hasTypeConstructorMismatch() || hasConflictingConstraints();
    }

    @Override
    public boolean hasConflictingConstraints() {
        for (TypeParameterDescriptor typeParameter : typeParameterConstraints.keySet()) {
            TypeConstraints typeConstraints = getTypeConstraints(typeParameter);
            if (typeConstraints != null && ConstraintsUtil.getValues(typeConstraints).size() > 1) return true;
        }
        return false;
    }

    @Override
    public boolean hasUnknownParameters() {
        for (TypeConstraintsImpl constraints : typeParameterConstraints.values()) {
            if (constraints.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public TypeSubstitutor getResultingSubstitutor() {
        return resultingSubstitutor;
    }

    @NotNull
    @Override
    public TypeSubstitutor getCurrentSubstitutor() {
        return currentSubstitutor;
    }

    @NotNull
    public static JetType createCorrespondingExtensionFunctionType(@NotNull JetType functionType, @NotNull JetType receiverType) {
        assert KotlinBuiltIns.getInstance().isFunctionType(functionType);

        List<TypeProjection> typeArguments = functionType.getArguments();
        assert !typeArguments.isEmpty();

        List<JetType> arguments = Lists.newArrayList();
        // excluding the last type argument of the function type, which is the return type
        int index = 0;
        int lastIndex = typeArguments.size() - 1;
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
