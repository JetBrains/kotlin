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
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.*;

/**
 * @author svtk
 */
public class ConstraintsUtil {

    @NotNull
    public static Set<JetType> getValues(@Nullable TypeConstraints typeConstraints) {
        Set<JetType> values = Sets.newLinkedHashSet();
        if (typeConstraints != null && !typeConstraints.isEmpty()) {
            values.addAll(typeConstraints.getExactBounds());
            if (!typeConstraints.getLowerBounds().isEmpty()) {
                JetType superTypeOfLowerBounds = CommonSupertypes.commonSupertype(typeConstraints.getLowerBounds());
                if (values.isEmpty()) {
                    values.add(superTypeOfLowerBounds);
                }
                for (JetType value : values) {
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(superTypeOfLowerBounds, value)) {
                        values.add(superTypeOfLowerBounds);
                        break;
                    }
                }
            }
            if (!typeConstraints.getUpperBounds().isEmpty()) {
                //todo subTypeOfUpperBounds
                JetType subTypeOfUpperBounds = typeConstraints.getUpperBounds().iterator().next(); //todo
                if (values.isEmpty()) {
                    values.add(subTypeOfUpperBounds);
                }
                for (JetType value : values) {
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(value, subTypeOfUpperBounds)) {
                        values.add(subTypeOfUpperBounds);
                        break;
                    }
                }
            }
        }
        return values;
    }

    @Nullable
    public static JetType getValue(@Nullable TypeConstraints typeConstraints) {
        //todo all checks
        //todo variance dependance
        if (typeConstraints == null) {
            //todo assert typeConstraints != null;
            return null;
        }
        Set<JetType> values = getValues(typeConstraints);
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return null;
    }



    @Nullable
    public static TypeParameterDescriptor getFirstConflictingParameter(@NotNull ConstraintSystem constraintSystem) {
        for (TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables()) {
            TypeConstraints constraints = constraintSystem.getTypeConstraints(typeParameter);
            if (getValues(constraints).size() > 1) {
                return typeParameter;
            }
        }
        return null;
    }

    @NotNull
    public static Collection<TypeSubstitutor> getSubstitutorsForConflictingParameters(@NotNull ConstraintSystem constraintSystem) {
        TypeParameterDescriptor firstConflictingParameter = getFirstConflictingParameter(constraintSystem);
        if (firstConflictingParameter == null) return Collections.emptyList();

        Collection<JetType> conflictingTypes = getValues(constraintSystem.getTypeConstraints(firstConflictingParameter));

        ArrayList<Map<TypeConstructor, TypeProjection>> substitutionContexts = Lists.newArrayList();
        for (JetType type : conflictingTypes) {
            Map<TypeConstructor, TypeProjection> context = Maps.newLinkedHashMap();
            context.put(firstConflictingParameter.getTypeConstructor(), new TypeProjection(type));
            substitutionContexts.add(context);
        }

        for (TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables()) {
            if (typeParameter == firstConflictingParameter) continue;

            JetType safeType = getSafeValue(constraintSystem, typeParameter);
            for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
                TypeProjection typeProjection = new TypeProjection(safeType);
                context.put(typeParameter.getTypeConstructor(), typeProjection);
            }
        }
        Collection<TypeSubstitutor> typeSubstitutors = Lists.newArrayList();
        for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
            typeSubstitutors.add(TypeSubstitutor.create(context));
        }
        return typeSubstitutors;
    }

    @NotNull
    public static JetType getSafeValue(@NotNull ConstraintSystem constraintSystem, @NotNull TypeParameterDescriptor typeParameter) {
        TypeConstraints constraints = constraintSystem.getTypeConstraints(typeParameter);
        JetType type = getValue(constraints);
        if (type != null) {
            return type;
        }
        //todo may be error type
        return typeParameter.getUpperBoundsAsType();
    }

    public static boolean checkUpperBoundIsSatisfied(@NotNull ConstraintSystem constraintSystem,
            @NotNull TypeParameterDescriptor typeParameter) {
        TypeConstraints typeConstraints = constraintSystem.getTypeConstraints(typeParameter);
        assert typeConstraints != null;
        JetType type = getValue(typeConstraints);
        JetType upperBound = typeParameter.getUpperBoundsAsType();
        JetType substitute = constraintSystem.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);

        if (type != null) {
            if (substitute == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitute)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkBoundsAreSatisfied(@NotNull ConstraintSystem constraintSystem) {
        for (TypeParameterDescriptor typeVariable : constraintSystem.getTypeVariables()) {
            JetType type = getValue(constraintSystem.getTypeConstraints(typeVariable));
            JetType upperBound = typeVariable.getUpperBoundsAsType();
            JetType substitutedType = constraintSystem.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);

            if (type != null) {
                if (substitutedType == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitutedType)) {
                    return false;
                }
            }
        }
        return true;
    }
}
