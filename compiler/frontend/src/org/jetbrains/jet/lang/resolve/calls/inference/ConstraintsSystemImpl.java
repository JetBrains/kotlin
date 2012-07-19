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
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.resolve.calls.inference.TypeConstraintsImpl.ConstraintKind;

import java.util.*;

/**
 * @author svtk
 */
public class ConstraintsSystemImpl implements ConstraintsSystem {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
    private final Set<ConstraintPosition> errorConstraintPositions = Sets.newHashSet();
    private final TypeSubstitutor resultingSubstitutor;
    private final TypeSubstitutor currentSubstitutor;
    private boolean hasErrorInConstrainingTypes;

    public ConstraintsSystemImpl() {
        this.resultingSubstitutor = createTypeSubstitutorWithDefaultForUnknownTypeParameter(null);
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
        if (!(constrainingTypeDescriptor instanceof ClassDescriptor) || !(subjectTypeDescriptor instanceof ClassDescriptor)) {
            errorConstraintPositions.add(constraintPosition);
            return;
        }
        switch (constraintKind) {
            case SUPER_TYPE: {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(constrainingType, subjectType);
                if (correspondingSupertype != null) {
                    constrainingType = correspondingSupertype;
                }
                break;
            }
            case SUB_TYPE: {
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
            //todo constrainingArguments.get(i).getType() -> type projections
            addConstraint(ConstraintKind.fromVariance(parameters.get(i).getVariance()), subjectArguments.get(i).getType(),
                          constrainingArguments.get(i).getType(), constraintPosition);
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
        return !hasTypeConstructorMismatch() && !hasUnknownParameters() && !hasConflictingConstraints();
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
}
