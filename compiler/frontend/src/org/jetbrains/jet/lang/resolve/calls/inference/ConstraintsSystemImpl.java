/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;

import java.util.*;

/**
 * @author svtk
 */
public class ConstraintsSystemImpl implements ConstraintsSystem {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
    private final Set<ConstraintPosition> errorConstraintPositions = Sets.newHashSet();
    private final TypeSubstitutor resultingSubstitutor;
    private boolean hasErrorInConstrainingTypes;

    public ConstraintsSystemImpl() {
        this.resultingSubstitutor = TypeSubstitutor.create(new TypeSubstitution() {
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
                    return new TypeProjection(DONT_CARE);
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
    public boolean hasErrorInConstrainingTypes() {
        return hasErrorInConstrainingTypes;
    }

    @Override
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        typeParameterConstraints.put(typeParameterDescriptor, new TypeConstraintsImpl(positionVariance));
    }

    @NotNull
    public ConstraintsSystemImpl replaceTypeVariables(@NotNull Map<TypeParameterDescriptor, TypeParameterDescriptor> typeVariablesMap) {
        ConstraintsSystemImpl newConstraintSystem = new ConstraintsSystemImpl();
        for (Map.Entry<TypeParameterDescriptor, TypeConstraintsImpl> entry : typeParameterConstraints.entrySet()) {
            TypeParameterDescriptor typeParameter = entry.getKey();
            TypeConstraintsImpl typeConstraints = entry.getValue();

            TypeParameterDescriptor newTypeParameter = typeVariablesMap.get(typeParameter);
            assert newTypeParameter != null;
            newConstraintSystem.typeParameterConstraints.put(newTypeParameter, typeConstraints);
        }
        for (ConstraintPosition constraintPosition : errorConstraintPositions) {
            newConstraintSystem.errorConstraintPositions.add(constraintPosition);
        }
        newConstraintSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes;
        return newConstraintSystem;
    }

    @Override
    public void addSubtypingConstraint(@NotNull JetType subjectType, @Nullable JetType constrainingType, @NotNull ConstraintPosition constraintPosition) {
        addConstraint(ConstraintKind.SUB_TYPE, subjectType, constrainingType, constraintPosition);
    }

    @Override
    public void addSupertypeConstraint(@NotNull JetType subjectType, @Nullable JetType constrainingType, @NotNull ConstraintPosition constraintPosition) {
        addConstraint(ConstraintKind.SUPER_TYPE, subjectType, constrainingType, constraintPosition);
    }

    private void addConstraint(@NotNull ConstraintKind constraintKind,
            @NotNull JetType subjectType,
            @Nullable JetType constrainingType,
            @NotNull ConstraintPosition constraintPosition) {

        if (constrainingType == null || (ErrorUtils.isErrorType(constrainingType) && constrainingType != DONT_CARE)) {
            hasErrorInConstrainingTypes = true;
            return;
        }

        assert subjectType != TypeUtils.NO_EXPECTED_TYPE : "Subject type shouldn't be NO_EXPECTED_TYPE (in position " + constraintPosition + " )";

        if (constrainingType == DONT_CARE || ErrorUtils.isErrorType(subjectType) || constrainingType == TypeUtils.NO_EXPECTED_TYPE) {
            return;
        }

        DeclarationDescriptor constrainingTypeDescriptor = constrainingType.getConstructor().getDeclarationDescriptor();
        DeclarationDescriptor subjectTypeDescriptor = subjectType.getConstructor().getDeclarationDescriptor();

        if (subjectTypeDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) subjectTypeDescriptor;
            TypeConstraintsImpl typeConstraints = typeParameterConstraints.get(typeParameter);
            if (typeConstraints != null) {
                addBoundToTypeConstraints(constraintKind, subjectType, constrainingType, typeConstraints);
                return;
            }
        }
        if (constrainingTypeDescriptor instanceof TypeParameterDescriptor) {
            assert typeParameterConstraints.get(constrainingTypeDescriptor) == null : "Constraining type contains type variable " + "";//todo
        }

        if (constrainingTypeDescriptor instanceof ClassDescriptor && subjectTypeDescriptor instanceof ClassDescriptor) {
            switch (constraintKind) {
                case SUPER_TYPE:
                {
                    JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(constrainingType, subjectType);
                    if (correspondingSupertype != null) {
                        constrainingType = correspondingSupertype;
                    }
                    break;
                }
                case SUB_TYPE:
                {
                    JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(subjectType, constrainingType);
                    if (correspondingSupertype != null) {
                        subjectType = correspondingSupertype;
                    }
                }
                case EQUAL: //nothing
            }
            //todo type constructors are same, else error

            if (constrainingType.getConstructor().getParameters().size() != subjectType.getConstructor().getParameters().size()) {
                errorConstraintPositions.add(constraintPosition);
                return;
            }
            ClassDescriptor subClass = (ClassDescriptor) constrainingType.getConstructor().getDeclarationDescriptor();
            ClassDescriptor superClass = (ClassDescriptor) subjectType.getConstructor().getDeclarationDescriptor();
            if (DescriptorUtils.isSubclass(subClass, superClass)) {
                List<TypeProjection> subArguments = constrainingType.getArguments();
                List<TypeProjection> superArguments = subjectType.getArguments();
                List<TypeParameterDescriptor> superParameters = subjectType.getConstructor().getParameters();
                for (int i = 0; i < superArguments.size(); i++) {
                    //todo subArguments.get(i).getType() -> type projections
                    addConstraint(ConstraintKind.fromVariance(superParameters.get(i).getVariance()), superArguments.get(i).getType(),
                                  subArguments.get(i).getType(), constraintPosition);
                }
                return;
            }
        }
        errorConstraintPositions.add(constraintPosition);
    }

    //todo move to type constraints
    private void addBoundToTypeConstraints(@NotNull ConstraintKind constraintKind, @NotNull JetType subjectType,
            @NotNull JetType constrainingType, @NotNull TypeConstraintsImpl typeConstraints) {

        if (TypeUtils.dependsOnTypeParameterConstructors(constrainingType, Collections.singleton(DONT_CARE.getConstructor()))) return;
        //todo it's an error
        if (subjectType.isNullable()) {
            constrainingType = TypeUtils.makeNotNullable(constrainingType);
        }
        //todo switch
        if (constraintKind == ConstraintKind.SUPER_TYPE) {
            typeConstraints.addLowerBound(constrainingType);
        }
        else if (constraintKind == ConstraintKind.SUB_TYPE) {
            typeConstraints.addUpperBound(constrainingType);
        }
        else {
            typeConstraints.addExactBound(constrainingType);
        }
    }

    @NotNull
    @Override
    public Set<TypeParameterDescriptor> getTypeVariables() {
        return typeParameterConstraints.keySet();
    }

    @Override
    @Nullable
    public TypeConstraints getTypeConstraints(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        return typeParameterConstraints.get(typeParameterDescriptor);
    }

    @Override
    public boolean isSuccessful() {
        return !hasTypeConstructorMismatch() && !hasUnknownParameters() && !hasConflictingParameters();
    }

    @Override
    public boolean hasContradiction() {
        return hasTypeConstructorMismatch() || hasConflictingParameters();
    }

    @Override
    public boolean hasConflictingParameters() {
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

    private enum ConstraintKind {
        SUB_TYPE, SUPER_TYPE, EQUAL;

        @NotNull
        static ConstraintKind fromVariance(@NotNull Variance variance) {
            ConstraintKind constraintKind = null;
            switch (variance) {
                case INVARIANT:
                    constraintKind = EQUAL;
                    break;
                case OUT_VARIANCE:
                    constraintKind = SUPER_TYPE;
                    break;
                case IN_VARIANCE:
                    constraintKind = SUB_TYPE;
            }
            return constraintKind;
        }
    }
}
