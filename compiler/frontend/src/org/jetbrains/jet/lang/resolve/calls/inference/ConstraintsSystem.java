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

import com.google.common.base.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collection;
import java.util.Set;

/**
 * @author svtk
 */
public interface ConstraintsSystem {

    void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance);

    @NotNull
    Set<TypeParameterDescriptor> getTypeVariables();

    //todo
    /** only subject type might contain type variables
     *
     * @param subjectType
     * @param constrainingType
     * @param constraintPosition
     */
    void addSubtypingConstraint(@NotNull JetType subjectType, @Nullable JetType constrainingType, @NotNull ConstraintPosition constraintPosition);

    // only subject type might contain type variables
    void addSupertypeConstraint(@NotNull JetType subjectType, @Nullable JetType constrainingType, @NotNull ConstraintPosition constraintPosition);

    boolean isSuccessful();

    boolean hasContradiction();

    boolean hasConflictingParameters();

    boolean hasUnknownParameters();

    boolean hasTypeConstructorMismatch();

    boolean hasTypeConstructorMismatchAt(@NotNull ConstraintPosition constraintPosition);

    boolean hasErrorInConstrainingTypes();

    @Nullable
    TypeConstraints getTypeConstraints(@NotNull TypeParameterDescriptor typeParameterDescriptor);

    /**
     *
     * @return
     */
    @NotNull
    TypeSubstitutor getResultingSubstitutor();
}
