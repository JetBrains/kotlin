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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
public class ConstraintsBuilderImpl implements ConstraintsBuilder {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    enum ConstraintType {
        SUB_TYPE, SUPER_TYPE, EQUAL
    }

    public static ConstraintType varianceToConstraintType(Variance variance) {
        if (variance == Variance.INVARIANT) {
            return ConstraintType.EQUAL;
        }
        if (variance == Variance.OUT_VARIANCE) {
            return ConstraintType.SUB_TYPE;
        }
        return ConstraintType.SUPER_TYPE;
    }

    private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
    private final TypeSubstitutor typeSubstitutor;
    private final Collection<ConstraintPosition> errorConstraintPositions;
    private boolean typeConstructorMismatch;

    public ConstraintsBuilderImpl() {
        this(false, Lists.<ConstraintPosition>newArrayList());
    }

    public ConstraintsBuilderImpl(boolean typeConstructorMismatch, Collection<ConstraintPosition> errorConstraintPositions) {
        this.typeConstructorMismatch = typeConstructorMismatch;
        this.errorConstraintPositions = errorConstraintPositions;
        this.typeSubstitutor = TypeSubstitutor.create(new TypeSubstitution() {
            @Override
            public TypeProjection get(TypeConstructor key) {
                DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
                if (declarationDescriptor instanceof TypeParameterDescriptor) {
                    TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;

                    JetType value = getValue(descriptor);
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
    public Collection<ConstraintPosition> getTypeConstructorMismatchConstraintPositions() {
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
        addConstraint(ConstraintType.SUB_TYPE, subjectType, constrainingType, constraintPosition);
    }

    @Override
    public void addSupertypeConstraint(@NotNull JetType subjectType, @NotNull JetType constrainingType, @NotNull ConstraintPosition constraintPosition) {
        addConstraint(ConstraintType.SUPER_TYPE, subjectType, constrainingType, constraintPosition);
    }

    public void addConstraint(@NotNull ConstraintType constraintType, @NotNull JetType exactType, @NotNull JetType expectedType,
            @NotNull ConstraintPosition constraintPosition) {
        if (exactType == DONT_CARE || expectedType == DONT_CARE || exactType == TypeUtils.NO_EXPECTED_TYPE
            || expectedType == TypeUtils.NO_EXPECTED_TYPE) {
            return;
        }

        DeclarationDescriptor expectedTypeDescriptor = expectedType.getConstructor().getDeclarationDescriptor();
        DeclarationDescriptor exactTypeDescriptor = exactType.getConstructor().getDeclarationDescriptor();

        if (expectedTypeDescriptor instanceof TypeParameterDescriptor) {
            if (TypeUtils.dependsOnTypeParameterConstructors(exactType, Collections.singleton(DONT_CARE.getConstructor()))) return;
            if (expectedType.isNullable()) {
                exactType = TypeUtils.makeNotNullable(exactType);
            }
            TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) expectedTypeDescriptor;
            if (constraintType == ConstraintType.SUB_TYPE) {
                typeParameterConstraints.get(typeParameter).addLowerConstraint(exactType);
            }
            else if (constraintType == ConstraintType.SUPER_TYPE) {
                typeParameterConstraints.get(typeParameter).addUpperConstraint(exactType);
            }
            else {
                typeParameterConstraints.get(typeParameter).addEqualConstraint(exactType);
            }
            return;
        }

        if (exactTypeDescriptor instanceof ClassDescriptor && expectedTypeDescriptor instanceof ClassDescriptor) {
            if (constraintType != ConstraintType.SUPER_TYPE) {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(exactType, expectedType);
                if (correspondingSupertype != null) {
                    exactType = correspondingSupertype;
                }
            }
            else {
                JetType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(expectedType, exactType);
                if (correspondingSupertype != null) {
                    expectedType = correspondingSupertype;
                }
            }

            if (exactType.getConstructor().getParameters().size() != expectedType.getConstructor().getParameters().size()) {
                errorConstraintPositions.add(constraintPosition);
                typeConstructorMismatch = true;
                return;
            }

            ClassDescriptor subClass = (ClassDescriptor) exactType.getConstructor().getDeclarationDescriptor();
            ClassDescriptor superClass = (ClassDescriptor) expectedType.getConstructor().getDeclarationDescriptor();
            if (DescriptorUtils.isSubclass(subClass, superClass)) {
                List<TypeProjection> subArguments = exactType.getArguments();
                List<TypeProjection> superArguments = expectedType.getArguments();
                List<TypeParameterDescriptor> superParameters = expectedType.getConstructor().getParameters();
                for (int i = 0; i < superArguments.size(); i++) {
                    addConstraint(varianceToConstraintType(superParameters.get(i).getVariance()), subArguments.get(i).getType(), superArguments.get(i).getType(),
                                  constraintPosition);
                }
                return;
            }
        }
        typeConstructorMismatch = true;
        errorConstraintPositions.add(constraintPosition);
    }

    @Nullable
    public JetType getValue(@NotNull TypeParameterDescriptor typeParameter) {
        //todo all checks
        TypeConstraintsImpl typeConstraints = typeParameterConstraints.get(typeParameter);
        //todo variance dependance
        if (typeConstraints == null) {
            //todo assert typeConstraints != null;
            return null;
        }
        if (typeConstraints.getLowerConstraint() != null) {
            return typeConstraints.getLowerConstraint();
        }

        if (typeConstraints.getUpperConstraint() != null) {
            return typeConstraints.getUpperConstraint();
        }
        return null;
    }

    @NotNull
    @Override
    public Set<TypeParameterDescriptor> getTypeParameters() {
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
            if (typeConstraints != null && !typeConstraints.isSuccessful()) return true;
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
    public TypeSubstitutor getSubstitutor() {
        return typeSubstitutor;
    }

    public boolean upperBoundsAreSatisfied() {
        for (TypeParameterDescriptor typeParameter : typeParameterConstraints.keySet()) {
            JetType type = getValue(typeParameter);
            JetType upperBound = typeParameter.getUpperBoundsAsType();
            JetType substitute = getSubstitutor().substitute(upperBound, Variance.INVARIANT);

            if (type != null) {
                if (substitute == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitute)) {
                    return false;
                }
            }
        }
        return true;
    }
}
