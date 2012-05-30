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

package org.jetbrains.jet.lang.types;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class SubstitutionUtils {
    @NotNull
    public static Map<TypeConstructor, TypeProjection> buildSubstitutionContext(@NotNull  JetType context) {
        return buildSubstitutionContext(context.getConstructor().getParameters(), context.getArguments());
    }

    /**
     * Builds a context with all the supertypes' parameters substituted
     */
    @NotNull
    public static TypeSubstitutor buildDeepSubstitutor(@NotNull JetType type) {
        Map<TypeConstructor, TypeProjection> substitution = Maps.newHashMap();
        TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitution);
        // we use the mutability of the map here
        fillInDeepSubstitutor(type, typeSubstitutor, substitution, null);
        return typeSubstitutor;
    }

    @NotNull
    public static Multimap<TypeConstructor, TypeProjection> buildDeepSubstitutionMultimap(@NotNull JetType type) {
        Multimap<TypeConstructor, TypeProjection> fullSubstitution = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        Map<TypeConstructor, TypeProjection> substitution = Maps.newHashMap();
        TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitution);
        // we use the mutability of the map here
        fillInDeepSubstitutor(type, typeSubstitutor, substitution, fullSubstitution);
        return fullSubstitution;
    }

    // we use the mutability of the substitution map here
    private static void fillInDeepSubstitutor(@NotNull JetType context, @NotNull TypeSubstitutor substitutor, @NotNull Map<TypeConstructor, TypeProjection> substitution, @Nullable Multimap<TypeConstructor, TypeProjection> fullSubstitution) {
        List<TypeParameterDescriptor> parameters = context.getConstructor().getParameters();
        List<TypeProjection> arguments = context.getArguments();

        if (parameters.size() != arguments.size()) {
            throw new IllegalStateException();
        }

        for (int i = 0; i < arguments.size(); i++) {
            TypeProjection argument = arguments.get(i);
            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

            JetType substitute = substitutor.substitute(argument.getType(), Variance.INVARIANT);
            assert substitute != null;
            TypeProjection substitutedTypeProjection = new TypeProjection(argument.getProjectionKind(), substitute);
            substitution.put(typeParameterDescriptor.getTypeConstructor(), substitutedTypeProjection);
            if (fullSubstitution != null) {
                fullSubstitution.put(typeParameterDescriptor.getTypeConstructor(), substitutedTypeProjection);
            }
        }
        if (JetStandardClasses.isNothingOrNullableNothing(context)) return;
        for (JetType supertype : context.getConstructor().getSupertypes()) {
            fillInDeepSubstitutor(supertype, substitutor, substitution, fullSubstitution);
        }
    }

    @NotNull
    public static Map<TypeConstructor, TypeProjection> buildSubstitutionContext(@NotNull List<TypeParameterDescriptor> parameters, @NotNull List<TypeProjection> contextArguments) {
        Map<TypeConstructor, TypeProjection> parameterValues = new HashMap<TypeConstructor, TypeProjection>();
        fillInSubstitutionContext(parameters, contextArguments, parameterValues);
        return parameterValues;
    }

    private static void fillInSubstitutionContext(List<TypeParameterDescriptor> parameters, List<TypeProjection> contextArguments, Map<TypeConstructor, TypeProjection> parameterValues) {
        if (parameters.size() != contextArguments.size()) {
            throw new IllegalArgumentException("type parameter count != context arguments");
        }
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameter = parameters.get(i);
            TypeProjection value = contextArguments.get(i);
            parameterValues.put(parameter.getTypeConstructor(), value);
        }
    }

    @NotNull
    public static TypeProjection makeStarProjection(@NotNull TypeParameterDescriptor parameterDescriptor) {
        return new TypeProjection(Variance.OUT_VARIANCE, parameterDescriptor.getUpperBoundsAsType());
    }

    public static boolean hasUnsubstitutedTypeParameters(JetType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptorImpl) {
            return true;
        }

        for(TypeProjection proj : type.getArguments()) {
            if(hasUnsubstitutedTypeParameters(proj.getType())) {
                return true;
            }
        }

        return false;
    }

    public static Map<TypeConstructor, TypeProjection> removeTrivialSubstitutions(Map<TypeConstructor, TypeProjection> context) {
        Map<TypeConstructor, TypeProjection> clean = Maps.newHashMap(context);
        boolean changed = false;
        for (Iterator<Map.Entry<TypeConstructor, TypeProjection>> iterator = clean.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<TypeConstructor, TypeProjection> entry = iterator.next();
            TypeConstructor key = entry.getKey();
            TypeProjection value = entry.getValue();
            if (key == value.getType().getConstructor() && value.getProjectionKind() == Variance.INVARIANT) {
                iterator.remove();
                changed = true;
            }
        }
        return changed ? clean : context;
    }

    public static void assertNotImmediatelyRecursive(Map<TypeConstructor, TypeProjection> context) {
        // Make sure we never replace a T with "Foo<T>" or something similar,
        // because the substitution will not terminate in this case
        // This check is not complete. It does not find cases like
        //    T -> Foo<T1>
        //    T -> Bar<T>

        for (Map.Entry<TypeConstructor, TypeProjection> entry : context.entrySet()) {
            TypeConstructor key = entry.getKey();
            TypeProjection value = entry.getValue();
            if (TypeUtils.typeConstructorUsedInType(key, value.getType())) {
                throw new IllegalStateException("Immediately recursive substitution: " + context + "\nProblematic parameter: " + key + " -> " + value);
            }
        }
    }
}
