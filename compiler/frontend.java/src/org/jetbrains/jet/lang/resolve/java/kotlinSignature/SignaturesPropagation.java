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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.psi.HierarchicalMethodSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.MutableReadOnlyCollectionsMap;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SignaturesPropagation {
    public static JetType modifyReturnTypeAccordingToSuperMethods(
            @NotNull JetType autoType, // type built by JavaTypeTransformer
            @NotNull PsiMethodWrapper method,
            @NotNull BindingTrace trace
    ) {
        Set<JetType> typesFromSuperMethods = Sets.newHashSet();
        for (HierarchicalMethodSignature superSignature : method.getPsiMethod().getHierarchicalMethodSignature().getSuperSignatures()) {
            DeclarationDescriptor superFun = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, superSignature.getMethod());
            if (superFun instanceof FunctionDescriptor) {
                typesFromSuperMethods.add(((FunctionDescriptor) superFun).getReturnType());
            }
            else {
                // Function descriptor can't be find iff superclass is java.lang.Collection or similar (translated to jet.* collections)
                assert !JavaToKotlinClassMap.getInstance().mapPlatformClass(
                        new FqName(superSignature.getMethod().getContainingClass().getQualifiedName())).isEmpty():
                            "Can't find super function for " + method.getPsiMethod() + " defined in "
                            +  method.getPsiMethod().getContainingClass();
            }
        }

        return modifyReturnTypeAccordingToSuperMethods(autoType, typesFromSuperMethods, true);
    }

    @NotNull
    private static JetType modifyReturnTypeAccordingToSuperMethods(
            @NotNull JetType autoType,
            @NotNull Collection<JetType> typesFromSuper,
            boolean covariantPosition
    ) {
        if (ErrorUtils.isErrorType(autoType)) {
            return autoType;
        }

        boolean resultNullable = returnTypeMustBeNullable(autoType, typesFromSuper, covariantPosition);
        List<TypeProjection> resultArguments = getTypeArgsOfReturnType(autoType, typesFromSuper);
        JetScope resultScope;
        ClassifierDescriptor classifierDescriptor = getReturnTypeClassifier(autoType, typesFromSuper);
        if (classifierDescriptor instanceof ClassDescriptor) {
            resultScope = ((ClassDescriptor) classifierDescriptor).getMemberScope(resultArguments);
        }
        else {
            resultScope = autoType.getMemberScope();
        }

        return new JetTypeImpl(autoType.getAnnotations(),
                               classifierDescriptor.getTypeConstructor(),
                               resultNullable,
                               resultArguments,
                               resultScope);
    }

    @NotNull
    private static List<TypeProjection> getTypeArgsOfReturnType(@NotNull JetType autoType, @NotNull Collection<JetType> typesFromSuper) {
        TypeConstructor typeConstructor = autoType.getConstructor();
        List<TypeProjection> autoArguments = autoType.getArguments();

        if (!(typeConstructor.getDeclarationDescriptor() instanceof ClassDescriptor)) {
            assert autoArguments.isEmpty() :
                    "Unexpected type arguments when type constructor is not ClassDescriptor, type = " + autoType;
            return autoArguments;
        }

        List<List<TypeProjection>> typeArgumentsFromSuper = calculateTypeArgumentsFromSuper(autoType, typesFromSuper);

        // Modify type arguments using info from typesFromSuper
        List<TypeProjection> resultArguments = Lists.newArrayList();
        for (int i = 0; i < autoArguments.size(); i++) {
            TypeProjection argument = autoArguments.get(i);

            TypeCheckingProcedure.EnrichedProjectionKind effectiveProjectionKind =
                    TypeCheckingProcedure.getEffectiveProjectionKind(typeConstructor.getParameters().get(i), argument);

            JetType argumentType = argument.getType();
            List<TypeProjection> projectionsFromSuper = typeArgumentsFromSuper.get(i);
            Collection<JetType> argTypesFromSuper = getTypes(projectionsFromSuper);
            boolean covariantPosition = effectiveProjectionKind == TypeCheckingProcedure.EnrichedProjectionKind.OUT;

            JetType type = modifyReturnTypeAccordingToSuperMethods(argumentType, argTypesFromSuper, covariantPosition);
            Variance variance = calculateArgumentVarianceFromSuper(argument, projectionsFromSuper);

            resultArguments.add(new TypeProjection(variance, type));
        }
        return resultArguments;
    }

    private static Variance calculateArgumentVarianceFromSuper(TypeProjection argument, List<TypeProjection> projectionsFromSuper) {
        Set<Variance> variancesInSuper = Sets.newHashSet();
        for (TypeProjection projection : projectionsFromSuper) {
            variancesInSuper.add(projection.getProjectionKind());
        }

        Variance defaultVariance = argument.getProjectionKind();
        if (variancesInSuper.size() == 0) {
            return defaultVariance;
        }
        else if (variancesInSuper.size() == 1) {
            Variance varianceInSuper = variancesInSuper.iterator().next();
            if (defaultVariance == Variance.INVARIANT || defaultVariance == varianceInSuper) {
                return varianceInSuper;
            }
            else {
                // TODO report error
                return defaultVariance;
            }
        }
        else {
            // TODO report error
            return defaultVariance;
        }
    }

    @NotNull
    private static List<JetType> getTypes(@NotNull List<TypeProjection> projections) {
        List<JetType> types = Lists.newArrayList();
        for (TypeProjection projection : projections) {
            types.add(projection.getType());
        }
        return types;
    }

    // Returns list with type arguments info from supertypes
    private static List<List<TypeProjection>> calculateTypeArgumentsFromSuper(
            @NotNull JetType autoType,
            @NotNull Collection<JetType> typesFromSuper
    ) {
        ClassDescriptor klass = (ClassDescriptor) autoType.getConstructor().getDeclarationDescriptor();

        // For each superclass of autoType's class and its parameters, hold their mapping to autoType's parameters
        Multimap<TypeConstructor, TypeProjection> substitution = SubstitutionUtils.buildDeepSubstitutionMultimap(
                TypeUtils.makeUnsubstitutedType(klass, JetScope.EMPTY));

        // for each parameter of autoType, hold arguments in corresponding supertypes
        List<List<TypeProjection>> parameterToArgTypesFromSuper = Lists.newArrayList();
        for (TypeProjection ignored : autoType.getArguments()) {
            parameterToArgTypesFromSuper.add(new ArrayList<TypeProjection>());
        }

        // Enumerate all types from super and all its parameters
        for (JetType typeFromSuper : typesFromSuper) {
            List<TypeParameterDescriptor> typeFromSuperParameters = typeFromSuper.getConstructor().getParameters();
            for (int i = 0; i < typeFromSuperParameters.size(); i++) {
                TypeParameterDescriptor typeFromSuperParam = typeFromSuperParameters.get(i);
                TypeProjection typeFromSuperArgType = typeFromSuper.getArguments().get(i);

                // if it is mapped to autoType's parameter, then store it into map
                for (TypeProjection projection : substitution.get(typeFromSuperParam.getTypeConstructor())) {
                    ClassifierDescriptor classifier = projection.getType().getConstructor().getDeclarationDescriptor();

                    if (classifier instanceof TypeParameterDescriptor && classifier.getContainingDeclaration() == klass) {
                        parameterToArgTypesFromSuper.get(((TypeParameterDescriptor) classifier).getIndex()).add(typeFromSuperArgType);
                    }
                }
            }
        }
        return parameterToArgTypesFromSuper;
    }

    private static boolean returnTypeMustBeNullable(JetType autoType, Collection<JetType> typesFromSuper, boolean covariantPosition) {
        if (typesFromSuper.isEmpty()) {
            return autoType.isNullable();
        }

        boolean someSupersNullable = false;
        boolean someSupersNotNull = false;
        for (JetType typeFromSuper : typesFromSuper) {
            if (typeFromSuper.isNullable()) {
                someSupersNullable = true;
            }
            else {
                someSupersNotNull = true;
            }
        }
        if (someSupersNotNull && someSupersNullable) {
            //noinspection IfStatementWithIdenticalBranches
            if (covariantPosition) {
                return false;
            }
            else {
                // TODO error!
                return true;
            }
        }

        assert someSupersNotNull || someSupersNullable; // we have at least one type, which is either not-null or nullable

        return someSupersNullable && autoType.isNullable();
    }

    @NotNull
    private static ClassifierDescriptor getReturnTypeClassifier(@NotNull JetType autoType, @NotNull Collection<JetType> typesFromSuper) {
        ClassifierDescriptor classifier = autoType.getConstructor().getDeclarationDescriptor();
        if (!(classifier instanceof ClassDescriptor)) {
            assert classifier != null : "no declaration descriptor for type " + autoType;
            return classifier;
        }
        ClassDescriptor clazz = (ClassDescriptor) classifier;

        MutableReadOnlyCollectionsMap collectionsMap = MutableReadOnlyCollectionsMap.getInstance();

        if (collectionsMap.isMutableCollection(clazz)) {

            boolean someSupersMutable = false;
            boolean someSupersReadOnly = false;
            for (JetType typeFromSuper : typesFromSuper) {
                ClassifierDescriptor classifierFromSuper = typeFromSuper.getConstructor().getDeclarationDescriptor();
                if (classifierFromSuper instanceof ClassDescriptor) {
                    ClassDescriptor classFromSuper = (ClassDescriptor) classifierFromSuper;

                    if (collectionsMap.isMutableCollection(classFromSuper)) {
                        someSupersMutable = true;
                    }
                    else if (collectionsMap.isReadOnlyCollection(classFromSuper)) {
                        someSupersReadOnly = true;
                    }
                }
            }

            if (someSupersReadOnly && !someSupersMutable) {
                return collectionsMap.convertMutableToReadOnly(clazz);
            }
        }
        return classifier;
    }

    private SignaturesPropagation() {
    }
}
