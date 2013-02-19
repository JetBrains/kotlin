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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesPropagationData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.SubstitutionUtils;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        final SimpleFunctionDescriptorImpl functionDescriptorImpl = new SimpleFunctionDescriptorImpl(
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

        final List<String> signatureErrors = Lists.newArrayList();

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

        if (!RawTypesCheck.hasRawTypesInHierarchicalSignature(psiMethod) && JavaMethodSignatureUtil.isMethodReturnTypeCompatible(psiMethod)) {
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
            TypeSubstitutor substitutor = SubstitutionUtils.buildDeepSubstitutor(
                    ((ClassDescriptor) functionDescriptor.getContainingDeclaration()).getDefaultType());
            FunctionDescriptor superFunctionSubstituted = superFunction.substitute(substitutor);

            assert superFunctionSubstituted != null :
                    "Couldn't substitute super function: " + superFunction + ", substitutor = " + substitutor;

            OverrideCompatibilityInfo.Result overridableResult =
                    isOverridableBy(superFunctionSubstituted, functionDescriptor).getResult();
            boolean paramsOk = overridableResult == OverrideCompatibilityInfo.Result.OVERRIDABLE;
            boolean returnTypeOk =
                    isReturnTypeOkForOverride(JetTypeChecker.INSTANCE, superFunctionSubstituted, functionDescriptor);
            if (!paramsOk || !returnTypeOk) {
                String errorMessage = "Loaded Java method overrides another, but resolved as Kotlin function, doesn't.\n"
                                      + "super function = " + superFunction + "\n"
                                      + "super class = " + superFunction.getContainingDeclaration() + "\n"
                                      + "sub function = " + functionDescriptor + "\n"
                                      + "sub class = " + functionDescriptor.getContainingDeclaration() + "\n"
                                      + "sub method = " + PsiFormatUtil.getExternalName(method.getPsiMethod()) + "\n"
                                      + "@KotlinSignature = " + method.getSignatureAnnotation().signature();

                if (ApplicationManager.getApplication().isUnitTestMode()) {
                    throw new IllegalStateException(errorMessage);
                }
                else {
                    LOG.error(errorMessage);
                }
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
                    returnType, TypeUsage.MEMBER_SIGNATURE_COVARIANT, typeVariableResolver);
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