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
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.CollectionClassMapping;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;

import java.util.*;

public class SignaturesPropagation {
    public static JetType modifyReturnTypeAccordingToSuperMethods(
            @NotNull JetType autoType, // type built by JavaTypeTransformer
            @NotNull List<FunctionDescriptor> superFunctions,
            @NotNull Function1<String, Void> reportError
    ) {
        List<JetType> typesFromSuperMethods = ContainerUtil.map(superFunctions,
                                                                new Function<FunctionDescriptor, JetType>() {
                                                                    @Override
                                                                    public JetType fun(FunctionDescriptor superFunction) {
                                                                        return superFunction.getReturnType();
                                                                    }
                                                                });

        return modifyTypeAccordingToSuperMethods(autoType, typesFromSuperMethods, true, reportError);
    }

    public static JavaDescriptorResolver.ValueParameterDescriptors modifyValueParametersAccordingToSuperMethods(
            @NotNull JavaDescriptorResolver.ValueParameterDescriptors parameters, // descriptors built by parameters resolver
            @NotNull List<FunctionDescriptor> superFunctions,
            @NotNull Function1<String, Void> reportError
    ) {
        // we are not processing receiver type specifically:
        // if this function comes from Kotlin, then we don't need to do it, if it doesn't, then it can't have receiver

        List<ValueParameterDescriptor> resultParameters = Lists.newArrayList();

        for (final ValueParameterDescriptor originalParam : parameters.getDescriptors()) {
            final int index = originalParam.getIndex();
            List<JetType> typesFromSuperMethods = ContainerUtil.map(superFunctions,
                    new Function<FunctionDescriptor, JetType>() {
                        @Override
                        public JetType fun(FunctionDescriptor superFunction) {
                            return superFunction.getValueParameters().get(index).getType();
                        }
                    });

            JetType altType = modifyTypeAccordingToSuperMethods(originalParam.getType(), typesFromSuperMethods, false, reportError);

            resultParameters.add(new ValueParameterDescriptorImpl(
                    originalParam.getContainingDeclaration(),
                    index,
                    originalParam.getAnnotations(),
                    originalParam.getName(),
                    originalParam.isVar(),
                    altType,
                    originalParam.declaresDefaultValue(),
                    originalParam.getVarargElementType()
            ));
        }

        return new JavaDescriptorResolver.ValueParameterDescriptors(parameters.getReceiverType(), resultParameters);
    }

    public static List<FunctionDescriptor> getSuperFunctionsForMethod(
            @NotNull PsiMethodWrapper method,
            @NotNull BindingTrace trace
    ) {
        List<FunctionDescriptor> superFunctions = Lists.newArrayList();
        for (HierarchicalMethodSignature superSignature : method.getPsiMethod().getHierarchicalMethodSignature().getSuperSignatures()) {
            DeclarationDescriptor superFun = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, superSignature.getMethod());
            if (superFun instanceof FunctionDescriptor) {
                superFunctions.add(((FunctionDescriptor) superFun));
            }
            else {
                // TODO assert is temporarily disabled
                // It fails because of bug in IDEA on Mac: it adds invalid roots to JDK classpath and it leads to the problem that
                // getHierarchicalMethodSignature() returns elements from invalid virtual files

                // Function descriptor can't be find iff superclass is java.lang.Collection or similar (translated to jet.* collections)
                //assert !JavaToKotlinClassMap.getInstance().mapPlatformClass(
                //        new FqName(superSignature.getMethod().getContainingClass().getQualifiedName())).isEmpty():
                //        "Can't find super function for " + method.getPsiMethod() + " defined in "
                //        +  method.getPsiMethod().getContainingClass();
            }
        }

        // sorting for diagnostic stability
        Collections.sort(superFunctions, new Comparator<FunctionDescriptor>() {
            @Override
            public int compare(FunctionDescriptor fun1, FunctionDescriptor fun2) {
                FqNameUnsafe fqName1 = DescriptorUtils.getFQName(fun1.getContainingDeclaration());
                FqNameUnsafe fqName2 = DescriptorUtils.getFQName(fun2.getContainingDeclaration());
                return fqName1.getFqName().compareTo(fqName2.getFqName());
            }
        });
        return superFunctions;
    }


    @NotNull
    private static JetType modifyTypeAccordingToSuperMethods(
            @NotNull JetType autoType,
            @NotNull List<JetType> typesFromSuper,
            boolean covariantPosition,
            @NotNull Function1<String, Void> reportError
    ) {
        if (ErrorUtils.isErrorType(autoType)) {
            return autoType;
        }

        boolean resultNullable = typeMustBeNullable(autoType, typesFromSuper, covariantPosition, reportError);
        List<TypeProjection> resultArguments = getTypeArgsOfType(autoType, typesFromSuper, reportError);
        JetScope resultScope;
        ClassifierDescriptor classifierDescriptor = modifyTypeClassifier(autoType, typesFromSuper);
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
    private static List<TypeProjection> getTypeArgsOfType(
            @NotNull JetType autoType,
            @NotNull List<JetType> typesFromSuper,
            @NotNull Function1<String, Void> reportError
    ) {
        TypeConstructor typeConstructor = autoType.getConstructor();
        ClassifierDescriptor classifier = typeConstructor.getDeclarationDescriptor();
        List<TypeProjection> autoArguments = autoType.getArguments();

        if (!(classifier instanceof ClassDescriptor)) {
            assert autoArguments.isEmpty() :
                    "Unexpected type arguments when type constructor is not ClassDescriptor, type = " + autoType;
            return autoArguments;
        }

        List<List<TypeProjection>> typeArgumentsFromSuper = calculateTypeArgumentsFromSuper((ClassDescriptor) classifier, typesFromSuper);

        // Modify type arguments using info from typesFromSuper
        List<TypeProjection> resultArguments = Lists.newArrayList();
        for (TypeParameterDescriptor parameter : typeConstructor.getParameters()) {
            TypeProjection argument = autoArguments.get(parameter.getIndex());

            TypeCheckingProcedure.EnrichedProjectionKind effectiveProjectionKind =
                    TypeCheckingProcedure.getEffectiveProjectionKind(parameter, argument);

            JetType argumentType = argument.getType();
            List<TypeProjection> projectionsFromSuper = typeArgumentsFromSuper.get(parameter.getIndex());
            List<JetType> argTypesFromSuper = getTypes(projectionsFromSuper);
            boolean covariantPosition = effectiveProjectionKind == TypeCheckingProcedure.EnrichedProjectionKind.OUT;

            JetType type = modifyTypeAccordingToSuperMethods(argumentType, argTypesFromSuper, covariantPosition, reportError);
            Variance projectionKind = calculateArgumentProjectionKindFromSuper(argument, projectionsFromSuper, reportError);

            resultArguments.add(new TypeProjection(projectionKind, type));
        }
        return resultArguments;
    }

    private static Variance calculateArgumentProjectionKindFromSuper(
            @NotNull TypeProjection argument,
            @NotNull List<TypeProjection> projectionsFromSuper,
            @NotNull Function1<String, Void> reportError
    ) {
        Set<Variance> projectionKindsInSuper = Sets.newLinkedHashSet();
        for (TypeProjection projection : projectionsFromSuper) {
            projectionKindsInSuper.add(projection.getProjectionKind());
        }

        Variance defaultProjectionKind = argument.getProjectionKind();
        if (projectionKindsInSuper.size() == 0) {
            return defaultProjectionKind;
        }
        else if (projectionKindsInSuper.size() == 1) {
            Variance projectionKindInSuper = projectionKindsInSuper.iterator().next();
            if (defaultProjectionKind == Variance.INVARIANT || defaultProjectionKind == projectionKindInSuper) {
                return projectionKindInSuper;
            }
            else {
                reportError.invoke("Incompatible projection kinds in type arguments of super methods' return types: "
                       + projectionsFromSuper + ", defined in current: " + argument);
                return defaultProjectionKind;
            }
        }
        else {
            reportError.invoke("Incompatible projection kinds in type arguments of super methods' return types: " + projectionsFromSuper);
            return defaultProjectionKind;
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
    // Example:
    //     - Foo<A, B> is a subtype of Bar<A, List<B>>, Baz<Boolean, A>
    //     - input: klass = Foo, typesFromSuper = [Bar<String, List<Int>>, Baz<Boolean, CharSequence>]
    //     - output[0] = [String, CharSequence], output[1] = []
    private static List<List<TypeProjection>> calculateTypeArgumentsFromSuper(
            @NotNull ClassDescriptor klass,
            @NotNull Collection<JetType> typesFromSuper
    ) {
        // For each superclass of klass and its parameters, hold their mapping to klass' parameters
        // #0 of Bar ->  A
        // #1 of Bar ->  List<B>
        // #0 of Baz ->  Boolean
        // #1 of Baz ->  A
        // #0 of Foo ->  A (mapped to itself)
        // #1 of Foo ->  B (mapped to itself)
        Multimap<TypeConstructor, TypeProjection> substitution = SubstitutionUtils.buildDeepSubstitutionMultimap(
                TypeUtils.makeUnsubstitutedType(klass, JetScope.EMPTY));

        // for each parameter of klass, hold arguments in corresponding supertypes
        List<List<TypeProjection>> parameterToArgumentsFromSuper = Lists.newArrayList();
        for (TypeParameterDescriptor ignored : klass.getTypeConstructor().getParameters()) {
            parameterToArgumentsFromSuper.add(new ArrayList<TypeProjection>());
        }

        // Enumerate all types from super and all its parameters
        for (JetType typeFromSuper : typesFromSuper) {
            for (TypeParameterDescriptor parameter : typeFromSuper.getConstructor().getParameters()) {
                TypeProjection argument = typeFromSuper.getArguments().get(parameter.getIndex());

                // for given example, this block is executed four times:
                // 1. typeFromSuper = Bar<String, List<Int>>,      parameter = "#0 of Bar",  argument = String
                // 2. typeFromSuper = Bar<String, List<Int>>,      parameter = "#1 of Bar",  argument = List<Int>
                // 3. typeFromSuper = Baz<Boolean, CharSequence>,  parameter = "#0 of Baz",  argument = Boolean
                // 4. typeFromSuper = Baz<Boolean, CharSequence>,  parameter = "#1 of Baz",  argument = CharSequence

                // if it is mapped to klass' parameter, then store it into map
                for (TypeProjection projection : substitution.get(parameter.getTypeConstructor())) {
                    // 1. projection = A
                    // 2. projection = List<B>
                    // 3. projection = Boolean
                    // 4. projection = A
                    ClassifierDescriptor classifier = projection.getType().getConstructor().getDeclarationDescriptor();

                    // this condition is true for 1 and 4, false for 2 and 3
                    if (classifier instanceof TypeParameterDescriptor && classifier.getContainingDeclaration() == klass) {
                        int parameterIndex = ((TypeParameterDescriptor) classifier).getIndex();
                        parameterToArgumentsFromSuper.get(parameterIndex).add(argument);
                    }
                }
            }
        }
        return parameterToArgumentsFromSuper;
    }

    private static boolean typeMustBeNullable(
            @NotNull JetType autoType,
            @NotNull List<JetType> typesFromSuper,
            boolean covariantPosition,
            @NotNull Function1<String, Void> reportError
    ) {
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
                reportError.invoke("Incompatible types in superclasses: " + typesFromSuper);
            }
        }

        assert someSupersNotNull || someSupersNullable; // we have at least one type, which is either not-null or nullable

        // This check may seem like voodoo magic, but it's not.
        // We want to handle case when parameter of super method is nullable, but parameter of sub method claims to be not null:
        // of course, it is an error in annotations, and parameter of sub method should be nullable.
        if (!covariantPosition && someSupersNullable && !autoType.isNullable()) {
            reportError.invoke("In superclass type is nullable: " + typesFromSuper + ", in subclass it is not: " + autoType);
            return true;
        }

        return someSupersNullable && autoType.isNullable();
    }

    @NotNull
    private static ClassifierDescriptor modifyTypeClassifier(@NotNull JetType autoType, @NotNull List<JetType> typesFromSuper) {
        ClassifierDescriptor classifier = autoType.getConstructor().getDeclarationDescriptor();
        if (!(classifier instanceof ClassDescriptor)) {
            assert classifier != null : "no declaration descriptor for type " + autoType;
            return classifier;
        }
        ClassDescriptor clazz = (ClassDescriptor) classifier;

        CollectionClassMapping collectionMapping = CollectionClassMapping.getInstance();

        if (collectionMapping.isMutableCollection(clazz)) {

            boolean someSupersMutable = false;
            boolean someSupersReadOnly = false;
            for (JetType typeFromSuper : typesFromSuper) {
                ClassifierDescriptor classifierFromSuper = typeFromSuper.getConstructor().getDeclarationDescriptor();
                if (classifierFromSuper instanceof ClassDescriptor) {
                    ClassDescriptor classFromSuper = (ClassDescriptor) classifierFromSuper;

                    if (collectionMapping.isMutableCollection(classFromSuper)) {
                        someSupersMutable = true;
                    }
                    else if (collectionMapping.isReadOnlyCollection(classFromSuper)) {
                        someSupersReadOnly = true;
                    }
                }
            }

            if (someSupersReadOnly && !someSupersMutable) {
                return collectionMapping.convertMutableToReadOnly(clazz);
            }
        }
        return classifier;
    }

    private SignaturesPropagation() {
    }
}
