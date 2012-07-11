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
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author svtk
 */
public class ConstraintsUtil {

    @Nullable
    public static TypeParameterDescriptor getFirstConflictingParameter(@NotNull ConstraintsBuilder constraintsBuilder) {
        for (TypeParameterDescriptor typeParameter : constraintsBuilder.getTypeParameters()) {
            TypeConstraints constraints = constraintsBuilder.getTypeConstraints(typeParameter);
            if (!constraints.getConflicts().isEmpty()) {
                return typeParameter;
            }
        }
        return null;
    }

    @NotNull
    public static Collection<TypeSubstitutor> getSubstitutorsForConflictingParameters(@NotNull ConstraintsBuilder constraintsBuilder) {
        TypeParameterDescriptor firstConflictingParameter = getFirstConflictingParameter(constraintsBuilder);
        if (firstConflictingParameter == null) return Collections.emptyList();

        Collection<JetType> conflictingTypes = constraintsBuilder.getTypeConstraints(firstConflictingParameter).getConflicts();

        ArrayList<Map<TypeConstructor, TypeProjection>> substitutionContexts = Lists.newArrayList();
        for (JetType type : conflictingTypes) {
            Map<TypeConstructor, TypeProjection> context = Maps.newLinkedHashMap();
            context.put(firstConflictingParameter.getTypeConstructor(), new TypeProjection(type));
            substitutionContexts.add(context);
        }

        for (TypeParameterDescriptor typeParameter : constraintsBuilder.getTypeParameters()) {
            if (typeParameter == firstConflictingParameter) continue;

            JetType safeType = getSafeValue(constraintsBuilder, typeParameter);
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
    public static JetType getSafeValue(@NotNull ConstraintsBuilder constraintsBuilder, @NotNull TypeParameterDescriptor typeParameter) {
        JetType type = constraintsBuilder.getValue(typeParameter);
        if (type != null) {
            return type;
        }
        //todo may be error type
        return typeParameter.getUpperBoundsAsType();
    }

    public static boolean checkUpperBoundIsSatisfied(@NotNull ConstraintsBuilder constraintsBuilder,
            @NotNull TypeParameterDescriptor typeParameter) {
        assert constraintsBuilder.getTypeConstraints(typeParameter) != null;
        JetType type = constraintsBuilder.getValue(typeParameter);
        JetType upperBound = typeParameter.getUpperBoundsAsType();
        JetType substitute = constraintsBuilder.getSubstitutor().substitute(upperBound, Variance.INVARIANT);

        if (type != null) {
            if (substitute == null || !JetTypeChecker.INSTANCE.isSubtypeOf(type, substitute)) {
                return false;
            }
        }
        return true;
    }
}
