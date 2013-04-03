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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesPropagationData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.provider.PackagePsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.*;
import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.JAVA;
import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public final class JavaFunctionResolver {
    private static final Logger LOG = Logger.getInstance(JavaFunctionResolver.class);

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
            @NotNull PsiClass psiClass, PsiMethodWrapper method,
            @NotNull PsiDeclarationProvider scopeData, @NotNull ClassOrNamespaceDescriptor ownerDescriptor
    ) {
        if (!DescriptorResolverUtils.isCorrectOwnerForEnumMember(ownerDescriptor, method.getPsiMember())) {
            return null;
        }

        PsiType returnPsiType = method.getReturnType();
        if (returnPsiType == null) {
            return null;
        }

        // TODO: ugly
        if (method.getJetMethodAnnotation().hasPropertyFlag()) {
            return null;
        }

        PsiMethod psiMethod = method.getPsiMethod();
        PsiClass containingClass = psiMethod.getContainingClass();
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

        List<String> signatureErrors = Lists.newArrayList();

        List<FunctionDescriptor> superFunctions;
        if (ownerDescriptor instanceof ClassDescriptor) {
            SignaturesPropagationData signaturesPropagationData = new SignaturesPropagationData(
                    (ClassDescriptor) ownerDescriptor, returnType, valueParameterDescriptors, methodTypeParameters, method, trace);
            superFunctions = signaturesPropagationData.getSuperFunctions();

            returnType = signaturesPropagationData.getModifiedReturnType();
            valueParameterDescriptors = signaturesPropagationData.getModifiedValueParameters();
            methodTypeParameters = signaturesPropagationData.getModifiedTypeParameters();

            signatureErrors.addAll(signaturesPropagationData.getSignatureErrors());
        }
        else {
            superFunctions = Collections.emptyList();
        }

        AlternativeMethodSignatureData alternativeMethodSignatureData =
                new AlternativeMethodSignatureData(method, valueParameterDescriptors, returnType, methodTypeParameters,
                                                   !superFunctions.isEmpty());
        if (alternativeMethodSignatureData.isAnnotated() && !alternativeMethodSignatureData.hasErrors()) {
            valueParameterDescriptors = alternativeMethodSignatureData.getValueParameters();
            returnType = alternativeMethodSignatureData.getReturnType();
            methodTypeParameters = alternativeMethodSignatureData.getTypeParameters();
        }
        else if (alternativeMethodSignatureData.hasErrors()) {
            signatureErrors.add(alternativeMethodSignatureData.getError());
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

        if (!RawTypesCheck.hasRawTypesInHierarchicalSignature(psiMethod)
                && JavaMethodSignatureUtil.isMethodReturnTypeCompatible(psiMethod)
                && !containsErrorType(superFunctions, functionDescriptorImpl)) {
            if (signatureErrors.isEmpty()) {
                checkFunctionsOverrideCorrectly(method, superFunctions, functionDescriptorImpl);
            }
            else {
                trace.record(BindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, functionDescriptorImpl, signatureErrors);
            }
        }

        return functionDescriptorImpl;
    }

    private static void checkFunctionsOverrideCorrectly(
            PsiMethodWrapper method,
            List<FunctionDescriptor> superFunctions,
            FunctionDescriptor functionDescriptor
    ) {
        for (FunctionDescriptor superFunction : superFunctions) {
            ClassDescriptor klass = (ClassDescriptor) functionDescriptor.getContainingDeclaration();
            List<TypeSubstitution> substitutions = Lists.newArrayList();
            while (true) {
                substitutions.add(SubstitutionUtils.buildDeepSubstitutor(klass.getDefaultType()).getSubstitution());
                if (!klass.isInner()) {
                    break;
                }
                klass = (ClassDescriptor) klass.getContainingDeclaration();
            }
            TypeSubstitutor substitutor = TypeSubstitutor.create(substitutions.toArray(new TypeSubstitution[substitutions.size()]));
            FunctionDescriptor superFunctionSubstituted = superFunction.substitute(substitutor);

            assert superFunctionSubstituted != null :
                    "Couldn't substitute super function: " + superFunction + ", substitutor = " + substitutor;

            OverrideCompatibilityInfo.Result overridableResult =
                    isOverridableBy(superFunctionSubstituted, functionDescriptor).getResult();
            boolean paramsOk = overridableResult == OverrideCompatibilityInfo.Result.OVERRIDABLE;
            boolean returnTypeOk =
                    isReturnTypeOkForOverride(JetTypeChecker.INSTANCE, superFunctionSubstituted, functionDescriptor);
            if (!paramsOk || !returnTypeOk) {
                LOG.error("Loaded Java method overrides another, but resolved as Kotlin function, doesn't.\n"
                          + "super function = " + superFunction + "\n"
                          + "super class = " + superFunction.getContainingDeclaration() + "\n"
                          + "sub function = " + functionDescriptor + "\n"
                          + "sub class = " + functionDescriptor.getContainingDeclaration() + "\n"
                          + "sub method = " + PsiFormatUtil.getExternalName(method.getPsiMethod()) + "\n"
                          + "@KotlinSignature = " + method.getSignatureAnnotation().signature());
            }
        }
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

        if (owner instanceof NamespaceDescriptor) {
            SimpleFunctionDescriptor samConstructor = resolveSamConstructor((NamespaceDescriptor) owner, namedMembers);
            if (samConstructor != null) {
                functionsFromCurrent.add(samConstructor);
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

        if (isEnumClassObject(owner)) {
            for (FunctionDescriptor functionDescriptor : Lists.newArrayList(functions)) {
                if (isEnumValueOfMethod(functionDescriptor) || isEnumValuesMethod(functionDescriptor)) {
                    functions.remove(functionDescriptor);
                }
            }
        }

        return functions;
    }

    @Nullable
    private static ClassDescriptor findClassInScope(@NotNull JetScope memberScope, @NotNull Name name) {
        ClassifierDescriptor classifier = memberScope.getClassifier(name);
        if (classifier instanceof ClassDescriptor) {
            return (ClassDescriptor) classifier;
        }
        return null;
    }

    // E.g. we have foo.Bar.Baz class declared in Java. It will produce the following descriptors structure:
    // namespace foo
    // +-- class Bar
    // |    +-- class Baz
    // +-- namespace Bar
    // We need to find class 'Baz' in namespace 'foo.Bar'.
    @Nullable
    private static ClassDescriptor findClassInNamespace(@NotNull NamespaceDescriptor namespace, @NotNull Name name) {
        // First, try to find in namespace directly
        ClassDescriptor found = findClassInScope(namespace.getMemberScope(), name);
        if (found != null) {
            return found;
        }

        // If unsuccessful, try to find class of the same name as current (class 'foo.Bar')
        NamespaceDescriptorParent parent = namespace.getContainingDeclaration();
        if (parent instanceof NamespaceDescriptor) {
            // Calling recursively, looking for 'Bar' in 'foo'
            ClassDescriptor classForCurrentNamespace = findClassInNamespace((NamespaceDescriptor) parent, namespace.getName());
            if (classForCurrentNamespace == null) {
                return null;
            }

            // Try to find nested class 'Baz' in class 'foo.Bar'
            return findClassInScope(DescriptorUtils.getStaticNestedClassesScope(classForCurrentNamespace), name);
        }
        return null;
    }

    @Nullable
    private SimpleFunctionDescriptor resolveSamConstructor(
            @NotNull NamespaceDescriptor ownerDescriptor,
            @NotNull NamedMembers namedMembers
    ) {
        PsiClass samInterface = namedMembers.getSamInterface();
        if (samInterface != null) {
            ClassDescriptor klass = findClassInNamespace(ownerDescriptor, namedMembers.getName());
            if (klass != null && SingleAbstractMethodUtils.isSamInterface(klass)) {
                SimpleFunctionDescriptor constructorFunction = SingleAbstractMethodUtils.createSamConstructorFunction(ownerDescriptor,
                                                                                                                      klass);
                trace.record(BindingContext.SAM_CONSTRUCTOR_TO_INTERFACE, constructorFunction, klass);
                trace.record(BindingContext.SOURCE_DESCRIPTOR_FOR_SYNTHESIZED, constructorFunction, klass);
                return constructorFunction;
            }
        }
        return null;
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
    public Set<FunctionDescriptor> resolveFunctionGroup(
            @NotNull Name functionName,
            @NotNull PackagePsiDeclarationProvider scopeData,
            @NotNull NamespaceDescriptor ownerDescriptor
    ) {
        NamedMembers namedMembers = scopeData.getMembersCache().get(functionName);
        if (namedMembers != null) {
            SimpleFunctionDescriptor samConstructor = resolveSamConstructor(ownerDescriptor, namedMembers);
            if (samConstructor != null) {
                return Collections.<FunctionDescriptor>singleton(samConstructor);
            }
        }
        return Collections.emptySet();
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
            TypeUsage typeUsage = JavaTypeTransformer.adjustTypeUsageWithMutabilityAnnotations(method.getPsiMethod(), TypeUsage.MEMBER_SIGNATURE_COVARIANT);
            transformedType = typeTransformer.transformToType(returnType, typeUsage, typeVariableResolver);
        }

        if (JavaAnnotationResolver.findAnnotationWithExternal(method.getPsiMethod(), JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION.getFqName().getFqName()) !=
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

    private static boolean containsErrorType(@NotNull List<FunctionDescriptor> superFunctions, @NotNull FunctionDescriptor function) {
        if (containsErrorType(function)) {
            return true;
        }

        for (FunctionDescriptor superFunction : superFunctions) {
            if (containsErrorType(superFunction)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsErrorType(@NotNull FunctionDescriptor function) {
        if (ErrorUtils.containsErrorType(function.getReturnType())) {
            return true;
        }
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            if (ErrorUtils.containsErrorType(parameter.getType())) {
                return true;
            }
        }
        for (TypeParameterDescriptor parameter : function.getTypeParameters()) {
            for (JetType upperBound : parameter.getUpperBounds()) {
                if (ErrorUtils.containsErrorType(upperBound)) {
                    return true;
                }
            }
        }

        return false;
    }
}