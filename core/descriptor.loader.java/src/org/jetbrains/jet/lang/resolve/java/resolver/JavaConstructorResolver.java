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
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.createSamAdapterConstructor;
import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.isSamAdapterNecessary;

public final class JavaConstructorResolver {
    private JavaResolverCache cache;
    private JavaTypeTransformer typeTransformer;
    private JavaValueParameterResolver valueParameterResolver;
    private ExternalSignatureResolver externalSignatureResolver;

    public JavaConstructorResolver() {
    }

    @Inject
    public void setCache(JavaResolverCache cache) {
        this.cache = cache;
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setValueParameterResolver(JavaValueParameterResolver valueParameterResolver) {
        this.valueParameterResolver = valueParameterResolver;
    }

    @Inject
    public void setExternalSignatureResolver(ExternalSignatureResolver externalSignatureResolver) {
        this.externalSignatureResolver = externalSignatureResolver;
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(@NotNull JavaClass javaClass, @NotNull ClassDescriptor containingClass) {
        Collection<ConstructorDescriptor> result = new ArrayList<ConstructorDescriptor>();

        Collection<JavaMethod> constructors = javaClass.getConstructors();

        if (constructors.isEmpty()) {
            ConstructorDescriptor defaultConstructor = resolveDefaultConstructor(javaClass, containingClass);
            if (defaultConstructor != null) {
                result.add(defaultConstructor);
            }
        }
        else {
            for (JavaMethod constructor : constructors) {
                ConstructorDescriptor descriptor = resolveConstructor(constructor, containingClass, javaClass.isStatic());
                result.add(descriptor);
                ConstructorDescriptor samAdapter = resolveSamAdapter(descriptor);
                if (samAdapter != null) {
                    result.add(samAdapter);
                }
            }
        }

        for (ConstructorDescriptor constructor : result) {
            ((ConstructorDescriptorImpl) constructor).setReturnType(containingClass.getDefaultType());
        }

        return result;
    }

    @Nullable
    private ConstructorDescriptor resolveDefaultConstructor(@NotNull JavaClass javaClass, @NotNull ClassDescriptor containingClass) {
        ConstructorDescriptor alreadyResolved = cache.getConstructor(javaClass);
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        boolean isAnnotation = javaClass.isAnnotationType();

        if (javaClass.isInterface() && !isAnnotation) return null;

        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                containingClass,
                Annotations.EMPTY,
                true);

        List<TypeParameterDescriptor> typeParameters = containingClass.getTypeConstructor().getParameters();

        List<ValueParameterDescriptor> valueParameters;
        if (isAnnotation) {
            TypeVariableResolver typeVariableResolver = new TypeVariableResolverImpl(typeParameters, containingClass);
            valueParameters = resolveAnnotationParameters(javaClass, constructorDescriptor, typeVariableResolver);
        }
        else {
            valueParameters = Collections.emptyList();
        }

        constructorDescriptor.initialize(typeParameters, valueParameters, getConstructorVisibility(containingClass), javaClass.isStatic());

        cache.recordConstructor(javaClass, constructorDescriptor);

        return constructorDescriptor;
    }

    @NotNull
    private List<ValueParameterDescriptor> resolveAnnotationParameters(
            @NotNull JavaClass javaClass,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        // A constructor for an annotation type takes all the "methods" in the @interface as parameters
        Collection<JavaMethod> methods = javaClass.getMethods();
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(methods.size());

        int index = 0;
        for (Iterator<JavaMethod> iterator = methods.iterator(); iterator.hasNext(); ) {
            JavaMethod method = iterator.next();
            assert method.getValueParameters().isEmpty() : "Annotation method can't have parameters: " + method;

            JavaType returnType = method.getReturnType();
            assert returnType != null : "Annotation method has no return type: " + method;

            // We take the following heuristic convention:
            // if the last method of the @interface is an array, we convert it into a vararg
            JetType varargElementType = null;
            if (!iterator.hasNext() && returnType instanceof JavaArrayType) {
                JavaType componentType = ((JavaArrayType) returnType).getComponentType();
                varargElementType = typeTransformer.transformToType(componentType, typeVariableResolver);
            }

            result.add(new ValueParameterDescriptorImpl(
                    constructorDescriptor,
                    index,
                    Annotations.EMPTY,
                    method.getName(),
                    // Parameters of annotation constructors in Java are never nullable
                    TypeUtils.makeNotNullable(typeTransformer.transformToType(returnType, typeVariableResolver)),
                    method.hasAnnotationParameterDefaultValue(),
                    varargElementType == null ? null : TypeUtils.makeNotNullable(varargElementType)));

            index++;
        }

        return result;
    }

    @NotNull
    private ConstructorDescriptor resolveConstructor(
            @NotNull JavaMethod constructor,
            @NotNull ClassDescriptor classDescriptor,
            boolean isStaticClass
    ) {
        ConstructorDescriptor alreadyResolved = cache.getConstructor(constructor);
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                Annotations.EMPTY, // TODO
                false);

        List<TypeParameterDescriptor> typeParameters = classDescriptor.getTypeConstructor().getParameters();

        List<ValueParameterDescriptor> valueParameters = valueParameterResolver.resolveValueParameters(
                constructorDescriptor, constructor,
                new TypeVariableResolverImpl(typeParameters, classDescriptor)
        );

        ExternalSignatureResolver.AlternativeMethodSignature effectiveSignature = externalSignatureResolver
                .resolveAlternativeMethodSignature(constructor, false, null, null, valueParameters,
                                                   Collections.<TypeParameterDescriptor>emptyList());

        constructorDescriptor
                .initialize(typeParameters, effectiveSignature.getValueParameters(), constructor.getVisibility(), isStaticClass);

        List<String> signatureErrors = effectiveSignature.getErrors();
        if (!signatureErrors.isEmpty()) {
            externalSignatureResolver.reportSignatureErrors(constructorDescriptor, signatureErrors);
        }

        cache.recordConstructor(constructor, constructorDescriptor);
        return constructorDescriptor;
    }
}
