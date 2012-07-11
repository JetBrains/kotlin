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
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;

import java.util.*;

/**
 * @author svtk
 */
public class ConstraintsSystemImpl implements ConstraintsSystem {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    enum ConstraintKind {
        SUB_TYPE, SUPER_TYPE, EQUAL
    }

    public static ConstraintKind varianceToConstraintKind(Variance variance) {
        if (variance == Variance.INVARIANT) {
            return ConstraintKind.EQUAL;
        }
        if (variance == Variance.OUT_VARIANCE) {
            return ConstraintKind.SUB_TYPE;
        }
        return ConstraintKind.SUPER_TYPE;
    }

    private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
    private final TypeSubstitutor resultingSubstitutor;
    private final Set<ConstraintPosition> errorConstraintPositions;
    private boolean typeConstructorMismatch;

    public ConstraintsSystemImpl() {
        this(false, Sets.<ConstraintPosition>newHashSet());
    }

    public ConstraintsSystemImpl(boolean typeConstructorMismatch, Set<ConstraintPosition> errorConstraintPositions) {
        this.typeConstructorMismatch = typeConstructorMismatch;
        this.errorConstraintPositions = errorConstraintPositions;
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
        return typeConstructorMismatch;
    }

    @NotNull
    @Override
    public Set<ConstraintPosition> getTypeConstructorMismatchConstraintPositions() {
        return errorConstraintPositions;
    }

    @Override
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        typeParameterConstraints.put(typeParameterDescriptor, new TypeConstraintsImpl(positionVariance));
    }

    //todo remove
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull TypeConstraints typeConstraints) {
        typeParameterConstraints.put(typeParameterDescriptor, (TypeConstraintsImpl) typeConstraints);
    }

    @Override
    public void addSubtypingConstraint(@NotNull JetType subjectType, @NotNull JetType constrainingType, @NotNull ConstraintPosition constraintPosition) {
        addConstraint(ConstraintKind.SUB_TYPE, subjectType, constrainingType, constraintPosition);
    }

    @Override
    public void addSupertypeConstraint(@NotNull JetType subjectType, @NotNull JetType constrainingType, @NotNull ConstraintPosition constraintPosition) {
        addConstraint(ConstraintKind.SUPER_TYPE, subjectType, constrainingType, constraintPosition);
    }

    public void addConstraint(@NotNull ConstraintKind constraintKind, @NotNull JetType subjectType, @NotNull JetType constrainingType,
            @NotNull ConstraintPosition constraintPosition) {
        if (subjectType == DONT_CARE || constrainingType == DONT_CARE || subjectType == TypeUtils.NO_EXPECTED_TYPE
            || constrainingType == TypeUtils.NO_EXPECTED_TYPE) {
            return;
        }

        DeclarationDescriptor constrainingTypeDescriptor = constrainingType.getConstructor().getDeclarationDescriptor();
        DeclarationDescriptor subjectTypeDescriptor = subjectType.getConstructor().getDeclarationDescriptor();

        if (constrainingTypeDescriptor instanceof TypeParameterDescriptor) {
            if (TypeUtils.dependsOnTypeParameterConstructors(subjectType, Collections.singleton(DONT_CARE.getConstructor()))) return;
            if (constrainingType.isNullable()) {
                subjectType = TypeUtils.makeNotNullable(subjectType);
            }
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) constrainingTypeDescriptor;
            if (constraintKind == ConstraintKind.SUB_TYPE) {
                typeParameterConstraints.get(typeParameter).addLowerBound(subjectType);
            }
            else if (constraintKind == ConstraintKind.SUPER_TYPE) {
                typeParameterConstraints.get(typeParameter).addUpperBound(subjectType);
            }
            else {
                typeParameterConstraints.get(typeParameter).addExactBound(subjectType);
            }
            return;
        }

        if (subjectTypeDescriptor instanceof ClassDescriptor && constrainingTypeDescriptor instanceof ClassDescriptor) {
            if (constraintKind != ConstraintKind.SUPER_TYPE) {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(subjectType, constrainingType);
                if (correspondingSupertype != null) {
                    subjectType = correspondingSupertype;
                }
            }
            else {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(constrainingType, subjectType);
                if (correspondingSupertype != null) {
                    constrainingType = correspondingSupertype;
                }
            }

            if (subjectType.getConstructor().getParameters().size() != constrainingType.getConstructor().getParameters().size()) {
                errorConstraintPositions.add(constraintPosition);
                typeConstructorMismatch = true;
                return;
            }

            ClassDescriptor subClass = (ClassDescriptor) subjectType.getConstructor().getDeclarationDescriptor();
            ClassDescriptor superClass = (ClassDescriptor) constrainingType.getConstructor().getDeclarationDescriptor();
            if (DescriptorUtils.isSubclass(subClass, superClass)) {
                List<TypeProjection> subArguments = subjectType.getArguments();
                List<TypeProjection> superArguments = constrainingType.getArguments();
                List<TypeParameterDescriptor> superParameters = constrainingType.getConstructor().getParameters();
                for (int i = 0; i < superArguments.size(); i++) {
                    addConstraint(varianceToConstraintKind(superParameters.get(i).getVariance()), subArguments.get(i).getType(), superArguments.get(i).getType(),
                                  constraintPosition);
                }
                return;
            }
        }
        typeConstructorMismatch = true;
        errorConstraintPositions.add(constraintPosition);
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

    public boolean upperBoundsAreSatisfied() {
        for (TypeParameterDescriptor typeParameter : typeParameterConstraints.keySet()) {
            JetType type = ConstraintsUtil.getValue(getTypeConstraints(typeParameter));
            JetType upperBound = typeParameter.getUpperBoundsAsType();
            JetType substitute = getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);

            if (type != null) {
                if (substitute == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitute)) {
                    return false;
                }
            }
        }
        return true;
    }
}
