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
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.TypeUsage;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaMethodDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamAdapterDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.SignaturesUtil;
import org.jetbrains.jet.lang.resolve.java.scope.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.OverridingUtil.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils.resolveOverrides;
import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.*;

public final class JavaFunctionResolver {
    private static final Logger LOG = Logger.getInstance(JavaFunctionResolver.class);

    private JavaTypeTransformer typeTransformer;
    private BindingTrace trace;
    private JavaResolverCache cache;
    private JavaTypeParameterResolver typeParameterResolver;
    private JavaValueParameterResolver valueParameterResolver;
    private JavaAnnotationResolver annotationResolver;
    private ExternalSignatureResolver externalSignatureResolver;

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setCache(JavaResolverCache cache) {
        this.cache = cache;
    }

    @Inject
    public void setTypeParameterResolver(JavaTypeParameterResolver typeParameterResolver) {
        this.typeParameterResolver = typeParameterResolver;
    }

    @Inject
    public void setValueParameterResolver(JavaValueParameterResolver valueParameterResolver) {
        this.valueParameterResolver = valueParameterResolver;
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setExternalSignatureResolver(ExternalSignatureResolver externalSignatureResolver) {
        this.externalSignatureResolver = externalSignatureResolver;
    }

    @Nullable
    SimpleFunctionDescriptor resolveFunctionMutely(@NotNull JavaMethod method, @NotNull ClassOrNamespaceDescriptor owner) {
        return resolveMethodToFunctionDescriptor(method, owner, false);
    }

    @Nullable
    private SimpleFunctionDescriptor resolveMethodToFunctionDescriptor(
            @NotNull JavaMethod method,
            @NotNull ClassOrNamespaceDescriptor ownerDescriptor,
            boolean record
    ) {
        if (!DescriptorResolverUtils.isCorrectOwnerForEnumMember(ownerDescriptor, method)) {
            return null;
        }

        JavaType returnJavaType = method.getReturnType();
        if (returnJavaType == null) {
            // This means that the method is a constructor
            return null;
        }

        SimpleFunctionDescriptor alreadyResolved = cache.getMethod(method);
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        SimpleFunctionDescriptorImpl functionDescriptorImpl = new JavaMethodDescriptor(
                ownerDescriptor,
                annotationResolver.resolveAnnotations(method),
                method.getName()
        );

        JavaTypeParameterResolver.Initializer typeParameterInitializer = typeParameterResolver.resolveTypeParameters(functionDescriptorImpl, method);
        typeParameterInitializer.initialize();
        List<TypeParameterDescriptor> methodTypeParameters = typeParameterInitializer.getDescriptors();

        TypeVariableResolver typeVariableResolver = new TypeVariableResolver(methodTypeParameters, functionDescriptorImpl);

        List<ValueParameterDescriptor> valueParameters =
                valueParameterResolver.resolveValueParameters(functionDescriptorImpl, method, typeVariableResolver);
        JetType returnType = makeReturnType(returnJavaType, method, typeVariableResolver);


        List<String> signatureErrors;
        List<FunctionDescriptor> superFunctions;
        ExternalSignatureResolver.AlternativeMethodSignature effectiveSignature;

        if (ownerDescriptor instanceof NamespaceDescriptor) {
            superFunctions = Collections.emptyList();
            effectiveSignature = externalSignatureResolver
                    .resolveAlternativeMethodSignature(method, false, returnType, null, valueParameters, methodTypeParameters);
            signatureErrors = effectiveSignature.getErrors();
        }
        else if (ownerDescriptor instanceof ClassDescriptor) {
            ExternalSignatureResolver.PropagatedMethodSignature propagated = externalSignatureResolver
                    .resolvePropagatedSignature(method, (ClassDescriptor) ownerDescriptor, returnType, null, valueParameters,
                                                methodTypeParameters);

            superFunctions = propagated.getSuperMethods();

            effectiveSignature = externalSignatureResolver
                    .resolveAlternativeMethodSignature(method, !superFunctions.isEmpty(), propagated.getReturnType(),
                                                       propagated.getReceiverType(), propagated.getValueParameters(),
                                                       propagated.getTypeParameters());

            signatureErrors = new ArrayList<String>(propagated.getErrors());
            signatureErrors.addAll(effectiveSignature.getErrors());
        }
        else {
            throw new IllegalStateException("Unknown class or namespace descriptor: " + ownerDescriptor);
        }

        functionDescriptorImpl.initialize(
                effectiveSignature.getReceiverType(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(ownerDescriptor),
                effectiveSignature.getTypeParameters(),
                effectiveSignature.getValueParameters(),
                effectiveSignature.getReturnType(),
                Modality.convertFromFlags(method.isAbstract(), !method.isFinal()),
                method.getVisibility(),
                /*isInline = */ false
        );

        if (record) {
            cache.recordMethod(method, functionDescriptorImpl);
        }

        if (!RawTypesCheck.hasRawTypesInHierarchicalSignature(method)
            && JavaMethodSignatureUtil.isMethodReturnTypeCompatible(method)
            && !containsErrorType(superFunctions, functionDescriptorImpl)) {
            if (signatureErrors.isEmpty()) {
                checkFunctionsOverrideCorrectly(method, superFunctions, functionDescriptorImpl);
            }
            else if (record) {
                externalSignatureResolver.reportSignatureErrors(functionDescriptorImpl, signatureErrors);
            }
        }

        return functionDescriptorImpl;
    }

    private static void checkFunctionsOverrideCorrectly(
            @NotNull JavaMethod method,
            @NotNull List<FunctionDescriptor> superFunctions,
            @NotNull FunctionDescriptor functionDescriptor
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
                          + "sub method = " + PsiFormatUtil.getExternalName(method.getPsi()) + "\n"
                          + "@KotlinSignature = " + SignaturesUtil.getKotlinSignature(method));
            }
        }
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForClass(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor owner) {
        Name methodName = members.getName();

        Set<SimpleFunctionDescriptor> functionsFromCurrent = Sets.newHashSet();
        for (JavaMethod method : members.getMethods()) {
            SimpleFunctionDescriptor function = resolveMethodToFunctionDescriptor(method, owner, true);
            if (function != null) {
                functionsFromCurrent.add(function);
                ContainerUtil.addIfNotNull(functionsFromCurrent, resolveSamAdapter(function));
            }
        }

        if (owner instanceof NamespaceDescriptor) {
            ContainerUtil.addIfNotNull(functionsFromCurrent, resolveSamConstructor((NamespaceDescriptor) owner, members));
        }

        Set<FunctionDescriptor> functions = new HashSet<FunctionDescriptor>();
        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            Collection<SimpleFunctionDescriptor> functionsFromSupertypes = getFunctionsFromSupertypes(methodName, classDescriptor);

            functions.addAll(resolveOverrides(methodName, functionsFromSupertypes, functionsFromCurrent, classDescriptor, trace));
        }

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
    private SamConstructorDescriptor resolveSamConstructor(@NotNull NamespaceDescriptor owner, @NotNull NamedMembers namedMembers) {
        if (namedMembers.getSamInterface() != null) {
            ClassDescriptorFromJvmBytecode klass = findClassInNamespace(owner, namedMembers.getName());
            if (klass != null) {
                SamConstructorDescriptor constructor = createSamConstructorFunction(owner, klass);
                cache.recordSourceDescriptorForSynthesized(constructor, klass);
                return constructor;
            }
        }
        return null;
    }

    @Nullable
    private SimpleFunctionDescriptor resolveSamAdapter(@NotNull SimpleFunctionDescriptor original) {
        if (!isSamAdapterNecessary(original)) return null;

        SamAdapterDescriptor<SimpleFunctionDescriptor> adapter = createSamAdapterFunction(original);
        cache.recordSourceDescriptorForSynthesized(adapter, original);
        return (SimpleFunctionDescriptor) adapter;
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForPackage(@NotNull NamedMembers members, @NotNull NamespaceDescriptor owner) {
        SamConstructorDescriptor samConstructor = resolveSamConstructor(owner, members);
        if (samConstructor != null) {
            return Collections.<FunctionDescriptor>singleton(samConstructor);
        }
        return Collections.emptySet();
    }

    @NotNull
    private JetType makeReturnType(
            @NotNull JavaType returnType,
            @NotNull JavaMethod method,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        TypeUsage typeUsage = JavaAnnotationResolver.hasReadonlyAnnotation(method) && !JavaAnnotationResolver.hasMutableAnnotation(method)
                              ? TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
                              : TypeUsage.MEMBER_SIGNATURE_COVARIANT;
        JetType transformedType = typeTransformer.transformToType(returnType, typeUsage, typeVariableResolver);

        if (JavaAnnotationResolver.hasNotNullAnnotation(method)) {
            return TypeUtils.makeNotNullable(transformedType);
        }
        else {
            return transformedType;
        }
    }

    @NotNull
    private static Set<SimpleFunctionDescriptor> getFunctionsFromSupertypes(@NotNull Name name, @NotNull ClassDescriptor descriptor) {
        Set<SimpleFunctionDescriptor> result = Sets.newLinkedHashSet();
        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            for (FunctionDescriptor function : supertype.getMemberScope().getFunctions(name)) {
                result.add((SimpleFunctionDescriptor) function);
            }
        }
        return result;
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
}
