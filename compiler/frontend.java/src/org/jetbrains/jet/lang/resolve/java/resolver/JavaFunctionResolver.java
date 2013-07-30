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
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesPropagationData;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.*;
import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.*;

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
    SimpleFunctionDescriptor resolveFunctionMutely(@NotNull JavaMethod method, @NotNull ClassOrNamespaceDescriptor owner) {
        return resolveMethodToFunctionDescriptor(method, owner, false);
    }

    @Nullable
    private SimpleFunctionDescriptor resolveMethodToFunctionDescriptor(
            @NotNull JavaMethod javaMethod,
            @NotNull ClassOrNamespaceDescriptor ownerDescriptor,
            boolean record
    ) {
        PsiMethodWrapper method = new PsiMethodWrapper(javaMethod.getPsi());

        if (!DescriptorResolverUtils.isCorrectOwnerForEnumMember(ownerDescriptor, method.getPsiMember())) {
            return null;
        }

        PsiType returnPsiType = method.getReturnType();
        if (returnPsiType == null) {
            return null;
        }

        PsiMethod psiMethod = method.getPsiMethod();

        if (trace.get(BindingContext.FUNCTION, psiMethod) != null) {
            return trace.get(BindingContext.FUNCTION, psiMethod);
        }

        SimpleFunctionDescriptorImpl functionDescriptorImpl = new SimpleFunctionDescriptorImpl(
                ownerDescriptor,
                annotationResolver.resolveAnnotations(psiMethod),
                Name.identifier(method.getName()),
                CallableMemberDescriptor.Kind.DECLARATION
        );

        List<TypeParameterDescriptor> methodTypeParameters = signatureResolver.resolveMethodTypeParameters(method, functionDescriptorImpl);

        TypeVariableResolver methodTypeVariableResolver = new TypeVariableResolver(
                methodTypeParameters, functionDescriptorImpl, "method " + method.getName() + " in class " + psiMethod.getContainingClass());

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
                Modality.convertFromFlags(method.isAbstract(), !method.isFinal()),
                DescriptorResolverUtils.resolveVisibility(psiMethod),
                /*isInline = */ false
        );

        if (functionDescriptorImpl.getKind() == CallableMemberDescriptor.Kind.DECLARATION && record) {
            BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, psiMethod, functionDescriptorImpl);
        }

        if (record) {
            trace.record(JavaBindingContext.IS_DECLARED_IN_JAVA, functionDescriptorImpl);
        }

        if (!RawTypesCheck.hasRawTypesInHierarchicalSignature(psiMethod)
            && JavaMethodSignatureUtil.isMethodReturnTypeCompatible(psiMethod)
            && !containsErrorType(superFunctions, functionDescriptorImpl)) {
            if (signatureErrors.isEmpty()) {
                checkFunctionsOverrideCorrectly(method, superFunctions, functionDescriptorImpl);
            }
            else {
                if (record) {
                    trace.record(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, functionDescriptorImpl, signatureErrors);
                }
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
    public Set<FunctionDescriptor> resolveFunctionGroupForClass(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor owner) {
        Name methodName = members.getName();

        Set<SimpleFunctionDescriptor> functionsFromSupertypes = null;
        if (owner instanceof ClassDescriptor) {
            functionsFromSupertypes = getFunctionsFromSupertypes(methodName, owner);
        }

        Set<SimpleFunctionDescriptor> functionsFromCurrent = Sets.newHashSet();
        for (PsiMethodWrapper method : members.getMethods()) {
            SimpleFunctionDescriptor function = resolveMethodToFunctionDescriptor(new JavaMethod(method.getPsiMethod()), owner, true);
            if (function != null) {
                functionsFromCurrent.add(function);
                ContainerUtil.addIfNotNull(functionsFromCurrent, resolveSamAdapter(function));
            }
        }

        if (owner instanceof NamespaceDescriptor) {
            ContainerUtil.addIfNotNull(functionsFromCurrent, resolveSamConstructor((NamespaceDescriptor) owner, members));
        }


        final Set<FunctionDescriptor> fakeOverrides = new HashSet<FunctionDescriptor>();
        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            OverrideResolver.generateOverridesInFunctionGroup(methodName, functionsFromSupertypes, functionsFromCurrent, classDescriptor,
                                                              new OverrideResolver.DescriptorSink() {
                                                                  @Override
                                                                  public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                                                                      fakeOverrides.add((FunctionDescriptor) fakeOverride);
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

        OverrideResolver.resolveUnknownVisibilities(fakeOverrides, trace);

        Set<FunctionDescriptor> functions = Sets.newHashSet(fakeOverrides);
        if (isEnumClassObject(owner)) {
            for (FunctionDescriptor functionDescriptor : functionsFromCurrent) {
                if (!(isEnumValueOfMethod(functionDescriptor) || isEnumValuesMethod(functionDescriptor))) {
                    functions.add(functionDescriptor);
                }
            }
        }
        else {
            functions.addAll(functionsFromCurrent);
        }

        return functions;
    }

    @Nullable
    private static ClassDescriptorFromJvmBytecode findClassInScope(@NotNull JetScope memberScope, @NotNull Name name) {
        ClassifierDescriptor classifier = memberScope.getClassifier(name);
        if (classifier instanceof ClassDescriptorFromJvmBytecode) {
            return (ClassDescriptorFromJvmBytecode) classifier;
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
    private static ClassDescriptorFromJvmBytecode findClassInNamespace(@NotNull NamespaceDescriptor namespace, @NotNull Name name) {
        // First, try to find in namespace directly
        ClassDescriptorFromJvmBytecode found = findClassInScope(namespace.getMemberScope(), name);
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
            ClassDescriptorFromJvmBytecode klass = findClassInNamespace(ownerDescriptor, namedMembers.getName());
            if (klass != null) {
                return recordSamConstructor(klass, createSamConstructorFunction(ownerDescriptor, klass), trace);
            }
        }
        return null;
    }

    @Nullable
    private SimpleFunctionDescriptor resolveSamAdapter(@NotNull SimpleFunctionDescriptor original) {
        return isSamAdapterNecessary(original)
               ? recordSamAdapter(original, createSamAdapterFunction(original), trace)
               : null;
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForPackage(@NotNull NamedMembers members, @NotNull NamespaceDescriptor owner) {
        SimpleFunctionDescriptor samConstructor = resolveSamConstructor(owner, members);
        if (samConstructor != null) {
            return Collections.<FunctionDescriptor>singleton(samConstructor);
        }
        return Collections.emptySet();
    }

    @NotNull
    private JetType makeReturnType(
            PsiType returnType, PsiMethodWrapper method,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {

        TypeUsage typeUsage = JavaTypeTransformer
                .adjustTypeUsageWithMutabilityAnnotations(method.getPsiMethod(), TypeUsage.MEMBER_SIGNATURE_COVARIANT);
        JetType transformedType = typeTransformer.transformToType(returnType, typeUsage, typeVariableResolver);

        if (JavaAnnotationResolver.findAnnotationWithExternal(method.getPsiMethod(), JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION) != null) {
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
        if (ErrorUtils.containsErrorType(function)) {
            return true;
        }

        for (FunctionDescriptor superFunction : superFunctions) {
            if (ErrorUtils.containsErrorType(superFunction)) {
                return true;
            }
        }

        return false;
    }

    private static SimpleFunctionDescriptor recordSamConstructor(
            ClassDescriptorFromJvmBytecode klass,
            SimpleFunctionDescriptor constructorFunction,
            BindingTrace trace
    ) {
        trace.record(JavaBindingContext.SAM_CONSTRUCTOR_TO_INTERFACE, constructorFunction, klass);
        trace.record(BindingContext.SOURCE_DESCRIPTOR_FOR_SYNTHESIZED, constructorFunction, klass);
        return constructorFunction;
    }

    static <F extends FunctionDescriptor> F recordSamAdapter(F original, F adapterFunction, BindingTrace trace) {
        trace.record(JavaBindingContext.SAM_ADAPTER_FUNCTION_TO_ORIGINAL, adapterFunction, original);
        trace.record(BindingContext.SOURCE_DESCRIPTOR_FOR_SYNTHESIZED, adapterFunction, original);
        return adapterFunction;
    }
}