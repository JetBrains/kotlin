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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.JAVA;
import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public final class JavaFunctionResolver {

    private JavaTypeTransformer typeTransformer;
    private BindingTrace trace;
    private JavaSignatureResolver signatureResolver;
    private JavaValueParameterResolver parameterResolver;
    private JavaAnnotationResolver annotationResolver;

    public JavaFunctionResolver() {
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setSignatureResolver(JavaSignatureResolver signatureResolver) {
        this.signatureResolver = signatureResolver;
    }

    @Inject
    public void setParameterResolver(JavaValueParameterResolver parameterResolver) {
        this.parameterResolver = parameterResolver;
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Nullable
    private SimpleFunctionDescriptor resolveMethodToFunctionDescriptor(
            @NotNull final PsiClass psiClass, final PsiMethodWrapper method,
            @NotNull PsiDeclarationProvider scopeData, @NotNull ClassOrNamespaceDescriptor ownerDescriptor
    ) {
        PsiType returnPsiType = method.getReturnType();
        if (returnPsiType == null) {
            return null;
        }

        // TODO: ugly
        if (method.getJetMethodAnnotation().hasPropertyFlag()) {
            return null;
        }

        final PsiMethod psiMethod = method.getPsiMethod();
        final PsiClass containingClass = psiMethod.getContainingClass();
        if (scopeData.getDeclarationOrigin() == KOTLIN) {
            // TODO: unless maybe class explicitly extends Object
            assert containingClass != null;
            String ownerClassName = containingClass.getQualifiedName();
            if (DescriptorResolverUtils.OBJECT_FQ_NAME.getFqName().equals(ownerClassName)) {
                return null;
            }
        }

        if (trace.get(BindingContext.FUNCTION, psiMethod) != null) {
            return trace.get(BindingContext.FUNCTION, psiMethod);
        }

        SimpleFunctionDescriptorImpl functionDescriptorImpl = new SimpleFunctionDescriptorImpl(
                ownerDescriptor,
                annotationResolver.resolveAnnotations(psiMethod),
                Name.identifier(method.getName()),
                DescriptorKindUtils.flagsToKind(method.getJetMethodAnnotation().kind())
        );

        String context = "method " + method.getName() + " in class " + psiClass.getQualifiedName();

        List<TypeParameterDescriptor> methodTypeParameters =
                signatureResolver.resolveMethodTypeParameters(method,
                                                              functionDescriptorImpl);

        TypeVariableResolver methodTypeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(methodTypeParameters,
                                                                                                                       functionDescriptorImpl,
                                                                                                                       context);


        JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors = parameterResolver
                .resolveParameterDescriptors(functionDescriptorImpl, method.getParameters(), methodTypeVariableResolver);
        JetType returnType = makeReturnType(returnPsiType, method, methodTypeVariableResolver);

        Set<JetType> typesFromSuperMethods = Sets.newHashSet();
        for (HierarchicalMethodSignature superSignature : method.getPsiMethod().getHierarchicalMethodSignature().getSuperSignatures()) {
            DeclarationDescriptor superFun = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, superSignature.getMethod());
            if (superFun instanceof FunctionDescriptor) {
                typesFromSuperMethods.add(((FunctionDescriptor) superFun).getReturnType());
            }
        }

        returnType = modifyReturnTypeAccordingToSuperMethods(returnType, typesFromSuperMethods, true);

        // TODO consider better place for this check
        AlternativeMethodSignatureData alternativeMethodSignatureData =
                new AlternativeMethodSignatureData(method, valueParameterDescriptors, returnType, methodTypeParameters);
        if (alternativeMethodSignatureData.isAnnotated() && !alternativeMethodSignatureData.hasErrors()) {
            valueParameterDescriptors = alternativeMethodSignatureData.getValueParameters();
            returnType = alternativeMethodSignatureData.getReturnType();
            methodTypeParameters = alternativeMethodSignatureData.getTypeParameters();
        }
        else if (alternativeMethodSignatureData.hasErrors()) {
            trace.record(BindingContext.ALTERNATIVE_SIGNATURE_DATA_ERROR, functionDescriptorImpl,
                         alternativeMethodSignatureData.getError());
        }

        functionDescriptorImpl.initialize(
                valueParameterDescriptors.getReceiverType(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(ownerDescriptor),
                methodTypeParameters,
                valueParameterDescriptors.getDescriptors(),
                returnType,
                DescriptorResolverUtils.resolveModality(method, method.isFinal()),
                DescriptorResolverUtils.resolveVisibility(psiMethod, method.getJetMethodAnnotation()),
                /*isInline = */ false
        );

        if (functionDescriptorImpl.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, psiMethod, functionDescriptorImpl);
        }

        if (scopeData.getDeclarationOrigin() == JAVA) {
            trace.record(BindingContext.IS_DECLARED_IN_JAVA, functionDescriptorImpl);
        }

        if (containingClass != psiClass && !method.isStatic()) {
            throw new IllegalStateException("non-static method in subclass");
        }
        return functionDescriptorImpl;
    }

    @NotNull
    private Set<FunctionDescriptor> resolveNamedGroupFunctions(
            @NotNull ClassOrNamespaceDescriptor owner, @NotNull PsiClass psiClass,
            NamedMembers namedMembers, Name methodName, PsiDeclarationProvider scopeData
    ) {
        final Set<FunctionDescriptor> functions = new HashSet<FunctionDescriptor>();

        Set<SimpleFunctionDescriptor> functionsFromSupertypes = null;
        if (owner instanceof ClassDescriptor) {
            functionsFromSupertypes = getFunctionsFromSupertypes(methodName, owner);
        }

        Set<SimpleFunctionDescriptor> functionsFromCurrent = Sets.newHashSet();
        for (PsiMethodWrapper method : namedMembers.getMethods()) {
            SimpleFunctionDescriptor function = resolveMethodToFunctionDescriptor(psiClass, method, scopeData, owner);
            if (function != null) {
                functionsFromCurrent.add(function);
            }
        }

        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            OverrideResolver.generateOverridesInFunctionGroup(methodName, functionsFromSupertypes, functionsFromCurrent, classDescriptor,
                  new OverrideResolver.DescriptorSink() {
                      @Override
                      public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                          functions.add((FunctionDescriptor) fakeOverride);
                      }

                      @Override
                      public void conflict(
                              @NotNull CallableMemberDescriptor fromSuper,
                              @NotNull CallableMemberDescriptor fromCurrent
                      ) {
                          // nop
                      }
                  });
        }

        OverrideResolver.resolveUnknownVisibilities(functions, trace);
        functions.addAll(functionsFromCurrent);

        if (DescriptorUtils.isEnumClassObject(owner)) {
            for (FunctionDescriptor functionDescriptor : Lists.newArrayList(functions)) {
                if (isEnumSpecialMethod(functionDescriptor)) {
                    functions.remove(functionDescriptor);
                }
            }
        }

        return functions;
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroup(
            @NotNull Name methodName,
            @NotNull ClassPsiDeclarationProvider scopeData,
            @NotNull ClassOrNamespaceDescriptor ownerDescriptor
    ) {

        NamedMembers namedMembers = scopeData.getMembersCache().get(methodName);
        if (namedMembers == null) {
            return Collections.emptySet();
        }
        PsiClass psiClass = scopeData.getPsiClass();
        return resolveNamedGroupFunctions(ownerDescriptor, psiClass, namedMembers, methodName, scopeData);
    }

    @NotNull
    private JetType makeReturnType(
            PsiType returnType, PsiMethodWrapper method,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {

        String returnTypeFromAnnotation = method.getJetMethodAnnotation().returnType();

        JetType transformedType;
        if (returnTypeFromAnnotation.length() > 0) {
            transformedType = typeTransformer.transformToType(returnTypeFromAnnotation, typeVariableResolver);
        }
        else {
            transformedType = typeTransformer.transformToType(
                    returnType, JavaTypeTransformer.TypeUsage.MEMBER_SIGNATURE_COVARIANT, typeVariableResolver);
        }

        if (JavaAnnotationResolver.findAnnotation(method.getPsiMethod(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) !=
            null) {
            return TypeUtils.makeNullableAsSpecified(transformedType, false);
        }
        else {
            return transformedType;
        }
    }

    @NotNull
    private static Set<SimpleFunctionDescriptor> getFunctionsFromSupertypes(
            @NotNull Name methodName, @NotNull ClassOrNamespaceDescriptor classOrNamespaceDescriptor
    ) {
        Set<SimpleFunctionDescriptor> r = Sets.newLinkedHashSet();
        for (JetType supertype : DescriptorResolverUtils.getSupertypes(classOrNamespaceDescriptor)) {
            for (FunctionDescriptor function : supertype.getMemberScope().getFunctions(methodName)) {
                r.add((SimpleFunctionDescriptor) function);
            }
        }
        return r;
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
        Multimap<TypeConstructor,TypeProjection> substitution = SubstitutionUtils.buildDeepSubstitutionMultimap(
                TypeUtils.makeUnsubstitutedType(klass, null));

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

        if (!someSupersNotNull && !someSupersNullable) { // no types from super
            return autoType.isNullable();
        }

        return someSupersNullable && autoType.isNullable();
    }

    @NotNull
    private static ClassifierDescriptor getReturnTypeClassifier(@NotNull JetType autoType, @NotNull Collection<JetType> typesFromSuper) {
        ClassifierDescriptor classifier = autoType.getConstructor().getDeclarationDescriptor();
        if (!(classifier instanceof ClassDescriptor)) {
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

    private static boolean isEnumSpecialMethod(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> methodTypeParameters = functionDescriptor.getValueParameters();
        String methodName = functionDescriptor.getName().getName();
        JetType nullableString = TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getStringType());
        if (methodName.equals("valueOf") && methodTypeParameters.size() == 1
            && JetTypeChecker.INSTANCE.isSubtypeOf(methodTypeParameters.get(0).getType(), nullableString)) {
            return true;
        }
        return (methodName.equals("values") && methodTypeParameters.isEmpty());
    }
}