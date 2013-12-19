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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaMethodDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor;
import org.jetbrains.jet.lang.resolve.java.scope.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.resolveOverrides;
import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.*;

public final class JavaFunctionResolver {
    private JavaTypeTransformer typeTransformer;
    private JavaResolverCache cache;
    private JavaTypeParameterResolver typeParameterResolver;
    private JavaValueParameterResolver valueParameterResolver;
    private JavaAnnotationResolver annotationResolver;
    private ExternalSignatureResolver externalSignatureResolver;
    private ErrorReporter errorReporter;
    private MethodSignatureChecker signatureChecker;

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
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

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Inject
    public void setSignatureChecker(MethodSignatureChecker signatureChecker) {
        this.signatureChecker = signatureChecker;
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
        if (!DescriptorResolverUtils.isCorrectOwnerForEnumMethod(ownerDescriptor, method)) {
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

        TypeVariableResolver typeVariableResolver = new TypeVariableResolverImpl(methodTypeParameters, functionDescriptorImpl);

        List<ValueParameterDescriptor> valueParameters =
                valueParameterResolver.resolveValueParameters(functionDescriptorImpl, method, typeVariableResolver);
        JetType returnType = makeReturnType(returnJavaType, method, typeVariableResolver);


        List<String> signatureErrors;
        List<FunctionDescriptor> superFunctions;
        ExternalSignatureResolver.AlternativeMethodSignature effectiveSignature;

        if (ownerDescriptor instanceof PackageFragmentDescriptor) {
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
                method.getVisibility()
                /*TODO what if user annotate it with inline?*/
        );

        if (record) {
            cache.recordMethod(method, functionDescriptorImpl);
        }

        signatureChecker.checkSignature(method, record, functionDescriptorImpl, signatureErrors, superFunctions);

        return functionDescriptorImpl;
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroupForClass(@NotNull NamedMembers members, @NotNull ClassOrNamespaceDescriptor owner) {
        Name methodName = members.getName();

        Set<SimpleFunctionDescriptor> functionsFromCurrent = new HashSet<SimpleFunctionDescriptor>();
        for (JavaMethod method : members.getMethods()) {
            SimpleFunctionDescriptor function = resolveMethodToFunctionDescriptor(method, owner, true);
            if (function != null) {
                functionsFromCurrent.add(function);
                SimpleFunctionDescriptor samAdapter = resolveSamAdapter(function);
                if (samAdapter != null) {
                    functionsFromCurrent.add(samAdapter);
                }
            }
        }

        if (owner instanceof JavaPackageFragmentDescriptor) {
            SamConstructorDescriptor samConstructor = resolveSamConstructor((JavaPackageFragmentDescriptor) owner, members);
            if (samConstructor != null) {
                functionsFromCurrent.add(samConstructor);
            }
        }

        Set<FunctionDescriptor> functions = new HashSet<FunctionDescriptor>();
        if (owner instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) owner;

            Collection<SimpleFunctionDescriptor> functionsFromSupertypes = getFunctionsFromSupertypes(methodName, classDescriptor);

            functions.addAll(resolveOverrides(methodName, functionsFromSupertypes, functionsFromCurrent, classDescriptor, errorReporter));
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
    public static SamConstructorDescriptor resolveSamConstructor(@NotNull JavaPackageFragmentDescriptor owner, @NotNull NamedMembers namedMembers) {
        if (namedMembers.getSamInterface() != null) {
            JavaClassDescriptor klass = DescriptorResolverUtils.findClassInPackage(owner, namedMembers.getName());
            if (klass != null) {
                return createSamConstructorFunction(owner, klass);
            }
        }
        return null;
    }

    @Nullable
    private static SimpleFunctionDescriptor resolveSamAdapter(@NotNull SimpleFunctionDescriptor original) {
        return isSamAdapterNecessary(original) ? (SimpleFunctionDescriptor) createSamAdapterFunction(original) : null;
    }

    @NotNull
    private JetType makeReturnType(
            @NotNull JavaType returnType,
            @NotNull JavaMethod method,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        TypeUsage typeUsage = annotationResolver.hasReadonlyAnnotation(method) && !annotationResolver.hasMutableAnnotation(method)
                              ? TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
                              : TypeUsage.MEMBER_SIGNATURE_COVARIANT;
        JetType transformedType = typeTransformer.transformToType(returnType, typeUsage, typeVariableResolver);

        if (annotationResolver.hasNotNullAnnotation(method)) {
            return TypeUtils.makeNotNullable(transformedType);
        }
        else {
            return transformedType;
        }
    }

    @NotNull
    private static Set<SimpleFunctionDescriptor> getFunctionsFromSupertypes(@NotNull Name name, @NotNull ClassDescriptor descriptor) {
        Set<SimpleFunctionDescriptor> result = new LinkedHashSet<SimpleFunctionDescriptor>();
        for (JetType supertype : descriptor.getTypeConstructor().getSupertypes()) {
            for (FunctionDescriptor function : supertype.getMemberScope().getFunctions(name)) {
                result.add((SimpleFunctionDescriptor) function);
            }
        }
        return result;
    }
}
