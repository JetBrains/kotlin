/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;
import static org.jetbrains.jet.lang.resolve.java.TypeUsage.*;
import static org.jetbrains.jet.lang.types.Variance.INVARIANT;

public class SignaturesPropagationData {
    private static final Logger LOG = Logger.getInstance(SignaturesPropagationData.class);

    private final List<TypeParameterDescriptor> modifiedTypeParameters;
    private final JavaDescriptorResolver.ValueParameterDescriptors modifiedValueParameters;
    private final JetType modifiedReturnType;

    private final List<String> signatureErrors = Lists.newArrayList();
    private final List<FunctionDescriptor> superFunctions;
    private final Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> autoTypeParameterToModified;
    final ClassDescriptor containingClass;

    public SignaturesPropagationData(
            @NotNull ClassDescriptor containingClass,
            @NotNull JetType autoReturnType, // type built by JavaTypeTransformer from Java signature and @NotNull annotations
            @NotNull JavaDescriptorResolver.ValueParameterDescriptors autoValueParameters, // descriptors built by parameters resolver
            @NotNull List<TypeParameterDescriptor> autoTypeParameters, // descriptors built by signature resolver
            @NotNull PsiMethodWrapper method,
            @NotNull BindingTrace trace
    ) {
        this.containingClass = containingClass;
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

    void reportError(String error) {
        signatureErrors.add(error);
    }

    private JetType modifyReturnTypeAccordingToSuperMethods(
            @NotNull JetType autoType // type built by JavaTypeTransformer
    ) {
        List<TypeAndVariance> typesFromSuperMethods = ContainerUtil.map(superFunctions,
                new Function<FunctionDescriptor, TypeAndVariance>() {
                    @Override
                    public TypeAndVariance fun(FunctionDescriptor superFunction) {
                        return new TypeAndVariance(superFunction.getReturnType(), Variance.OUT_VARIANCE);
                    }
                });

        return modifyTypeAccordingToSuperMethods(autoType, typesFromSuperMethods, MEMBER_SIGNATURE_COVARIANT);
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
                List<TypeAndVariance> upperBoundsFromSuperFunctions = Lists.newArrayList();

                for (Iterator<JetType> iterator : upperBoundFromSuperFunctionsIterators) {
                    assert iterator.hasNext();
                    upperBoundsFromSuperFunctions.add(new TypeAndVariance(iterator.next(), INVARIANT));
                }

                JetType modifiedUpperBound = modifyTypeAccordingToSuperMethods(autoUpperBound, upperBoundsFromSuperFunctions, UPPER_BOUND);
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
            List<TypeAndVariance> typesFromSuperMethods = ContainerUtil.map(superFunctions,
                    new Function<FunctionDescriptor, TypeAndVariance>() {
                        @Override
                        public TypeAndVariance fun(FunctionDescriptor superFunction) {
                            return new TypeAndVariance(superFunction.getValueParameters().get(index).getType(), INVARIANT);
                        }
                    });

            VarargCheckResult varargCheckResult =
                    checkVarargInSuperFunctions(originalParam);

            JetType altType = modifyTypeAccordingToSuperMethods(varargCheckResult.parameterType, typesFromSuperMethods, MEMBER_SIGNATURE_CONTRAVARIANT);

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
                    .substitute(originalReceiverType, INVARIANT);
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

        Map<ClassDescriptor, JetType> superclassToSupertype = getSuperclassToSupertypeMap(containingClass);

        for (HierarchicalMethodSignature superSignature : method.getPsiMethod().getHierarchicalMethodSignature().getSuperSignatures()) {
            PsiMethod superMethod = superSignature.getMethod();

            PsiClass psiClass = superMethod.getContainingClass();
            assert psiClass != null;
            String classFqName = psiClass.getQualifiedName();
            assert classFqName != null;

            if (!JavaToKotlinClassMap.getInstance().mapPlatformClass(new FqName(classFqName)).isEmpty()) {
                for (FunctionDescriptor superFun : JavaToKotlinMethodMap.INSTANCE.getFunctions(superMethod, containingClass)) {
                    superFunctions.add(substituteSuperFunction(superclassToSupertype, superFun));
                }
                continue;
            }

            PsiElement superDeclaration = superMethod instanceof JetClsMethod ? ((JetClsMethod) superMethod).getOrigin() : superMethod;
            DeclarationDescriptor superFun = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, superDeclaration);
            if (superFun == null) {
                reportCantFindSuperFunction(method);
                continue;
            }

            assert superFun instanceof FunctionDescriptor : superFun.getClass().getName();

            superFunctions.add(substituteSuperFunction(superclassToSupertype, (FunctionDescriptor) superFun));
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

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        if (someSupersVararg && originalVarargElementType == null) {
            // convert to vararg

            assert isArrayType(originalType);

            if (builtIns.isPrimitiveArray(originalType)) {
                // replace IntArray? with IntArray
                return new VarargCheckResult(TypeUtils.makeNotNullable(originalType), true);
            }

            // replace Array<out Foo>? with Array<Foo>
            JetType varargElementType = builtIns.getArrayElementType(originalType);
            return new VarargCheckResult(builtIns.getArrayType(INVARIANT, varargElementType), true);
        }
        else if (someSupersNotVararg && originalVarargElementType != null) {
            // convert to non-vararg

            assert isArrayType(originalType);

            if (builtIns.isPrimitiveArray(originalType)) {
                // replace IntArray with IntArray?
                return new VarargCheckResult(TypeUtils.makeNullable(originalType), false);
            }

            // replace Array<Foo> with Array<out Foo>?
            return new VarargCheckResult(TypeUtils.makeNullable(builtIns.getArrayType(Variance.OUT_VARIANCE, originalVarargElementType)),
                                         false);
        }

        return new VarargCheckResult(originalType, originalVarargElementType != null);
    }

    @NotNull
    private JetType modifyTypeAccordingToSuperMethods(
            @NotNull JetType autoType,
            @NotNull List<TypeAndVariance> typesFromSuper,
            @NotNull TypeUsage howThisTypeIsUsed
    ) {
        if (ErrorUtils.isErrorType(autoType)) {
            return autoType;
        }

        boolean resultNullable = typeMustBeNullable(autoType, typesFromSuper, howThisTypeIsUsed);
        ClassifierDescriptor resultClassifier = modifyTypeClassifier(autoType, typesFromSuper);
        List<TypeProjection> resultArguments = getTypeArgsOfType(autoType, resultClassifier, typesFromSuper);
        JetScope resultScope;
        if (resultClassifier instanceof ClassDescriptor) {
            resultScope = ((ClassDescriptor) resultClassifier).getMemberScope(resultArguments);
        }
        else {
            resultScope = autoType.getMemberScope();
        }

        JetTypeImpl type = new JetTypeImpl(autoType.getAnnotations(),
                                           resultClassifier.getTypeConstructor(),
                                           resultNullable,
                                           resultArguments,
                                           resultScope);

        PropagationHeuristics.checkArrayInReturnType(this, type, typesFromSuper);
        return type;
    }

    @NotNull
    private List<TypeProjection> getTypeArgsOfType(
            @NotNull JetType autoType,
            @NotNull ClassifierDescriptor classifier,
            @NotNull List<TypeAndVariance> typesFromSuper
    ) {
        List<TypeProjection> autoArguments = autoType.getArguments();

        if (!(classifier instanceof ClassDescriptor)) {
            assert autoArguments.isEmpty() :
                    "Unexpected type arguments when type constructor is not ClassDescriptor, type = " + autoType;
            return autoArguments;
        }

        List<List<TypeProjectionAndVariance>> typeArgumentsFromSuper = calculateTypeArgumentsFromSuper((ClassDescriptor) classifier,
                                                                                                       typesFromSuper);

        // Modify type arguments using info from typesFromSuper
        List<TypeProjection> resultArguments = Lists.newArrayList();
        for (TypeParameterDescriptor parameter : classifier.getTypeConstructor().getParameters()) {
            TypeProjection argument = autoArguments.get(parameter.getIndex());

            JetType argumentType = argument.getType();
            List<TypeProjectionAndVariance> projectionsFromSuper = typeArgumentsFromSuper.get(parameter.getIndex());
            List<TypeAndVariance> argTypesFromSuper = getTypes(projectionsFromSuper);

            JetType type = modifyTypeAccordingToSuperMethods(argumentType, argTypesFromSuper, TYPE_ARGUMENT);
            Variance projectionKind = calculateArgumentProjectionKindFromSuper(argument, projectionsFromSuper);

            resultArguments.add(new TypeProjection(projectionKind, type));
        }
        return resultArguments;
    }

    private Variance calculateArgumentProjectionKindFromSuper(
            @NotNull TypeProjection argument,
            @NotNull List<TypeProjectionAndVariance> projectionsFromSuper
    ) {
        Set<Variance> projectionKindsInSuper = Sets.newLinkedHashSet();
        for (TypeProjectionAndVariance projectionAndVariance : projectionsFromSuper) {
            projectionKindsInSuper.add(projectionAndVariance.typeProjection.getProjectionKind());
        }

        Variance defaultProjectionKind = argument.getProjectionKind();
        if (projectionKindsInSuper.size() == 0) {
            return defaultProjectionKind;
        }
        else if (projectionKindsInSuper.size() == 1) {
            Variance projectionKindInSuper = projectionKindsInSuper.iterator().next();
            if (defaultProjectionKind == INVARIANT || defaultProjectionKind == projectionKindInSuper) {
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
    private static List<TypeAndVariance> getTypes(@NotNull List<TypeProjectionAndVariance> projections) {
        List<TypeAndVariance> types = Lists.newArrayList();
        for (TypeProjectionAndVariance projection : projections) {
            types.add(new TypeAndVariance(projection.typeProjection.getType(),
                                          merge(projection.varianceOfPosition, projection.typeProjection.getProjectionKind())));
        }
        return types;
    }

    private static Variance merge(Variance positionOfOuter, Variance projectionKind) {
        // Inv<Inv<out X>>, X is in invariant position
        if (positionOfOuter == INVARIANT) return INVARIANT;
        // Out<X>, X is in out-position
        if (projectionKind == INVARIANT) return positionOfOuter;
        // Out<Out<X>>, X is in out-position
        // In<In<X>>, X is in out-position
        // Out<In<X>>, X is in in-position
        // In<Out<X>>, X is in in-position
        return positionOfOuter.superpose(projectionKind);
    }

    // Returns list with type arguments info from supertypes
    // Example:
    //     - Foo<A, B> is a subtype of Bar<A, List<B>>, Baz<Boolean, A>
    //     - input: klass = Foo, typesFromSuper = [Bar<String, List<Int>>, Baz<Boolean, CharSequence>]
    //     - output[0] = [String, CharSequence], output[1] = []
    private static List<List<TypeProjectionAndVariance>> calculateTypeArgumentsFromSuper(
            @NotNull ClassDescriptor klass,
            @NotNull Collection<TypeAndVariance> typesFromSuper
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
        List<List<TypeProjectionAndVariance>> parameterToArgumentsFromSuper = Lists.newArrayList();
        for (TypeParameterDescriptor ignored : klass.getTypeConstructor().getParameters()) {
            parameterToArgumentsFromSuper.add(new ArrayList<TypeProjectionAndVariance>());
        }

        // Enumerate all types from super and all its parameters
        for (TypeAndVariance typeFromSuper : typesFromSuper) {
            for (TypeParameterDescriptor parameter : typeFromSuper.type.getConstructor().getParameters()) {
                TypeProjection argument = typeFromSuper.type.getArguments().get(parameter.getIndex());

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
                        Variance effectiveVariance = parameter.getVariance().superpose(typeFromSuper.varianceOfPosition);
                        parameterToArgumentsFromSuper.get(parameterIndex).add(new TypeProjectionAndVariance(argument, effectiveVariance));
                    }
                }
            }
        }
        return parameterToArgumentsFromSuper;
    }

    private boolean typeMustBeNullable(
            @NotNull JetType autoType,
            @NotNull List<TypeAndVariance> typesFromSuper,
            @NotNull TypeUsage howThisTypeIsUsed
    ) {
        boolean someSupersNotCovariantNullable = false;
        boolean someSupersCovariantNullable = false;
        boolean someSupersNotNull = false;
        for (TypeAndVariance typeFromSuper : typesFromSuper) {
            if (!typeFromSuper.type.isNullable()) {
                someSupersNotNull = true;
            }
            else {
                if (typeFromSuper.varianceOfPosition == Variance.OUT_VARIANCE) {
                    someSupersCovariantNullable = true;
                }
                else {
                    someSupersNotCovariantNullable = true;
                }
            }
        }

        if (someSupersNotNull && someSupersNotCovariantNullable) {
            reportError("Incompatible types in superclasses: " + typesFromSuper);
            return autoType.isNullable();
        }
        else if (someSupersNotNull) {
            return false;
        }
        else if (someSupersNotCovariantNullable || someSupersCovariantNullable) {
            boolean annotatedAsNotNull = howThisTypeIsUsed != TYPE_ARGUMENT && !autoType.isNullable();

            if (annotatedAsNotNull && someSupersNotCovariantNullable) {
                reportError("In superclass type is nullable: " + typesFromSuper + ", in subclass it is not: " + autoType);
                return true;
            }

            return !annotatedAsNotNull;
        }
        return autoType.isNullable();
    }

    @NotNull
    private ClassifierDescriptor modifyTypeClassifier(
            @NotNull JetType autoType,
            @NotNull List<TypeAndVariance> typesFromSuper
    ) {
        ClassifierDescriptor classifier = autoType.getConstructor().getDeclarationDescriptor();
        if (!(classifier instanceof ClassDescriptor)) {
            assert classifier != null : "no declaration descriptor for type " + autoType;

            if (classifier instanceof TypeParameterDescriptor && autoTypeParameterToModified.containsKey(classifier)) {
                return autoTypeParameterToModified.get(classifier);
            }
            return classifier;
        }
        ClassDescriptor klass = (ClassDescriptor) classifier;

        CollectionClassMapping collectionMapping = CollectionClassMapping.getInstance();

        boolean someSupersMutable = false;
        boolean someSupersCovariantReadOnly = false;
        boolean someSupersNotCovariantReadOnly = false;
        for (TypeAndVariance typeFromSuper : typesFromSuper) {
            ClassifierDescriptor classifierFromSuper = typeFromSuper.type.getConstructor().getDeclarationDescriptor();
            if (classifierFromSuper instanceof ClassDescriptor) {
                ClassDescriptor classFromSuper = (ClassDescriptor) classifierFromSuper;

                if (collectionMapping.isMutableCollection(classFromSuper)) {
                    someSupersMutable = true;
                }
                else if (collectionMapping.isReadOnlyCollection(classFromSuper)) {
                    if (typeFromSuper.varianceOfPosition == Variance.OUT_VARIANCE) {
                        someSupersCovariantReadOnly = true;
                    }
                    else {
                        someSupersNotCovariantReadOnly = true;
                    }
                }
            }
        }

        if (someSupersMutable && someSupersNotCovariantReadOnly) {
            reportError("Incompatible types in superclasses: " + typesFromSuper);
            return classifier;
        }
        else if (someSupersMutable) {
            if (collectionMapping.isReadOnlyCollection(klass)) {
                return collectionMapping.convertReadOnlyToMutable(klass);
            }
        }
        else if (someSupersNotCovariantReadOnly || someSupersCovariantReadOnly) {
            if (collectionMapping.isMutableCollection(klass)) {
                return collectionMapping.convertMutableToReadOnly(klass);
            }
        }

        ClassifierDescriptor fixed = PropagationHeuristics.tryToFixOverridingTWithRawType(this, typesFromSuper);
        return fixed != null ? fixed : classifier;
    }

    private static Map<ClassDescriptor, JetType> getSuperclassToSupertypeMap(ClassDescriptor containingClass) {
        Map<ClassDescriptor, JetType> superclassToSupertype = Maps.newHashMap();
        for (JetType supertype : TypeUtils.getAllSupertypes(containingClass.getDefaultType())) {
            ClassifierDescriptor superclass = supertype.getConstructor().getDeclarationDescriptor();
            assert superclass instanceof ClassDescriptor;
            superclassToSupertype.put((ClassDescriptor) superclass, supertype);
        }
        return superclassToSupertype;
    }

    @NotNull
    private static FunctionDescriptor substituteSuperFunction(
            @NotNull Map<ClassDescriptor, JetType> superclassToSupertype,
            @NotNull FunctionDescriptor superFun
    ) {
        DeclarationDescriptor superFunContainer = superFun.getContainingDeclaration();
        assert superFunContainer instanceof ClassDescriptor: superFunContainer;

        JetType supertype = superclassToSupertype.get(superFunContainer);
        assert supertype != null : "Couldn't find super type for super function: " + superFun;
        TypeSubstitutor supertypeSubstitutor = TypeSubstitutor.create(supertype);

        FunctionDescriptor substitutedSuperFun = superFun.substitute(supertypeSubstitutor);
        assert substitutedSuperFun != null;
        return substitutedSuperFun;
    }

    private static boolean isArrayType(@NotNull JetType type) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        return builtIns.isArray(type) || builtIns.isPrimitiveArray(type);
    }

    private static void reportCantFindSuperFunction(PsiMethodWrapper method) {
        String errorMessage = "Can't find super function for " + method.getPsiMethod() +
                              " defined in " + method.getPsiMethod().getContainingClass();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            throw new IllegalStateException(errorMessage);
        }
        else {
            if (SystemInfo.isMac) {
                LOG.error("Remove duplicates from your JDK definition\n" + errorMessage);
            }
            else {
                LOG.error(errorMessage);
            }
        }
    }

    private static class VarargCheckResult {
        public final JetType parameterType;
        public final boolean isVararg;

        public VarargCheckResult(JetType parameterType, boolean isVararg) {
            this.parameterType = parameterType;
            this.isVararg = isVararg;
        }
    }

    private static class TypeProjectionAndVariance {
        public final TypeProjection typeProjection;
        public final Variance varianceOfPosition;

        public TypeProjectionAndVariance(TypeProjection typeProjection, Variance varianceOfPosition) {
            this.typeProjection = typeProjection;
            this.varianceOfPosition = varianceOfPosition;
        }

        public String toString() {
            return typeProjection.toString();
        }
    }

    static class TypeAndVariance {
        public final JetType type;
        public final Variance varianceOfPosition;

        public TypeAndVariance(JetType type, Variance varianceOfPosition) {
            this.type = type;
            this.varianceOfPosition = varianceOfPosition;
        }

        public String toString() {
            return type.toString();
        }
    }
}
