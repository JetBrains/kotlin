/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ConstraintsUtil {
    @Nullable
    public static TypeParameterDescriptor getFirstConflictingParameter(@NotNull ConstraintSystem constraintSystem) {
        for (TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables()) {
            TypeBounds constraints = constraintSystem.getTypeBounds(typeParameter);
            if (constraints.getValues().size() > 1) {
                return typeParameter;
            }
        }
        return null;
    }

    @NotNull
    public static Collection<TypeSubstitutor> getSubstitutorsForConflictingParameters(@NotNull ConstraintSystem constraintSystem) {
        TypeParameterDescriptor firstConflictingParameter = getFirstConflictingParameter(constraintSystem);
        if (firstConflictingParameter == null) return Collections.emptyList();

        Collection<KotlinType> conflictingTypes = constraintSystem.getTypeBounds(firstConflictingParameter).getValues();

        List<Map<TypeConstructor, TypeProjection>> substitutionContexts = Lists.newArrayList();
        for (KotlinType type : conflictingTypes) {
            Map<TypeConstructor, TypeProjection> context = Maps.newLinkedHashMap();
            context.put(firstConflictingParameter.getTypeConstructor(), new TypeProjectionImpl(type));
            substitutionContexts.add(context);
        }

        for (TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables()) {
            if (typeParameter == firstConflictingParameter) continue;

            KotlinType safeType = getSafeValue(constraintSystem, typeParameter);
            for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
                TypeProjection typeProjection = new TypeProjectionImpl(safeType);
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
    public static KotlinType getSafeValue(@NotNull ConstraintSystem constraintSystem, @NotNull TypeParameterDescriptor typeParameter) {
        KotlinType type = constraintSystem.getTypeBounds(typeParameter).getValue();
        if (type != null) {
            return type;
        }
        //todo may be error type
        return TypeIntersector.getUpperBoundsAsType(typeParameter);
    }

    public static boolean checkUpperBoundIsSatisfied(
            @NotNull ConstraintSystem constraintSystem,
            @NotNull TypeParameterDescriptor typeParameter,
            boolean substituteOtherTypeParametersInBound
    ) {
        KotlinType type = constraintSystem.getTypeBounds(typeParameter).getValue();
        if (type == null) return true;
        for (KotlinType upperBound : typeParameter.getUpperBounds()) {
            if (!substituteOtherTypeParametersInBound && TypeUtils.dependsOnTypeParameters(upperBound, constraintSystem.getTypeVariables())) {
                continue;
            }
            KotlinType substitutedUpperBound = constraintSystem.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT);

            assert substitutedUpperBound != null : "We wanted to substitute projections as a result for " + typeParameter;
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(type, substitutedUpperBound)) {
                return false;
            }
        }
        return true;
    }

    public static String getDebugMessageForStatus(@NotNull ConstraintSystemStatus status) {
        StringBuilder sb = new StringBuilder();
        List<Method> interestingMethods = Lists.newArrayList();
        for (Method method : status.getClass().getMethods()) {
            String name = method.getName();
            boolean isInteresting = name.startsWith("is") || name.startsWith("has") && !name.equals("hashCode");
            if (method.getParameterTypes().length == 0 && isInteresting) {
                interestingMethods.add(method);
            }
        }
        Collections.sort(interestingMethods, new Comparator<Method>() {
            @Override
            public int compare(@NotNull Method method1, @NotNull Method method2) {
                return method1.getName().compareTo(method2.getName());
            }
        });
        for (Iterator<Method> iterator = interestingMethods.iterator(); iterator.hasNext(); ) {
            Method method = iterator.next();
            try {
                sb.append("-").append(method.getName()).append(": ").append(method.invoke(status));
                if (iterator.hasNext()) {
                    sb.append("\n");
                }
            }
            catch (IllegalAccessException e) {
                sb.append(e.getMessage());
            }
            catch (InvocationTargetException e) {
                sb.append(e.getMessage());
            }
        }
        return sb.toString();
    }
}
