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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.CollectionClassMapping;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinMethodMap;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getVarargParameterType;

public class SignaturesPropagationData {
    private final List<TypeParameterDescriptor> modifiedTypeParameters;
    private final JavaDescriptorResolver.ValueParameterDescriptors modifiedValueParameters;
    private final JetType modifiedReturnType;

    private final List<String> signatureErrors = Lists.newArrayList();
    private final List<FunctionDescriptor> superFunctions;
    private final Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> autoTypeParameterToModified;

    public SignaturesPropagationData(
            @NotNull ClassDescriptor containingClass,
            @NotNull JetType autoReturnType, // type built by JavaTypeTransformer from Java signature and @NotNull annotations
            @NotNull JavaDescriptorResolver.ValueParameterDescriptors autoValueParameters, // descriptors built by parameters resolver
            @NotNull List<TypeParameterDescriptor> autoTypeParameters, // descriptors built by signature resolver
            @NotNull PsiMethodWrapper method,
            @NotNull BindingTrace trace
    ) {
        superFunctions = getSuperFunctionsForMethod(method, trace, containingClass);

        autoTypeParameterToModified = SignaturesUtil.recreateTypeParametersAndReturnMapping(autoTypeParameters);

        modifiedTypeParameters = modifyTypeParametersAccordingToSuperMethods(autoTypeParameters);
        modifiedReturnType = modifyReturnTypeAccordingToSuperMethods(autoReturnType);
        modifiedValueParameters = modifyValueParametersAccordingToSuperMethods(autoValueParameters);
    }

    public List<TypeParameterDescriptor> getModifiedTypeParameters() {
        return modifiedTypeParameters;
    }

    public JavaDescriptorResolver.ValueParameterDescriptors getModifiedValueParameters() {
        return modifiedValueParameters;
    }

    public JetType getModifiedReturnType() {
        return modifiedReturnType;
    }

    public List<String> getSignatureErrors() {
        return signatureErrors;
    }

    public List<FunctionDescriptor> getSuperFunctions() {
        return superFunctions;
    }
    
    private void reportError(String error) {
        signatureErrors.add(error);
    }

    private JetType modifyReturnTypeAccordingToSuperMethods(
            @NotNull JetType autoType // type built by JavaTypeTransformer
    ) {
        List<JetType> typesFromSuperMethods = ContainerUtil.map(superFunctions,
                                                                new Function<FunctionDescriptor, JetType>() {
                                                                    @Override
                                                                    public JetType fun(FunctionDescriptor superFunction) {
                                                                        return superFunction.getReturnType();
                                                                    }
                                                                });

        return modifyTypeAccordingToSuperMethods(autoType, typesFromSuperMethods, true);
    }

    private List<TypeParameterDescriptor> modifyTypeParametersAccordingToSuperMethods(List<TypeParameterDescriptor> autoTypeParameters) {
        List<TypeParameterDescriptor> result = Lists.newArrayList();

        for (TypeParameterDescriptor autoParameter : autoTypeParameters) {
            int index = autoParameter.getIndex();
            TypeParameterDescriptorImpl modifiedTypeParameter = autoTypeParameterToModified.get(autoParameter);

            List<Iterator<JetType>> upperBoundFromSuperFunctionsIterators = Lists.newArrayList();
            for (FunctionDescriptor superFunction : superFunctions) {
                upperBoundFromSuperFunctionsIterators.add(superFunction.getTypeParameters().get(index).getUpperBounds().iterator());
            }

            for (JetType autoUpperBound : autoParameter.getUpperBounds()) {
                List<JetType> upperBoundsFromSuperFunctions = Lists.newArrayList();

                for (Iterator<JetType> iterator : upperBoundFromSuperFunctionsIterators) {
                    assert iterator.hasNext();
                    upperBoundsFromSuperFunctions.add(iterator.next());
                }

                JetType modifiedUpperBound = modifyTypeAccordingToSuperMethods(autoUpperBound, upperBoundsFromSuperFunctions, false);
                modifiedTypeParameter.addUpperBound(modifiedUpperBound);
            }

            for (Iterator<JetType> iterator : upperBoundFromSuperFunctionsIterators) {
                assert !iterator.hasNext();
            }

            modifiedTypeParameter.setInitialized();
            result.add(modifiedTypeParameter);
        }

        return result;
    }

    private JavaDescriptorResolver.ValueParameterDescriptors modifyValueParametersAccordingToSuperMethods(
            @NotNull JavaDescriptorResolver.ValueParameterDescriptors parameters // descriptors built by parameters resolver
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

            VarargCheckResult varargCheckResult =
                    checkVarargInSuperFunctions(originalParam);

            JetType altType = modifyTypeAccordingToSuperMethods(varargCheckResult.parameterType, typesFromSuperMethods, false);

            resultParameters.add(new ValueParameterDescriptorImpl(
                    originalParam.getContainingDeclaration(),
                    index,
                    originalParam.getAnnotations(),
                    originalParam.getName(),
                    originalParam.isVar(),
                    altType,
                    originalParam.declaresDefaultValue(),
                    varargCheckResult.isVararg ? KotlinBuiltIns.getInstance().getArrayElementType(altType) : null
            ));
        }

        JetType originalReceiverType = parameters.getReceiverType();
        if (originalReceiverType != null) {
            JetType substituted = SignaturesUtil.createSubstitutorForFunctionTypeParameters(autoTypeParameterToModified)
                    .substitute(originalReceiverType, Variance.INVARIANT);
            assert substituted != null;
            return new JavaDescriptorResolver.ValueParameterDescriptors(substituted, resultParameters);
        }
        else {
            return new JavaDescriptorResolver.ValueParameterDescriptors(null, resultParameters);
        }
    }

    private static List<FunctionDescriptor> getSuperFunctionsForMethod(
            @NotNull PsiMethodWrapper method,
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor containingClass
    ) {
        List<FunctionDescriptor> superFunctions = Lists.newArrayList();
        for (HierarchicalMethodSignature superSignature : method.getPsiMethod().getHierarchicalMethodSignature().getSuperSignatures()) {
            DeclarationDescriptor superFun = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, superSignature.getMethod());
            if (superFun instanceof FunctionDescriptor) {
                superFunctions.add(((FunctionDescriptor) superFun));
            }
            else {
                String fqName = superSignature.getMethod().getContainingClass().getQualifiedName();
                assert fqName != null;
                Collection<ClassDescriptor> platformClasses = JavaToKotlinClassMap.getInstance().mapPlatformClass(new FqName(fqName));
                if (platformClasses.isEmpty()) {
                    // TODO assert is temporarily disabled
                    // It fails because of bug in IDEA on Mac: it adds invalid roots to JDK classpath and it leads to the problem that
                    // getHierarchicalMethodSignature() returns elements from invalid virtual files

                    //assert false : "Can't find super function for " + method.getPsiMethod() +
                    //               " defined in " +  method.getPsiMethod().getContainingClass()
                }
                else {
                    List<FunctionDescriptor> funsFromMap =
                            JavaToKotlinMethodMap.INSTANCE.getFunctions(superSignature.getMethod(), containingClass);
                    superFunctions.addAll(funsFromMap);
                }
            }
        }

        // sorting for diagnostic stability
        Collections.sort(superFunctions, new Comparator<FunctionDescriptor>() {
            @Override
            public int compare(FunctionDescriptor fun1, FunctionDescriptor fun2) {
                FqNameUnsafe fqName1 = getFQName(fun1.getContainingDeclaration());
                FqNameUnsafe fqName2 = getFQName(fun2.getContainingDeclaration());
                return fqName1.getFqName().compareTo(fqName2.getFqName());
            }
        });
        return superFunctions;
    }

    @NotNull
    private VarargCheckResult checkVarargInSuperFunctions(@NotNull ValueParameterDescriptor originalParam) {
        boolean someSupersVararg = false;
        boolean someSupersNotVararg = false;
        for (FunctionDescriptor superFunction : superFunctions) {
            if (superFunction.getValueParameters().get(originalParam.getIndex()).getVarargElementType() != null) {
                someSupersVararg = true;
            }
            else {
                someSupersNotVararg = true;
            }
        }

        JetType originalVarargElementType = originalParam.getVarargElementType();
        JetType originalType = originalParam.getType();

        if (someSupersVararg && someSupersNotVararg) {
            reportError("Incompatible super methods: some have vararg parameter, some have not");
            return new VarargCheckResult(originalType, originalVarargElementType != null);
        }

        if (someSupersVararg && originalVarargElementType == null) {
            assert isArrayType(originalType);

            // convert to vararg; replace Array<out Foo>? with Array<Foo>
            JetType varargElementType = KotlinBuiltIns.getInstance().getArrayElementType(originalType);
            return new VarargCheckResult(getVarargParameterType(varargElementType), true);
        }
        else if (someSupersNotVararg && originalVarargElementType != null) {
            assert isArrayType(originalType);

            // convert to non-vararg; replace Array<Foo> with Array<out Foo>?
            return new VarargCheckResult(TypeUtils.makeNullable(getVarargParameterType(originalVarargElementType, Variance.OUT_VARIANCE)),
                                         false);
        }

        return new VarargCheckResult(originalType, originalVarargElementType != null);
    }

    @NotNull
    private JetType modifyTypeAccordingToSuperMethods(
            @NotNull JetType autoType,
            @NotNull List<JetType> typesFromSuper,
            boolean covariantPosition
    ) {
        if (ErrorUtils.isErrorType(autoType)) {
            return autoType;
        }

        boolean resultNullable = typeMustBeNullable(autoType, typesFromSuper, covariantPosition);
        ClassifierDescriptor resultClassifier = modifyTypeClassifier(autoType, typesFromSuper, covariantPosition);
        List<TypeProjection> resultArguments = getTypeArgsOfType(autoType, resultClassifier, typesFromSuper);
        JetScope resultScope;
        if (resultClassifier instanceof ClassDescriptor) {
            resultScope = ((ClassDescriptor) resultClassifier).getMemberScope(resultArguments);
        }
        else {
            resultScope = autoType.getMemberScope();
        }

        return new JetTypeImpl(autoType.getAnnotations(),
                               resultClassifier.getTypeConstructor(),
                               resultNullable,
                               resultArguments,
                               resultScope);
    }

    @NotNull
    private List<TypeProjection> getTypeArgsOfType(
            @NotNull JetType autoType,
            @NotNull ClassifierDescriptor classifier,
            @NotNull List<JetType> typesFromSuper
    ) {
        List<TypeProjection> autoArguments = autoType.getArguments();

        if (!(classifier instanceof ClassDescriptor)) {
            assert autoArguments.isEmpty() :
                    "Unexpected type arguments when type constructor is not ClassDescriptor, type = " + autoType;
            return autoArguments;
        }

        List<List<TypeProjection>> typeArgumentsFromSuper = calculateTypeArgumentsFromSuper((ClassDescriptor) classifier, typesFromSuper);

        // Modify type arguments using info from typesFromSuper
        List<TypeProjection> resultArguments = Lists.newArrayList();
        for (TypeParameterDescriptor parameter : classifier.getTypeConstructor().getParameters()) {
            TypeProjection argument = autoArguments.get(parameter.getIndex());

            TypeCheckingProcedure.EnrichedProjectionKind effectiveProjectionKind =
                    TypeCheckingProcedure.getEffectiveProjectionKind(parameter, argument);

            JetType argumentType = argument.getType();
            List<TypeProjection> projectionsFromSuper = typeArgumentsFromSuper.get(parameter.getIndex());
            List<JetType> argTypesFromSuper = getTypes(projectionsFromSuper);
            boolean covariantPosition = effectiveProjectionKind == TypeCheckingProcedure.EnrichedProjectionKind.OUT;

            JetType type = modifyTypeAccordingToSuperMethods(argumentType, argTypesFromSuper, covariantPosition);
            Variance projectionKind = calculateArgumentProjectionKindFromSuper(argument, projectionsFromSuper);

            resultArguments.add(new TypeProjection(projectionKind, type));
        }
        return resultArguments;
    }

    private Variance calculateArgumentProjectionKindFromSuper(
            @NotNull TypeProjection argument,
            @NotNull List<TypeProjection> projectionsFromSuper
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
                reportError("Incompatible projection kinds in type arguments of super methods' return types: "
                            + projectionsFromSuper + ", defined in current: " + argument);
                return defaultProjectionKind;
            }
        }
        else {
            reportError("Incompatible projection kinds in type arguments of super methods' return types: " + projectionsFromSuper);
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

    private boolean typeMustBeNullable(
            @NotNull JetType autoType,
            @NotNull List<JetType> typesFromSuper,
            boolean covariantPosition
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
                reportError("Incompatible types in superclasses: " + typesFromSuper);
            }
        }

        assert someSupersNotNull || someSupersNullable; // we have at least one type, which is either not-null or nullable

        // This check may seem like voodoo magic, but it's not.
        // We want to handle case when parameter of super method is nullable, but parameter of sub method claims to be not null:
        // of course, it is an error in annotations, and parameter of sub method should be nullable.
        if (!covariantPosition && someSupersNullable && !autoType.isNullable()) {
            reportError("In superclass type is nullable: " + typesFromSuper + ", in subclass it is not: " + autoType);
            return true;
        }

        return someSupersNullable && autoType.isNullable();
    }

    @NotNull
    private ClassifierDescriptor modifyTypeClassifier(
            @NotNull JetType autoType,
            @NotNull List<JetType> typesFromSuper,
            boolean covariantPosition
    ) {
        ClassifierDescriptor classifier = autoType.getConstructor().getDeclarationDescriptor();
        if (!(classifier instanceof ClassDescriptor)) {
            assert classifier != null : "no declaration descriptor for type " + autoType;

            if (classifier instanceof TypeParameterDescriptor && autoTypeParameterToModified.containsKey(classifier)) {
                return autoTypeParameterToModified.get(classifier);
            }
            return classifier;
        }
        ClassDescriptor clazz = (ClassDescriptor) classifier;

        CollectionClassMapping collectionMapping = CollectionClassMapping.getInstance();

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

        if (covariantPosition) {
            if (collectionMapping.isMutableCollection(clazz)) {
                if (someSupersReadOnly && !someSupersMutable) {
                    return collectionMapping.convertMutableToReadOnly(clazz);
                }
            }
        }
        else {
            if (someSupersMutable == someSupersReadOnly) {
                //noinspection ConstantConditions
                if (someSupersMutable && someSupersReadOnly) {
                    reportError("Incompatible types in superclasses: " + typesFromSuper);
                }
                return classifier;
            }

            if (someSupersMutable && collectionMapping.isReadOnlyCollection(clazz)) {
                return collectionMapping.convertReadOnlyToMutable(clazz);
            }

            if (someSupersReadOnly && collectionMapping.isMutableCollection(clazz)) {
                return collectionMapping.convertMutableToReadOnly(clazz);
            }
        }
        return classifier;
    }

    private static boolean isArrayType(@NotNull JetType type) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        return builtIns.isArray(type) || builtIns.isPrimitiveArray(type);
    }

    private static class VarargCheckResult {
        public final JetType parameterType;
        public final boolean isVararg;

        private VarargCheckResult(JetType parameterType, boolean isVararg) {
            this.parameterType = parameterType;
            this.isVararg = isVararg;
        }
    }
}
