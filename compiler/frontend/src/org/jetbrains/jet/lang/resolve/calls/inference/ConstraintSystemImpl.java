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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.resolve.calls.inference.TypeConstraintsImpl.ConstraintKind;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.*;
import static org.jetbrains.jet.lang.resolve.calls.inference.TypeConstraintsImpl.ConstraintKind.*;
import static org.jetbrains.jet.lang.types.Variance.*;

public class ConstraintSystemImpl implements ConstraintSystem {

    private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
    private final Set<ConstraintPosition> errorConstraintPositions = Sets.newHashSet();
    private final TypeSubstitutor resultingSubstitutor;
    private final TypeSubstitutor currentSubstitutor;
    private boolean hasErrorInConstrainingTypes;

    public ConstraintSystemImpl() {
        this.resultingSubstitutor = createTypeSubstitutorWithDefaultForUnknownTypeParameter(new TypeProjection(CANT_INFER));
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
                    if (value != null && !TypeUtils.dependsOnTypeParameterConstructors(value, Collections.singleton(
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
        return errorConstraintPositions.size() == 1 && errorConstraintPositions.contains(ConstraintPosition.EXPECTED_TYPE_POSITION);
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
        addConstraint(SUPER_TYPE, subjectType, constrainingType, constraintPosition);
    }

    @Override
    public void addSubtypeConstraint(
            @Nullable JetType constrainingType,
            @NotNull JetType subjectType,
            @NotNull ConstraintPosition constraintPosition
    ) {
        addConstraint(SUB_TYPE, subjectType, constrainingType, constraintPosition);
    }

    private void addConstraint(@NotNull ConstraintKind constraintKind,
            @NotNull JetType subjectType,
            @Nullable JetType constrainingType,
            @NotNull ConstraintPosition constraintPosition) {

        if (constrainingType == TypeUtils.NO_EXPECTED_TYPE
            || constrainingType == DONT_CARE
            || constrainingType == CANT_INFER) {
            return;
        }

        if (constrainingType == null || (ErrorUtils.isErrorType(constrainingType) && constrainingType != PLACEHOLDER_FUNCTION_TYPE)) {
            hasErrorInConstrainingTypes = true;
            return;
        }

        assert subjectType != TypeUtils.NO_EXPECTED_TYPE : "Subject type shouldn't be NO_EXPECTED_TYPE (in position " + constraintPosition + " )";
        if (ErrorUtils.isErrorType(subjectType)) return;

        DeclarationDescriptor subjectTypeDescriptor = subjectType.getConstructor().getDeclarationDescriptor();

        KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();
        if (constrainingType == PLACEHOLDER_FUNCTION_TYPE) {
            if (!kotlinBuiltIns.isFunctionOrExtensionFunctionType(subjectType)) {
                if (subjectTypeDescriptor instanceof TypeParameterDescriptor && typeParameterConstraints.get(subjectTypeDescriptor) != null) {
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
        if (constraintKind == SUB_TYPE && kotlinBuiltIns.isFunctionType(constrainingType) && kotlinBuiltIns.isExtensionFunctionType(subjectType)) {
            constrainingType = createCorrespondingExtensionFunctionType(constrainingType, DONT_CARE);
        }

        DeclarationDescriptor constrainingTypeDescriptor = constrainingType.getConstructor().getDeclarationDescriptor();

        if (subjectTypeDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) subjectTypeDescriptor;
            TypeConstraintsImpl typeConstraints = typeParameterConstraints.get(typeParameter);
            if (typeConstraints != null) {
                if (TypeUtils.dependsOnTypeParameterConstructors(constrainingType, Collections.singleton(DONT_CARE.getConstructor()))) {
                    return;
                }
                if (subjectType.isNullable() && constrainingType.isNullable()) {
                    constrainingType = TypeUtils.makeNotNullable(constrainingType);
                }
                typeConstraints.addBound(constraintKind, constrainingType);
                return;
            }
        }
        if (constrainingTypeDescriptor instanceof TypeParameterDescriptor) {
            assert typeParameterConstraints.get(constrainingTypeDescriptor) == null : "Constraining type contains type variable " + constrainingTypeDescriptor.getName();
        }
        if (constraintKind == SUB_TYPE && kotlinBuiltIns.isNothingOrNullableNothing(constrainingType)) {
            // following constraints are always true:
            // 'Nothing' is a subtype of any type
            if (!constrainingType.isNullable()) return;
            // 'Nothing?' is a subtype of nullable type
            if (subjectType.isNullable()) return;
        }
        if (!(constrainingTypeDescriptor instanceof ClassDescriptor) || !(subjectTypeDescriptor instanceof ClassDescriptor)) {
            errorConstraintPositions.add(constraintPosition);
            return;
        }
        switch (constraintKind) {
            case SUB_TYPE: {
                if (kotlinBuiltIns.isNothingOrNullableNothing(constrainingType)) break;
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(constrainingType, subjectType);
                if (correspondingSupertype != null) {
                    constrainingType = correspondingSupertype;
                }
                break;
            }
            case SUPER_TYPE: {
                if (kotlinBuiltIns.isNothingOrNullableNothing(subjectType)) break;
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(subjectType, constrainingType);
                if (correspondingSupertype != null) {
                    subjectType = correspondingSupertype;
                }
            }
            case EQUAL: //nothing
        }
        if (constrainingType.getConstructor() != subjectType.getConstructor()) {
            errorConstraintPositions.add(constraintPosition);
            return;
        }
        TypeConstructor typeConstructor = subjectType.getConstructor();
        List<TypeProjection> subjectArguments = subjectType.getArguments();
        List<TypeProjection> constrainingArguments = constrainingType.getArguments();
        List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
        for (int i = 0; i < subjectArguments.size(); i++) {
            Variance typeParameterVariance = parameters.get(i).getVariance();
            TypeProjection subjectArgument = subjectArguments.get(i);
            TypeProjection constrainingArgument = constrainingArguments.get(i);

            ConstraintKind typeParameterConstraintKind = getTypeParameterConstraintKind(typeParameterVariance,
                subjectArgument, constrainingArgument, constraintKind);

            addConstraint(typeParameterConstraintKind, subjectArgument.getType(), constrainingArgument.getType(), constraintPosition);
        }
    }

    /**
     * Determines what constraint (supertype, subtype or equal) should be generated for type parameter {@code T} in a constraint like (in this example subtype one): <br/>
     * {@code MyClass<in/out/- A> <: MyClass<in/out/- B>}, where {@code MyClass<in/out/- T>} is declared. <br/>
     *
     * The parameters description is given according to the example above.
     * @param typeParameterVariance declared variance of T
     * @param subjectTypeProjection {@code in/out/- A}
     * @param constrainingTypeProjection {@code in/out/- B}
     * @param upperConstraintKind kind of the constraint {@code MyClass<...A>  <: MyClass<...B>} (subtype in this example).
     * @return kind of constraint to be generated: {@code A <: B} (subtype), {@code A >: B} (supertype) or {@code A = B} (equal).
     */
    @NotNull
    private static ConstraintKind getTypeParameterConstraintKind(
            @NotNull Variance typeParameterVariance,
            @NotNull TypeProjection subjectTypeProjection,
            @NotNull TypeProjection constrainingTypeProjection,
            @NotNull ConstraintKind upperConstraintKind
    ) {
        // If variance of type parameter is non-trivial, it should be taken into consideration to infer result constraint type.
        // Otherwise when type parameter declared as INVARIANT, there might be non-trivial use-site variance of a supertype.
        //
        // Example: Let class MyClass<T> is declared.
        //
        // If super type has 'out' projection:
        // MyClass<A> <: MyClass<out B>,
        // then constraint A <: B can be generated.
        //
        // If super type has 'in' projection:
        // MyClass<A> <: MyClass<in B>,
        // then constraint A >: B can be generated.
        //
        // Otherwise constraint A = B should be generated.

        Variance varianceForTypeParameter;
        if (typeParameterVariance != INVARIANT) {
            varianceForTypeParameter = typeParameterVariance;
        }
        else if (upperConstraintKind == SUPER_TYPE) {
            varianceForTypeParameter = constrainingTypeProjection.getProjectionKind();
        }
        else if (upperConstraintKind == SUB_TYPE) {
            varianceForTypeParameter = subjectTypeProjection.getProjectionKind();
        }
        else {
            varianceForTypeParameter = INVARIANT;
        }

        return getTypeParameterConstraintKind(varianceForTypeParameter, upperConstraintKind);
    }

    /**
     * Let class {@code MyClass<T, out R, in S>} is declared.<br/><br/>
     *
     * If upperConstraintKind is SUPER_TYPE:
     * {@code MyClass<A, B, C> <: MyClass<D, E, F>}, <br/>
     * then constraints {@code A = D, B <: E, C >: F} are generated. <br/><br/>
     *
     * If upperConstraintKind is SUB_TYPE:
     * {@code MyClass<A, B, C> >: MyClass<D, E, F>}, <br/>
     * then constraints {@code A = D, B >: E, C <: F} are generated. <br/><br/>
     *
     * If upperConstraintKind is EQUAL:
     * {@code MyClass<A, B, C> = MyClass<D, E, F>}, <br/>
     * then equality constraints {@code A = D, B = E, C = F} are generated. <br/><br/>
     *
     * Method getTypeParameterConstraintKind gets upperConstraintKind and variance of type parameter
     * (INVARIANT for T, OUT_VARIANCE for R in example above) and returns kind of constraint for corresponding type parameter.
     */
    @NotNull
    private static ConstraintKind getTypeParameterConstraintKind(
            @NotNull Variance typeParameterVariance,
            @NotNull ConstraintKind upperConstraintKind
    ) {
        if (upperConstraintKind == EQUAL || typeParameterVariance == INVARIANT) {
            return EQUAL;
        }
        if ((upperConstraintKind == SUB_TYPE && typeParameterVariance == OUT_VARIANCE) ||
            (upperConstraintKind == SUPER_TYPE && typeParameterVariance == IN_VARIANCE)) {
            return SUB_TYPE;
        }
        return SUPER_TYPE;
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
