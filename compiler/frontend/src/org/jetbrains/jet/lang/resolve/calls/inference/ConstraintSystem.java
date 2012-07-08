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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

/**
 * @author svtk
 */
public interface ConstraintSystem {

    enum ConstraintType {
        SUB_TYPE, SUPER_TYPE, EQUAL
    }

    JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance);

    void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull TypeBounds typeBounds);

    void addSubtypingConstraint(@NotNull JetType exactType, @NotNull JetType expectedType, @NotNull ConstraintPosition constraintPosition);

    void addConstraint(@NotNull ConstraintType constraintType, @NotNull JetType exactType, @NotNull JetType expectedType, @NotNull ConstraintPosition constraintPosition);

    TypeBounds getTypeBounds(TypeParameterDescriptor typeParameterDescriptor);

    Map<TypeParameterDescriptor, TypeBounds> getTypeBoundsMap();

    boolean isSuccessful();

    boolean hasContradiction();

    TypeParameterDescriptor getFirstConflictingParameter();

    TypeSubstitutor getSubstitutor();

    Collection<TypeSubstitutor> getSubstitutors();

    JetType getValue(TypeParameterDescriptor typeParameterDescriptor);

    boolean hasError();

    Queue<ConstraintPosition> getErrorConstraintPositions();

    boolean checkUpperBound(@NotNull TypeParameterDescriptor typeParameterDescriptor);
}
