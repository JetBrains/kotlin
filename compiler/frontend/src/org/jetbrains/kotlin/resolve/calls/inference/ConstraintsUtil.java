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
    public static TypeVariable getFirstConflictingVariable(@NotNull ConstraintSystem constraintSystem) {
        for (TypeVariable typeVariable : constraintSystem.getTypeVariables()) {
            TypeBounds constraints = constraintSystem.getTypeBounds(typeVariable);
            if (constraints.getValues().size() > 1) {
                return typeVariable;
            }
        }
        return null;
    }

    @NotNull
    public static Collection<TypeSubstitutor> getSubstitutorsForConflictingParameters(@NotNull ConstraintSystem constraintSystem) {
        TypeVariable firstConflictingVariable = getFirstConflictingVariable(constraintSystem);
        if (firstConflictingVariable == null) return Collections.emptyList();
        TypeParameterDescriptor firstConflictingParameter = constraintSystem.variableToDescriptor(firstConflictingVariable);

        Collection<KotlinType> conflictingTypes = constraintSystem.getTypeBounds(firstConflictingVariable).getValues();

        List<Map<TypeConstructor, TypeProjection>> substitutionContexts = Lists.newArrayList();
        for (KotlinType type : conflictingTypes) {
            Map<TypeConstructor, TypeProjection> context = Maps.newLinkedHashMap();
            context.put(firstConflictingParameter.getTypeConstructor(), new TypeProjectionImpl(type));
            substitutionContexts.add(context);
        }

        for (TypeVariable typeVariable : constraintSystem.getTypeVariables()) {
            if (typeVariable == firstConflictingVariable) continue;

            KotlinType safeType = getSafeValue(constraintSystem, typeVariable);
            for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
                TypeProjection typeProjection = new TypeProjectionImpl(safeType);
                context.put(constraintSystem.variableToDescriptor(typeVariable).getTypeConstructor(), typeProjection);
            }
        }
        Collection<TypeSubstitutor> typeSubstitutors = new ArrayList<TypeSubstitutor>(substitutionContexts.size());
        for (Map<TypeConstructor, TypeProjection> context : substitutionContexts) {
            typeSubstitutors.add(TypeSubstitutor.create(context));
        }
        return typeSubstitutors;
    }

    @NotNull
    private static KotlinType getSafeValue(@NotNull ConstraintSystem constraintSystem, @NotNull TypeVariable typeVariable) {
        KotlinType type = constraintSystem.getTypeBounds(typeVariable).getValue();
        if (type != null) {
            return type;
        }
        //todo may be error type
        return TypeIntersector.getUpperBoundsAsType(constraintSystem.variableToDescriptor(typeVariable));
    }

    public static boolean checkUpperBoundIsSatisfied(
            @NotNull ConstraintSystem constraintSystem,
            @NotNull TypeParameterDescriptor typeParameter,
            boolean substituteOtherTypeParametersInBound
    ) {
        KotlinType type = constraintSystem.getTypeBounds(constraintSystem.descriptorToVariable(typeParameter)).getValue();
        if (type == null) return true;
        for (KotlinType upperBound : typeParameter.getUpperBounds()) {
            if (!substituteOtherTypeParametersInBound &&
                TypeUtils.dependsOnTypeParameters(upperBound, constraintSystem.getTypeParameterDescriptors())) {
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
