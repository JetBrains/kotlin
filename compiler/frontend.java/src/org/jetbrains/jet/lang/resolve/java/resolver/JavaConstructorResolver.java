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
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.resolver.JavaFunctionResolver.recordSamAdapter;
import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.createSamAdapterConstructor;
import static org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils.isSamAdapterNecessary;

public final class JavaConstructorResolver {
    private BindingTrace trace;
    private JavaTypeTransformer typeTransformer;
    private JavaValueParameterResolver valueParameterResolver;

    public JavaConstructorResolver() {
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setValueParameterResolver(JavaValueParameterResolver valueParameterResolver) {
        this.valueParameterResolver = valueParameterResolver;
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(@NotNull JavaClass javaClass, @NotNull ClassDescriptor containingClass) {
        Collection<ConstructorDescriptor> result = Lists.newArrayList();

        Collection<JavaMethod> constructors = javaClass.getConstructors();

        if (containingClass.getKind() == ClassKind.OBJECT || containingClass.getKind() == ClassKind.CLASS_OBJECT) {
            result.add(DescriptorResolver.createPrimaryConstructorForObject(containingClass));
        }
        else if (constructors.isEmpty()) {
            ContainerUtil.addIfNotNull(result, resolveDefaultConstructor(javaClass, containingClass));
        }
        else {
            for (JavaMethod constructor : constructors) {
                ConstructorDescriptor descriptor = resolveConstructor(constructor, containingClass, javaClass.isStatic());
                result.add(descriptor);
                ContainerUtil.addIfNotNull(result, resolveSamAdapter(descriptor));
            }
        }

        for (ConstructorDescriptor constructor : result) {
            ((ConstructorDescriptorImpl) constructor).setReturnType(containingClass.getDefaultType());
        }

        return result;
    }

    @Nullable
    private ConstructorDescriptor resolveDefaultConstructor(@NotNull JavaClass javaClass, @NotNull ClassDescriptor containingClass) {
        ConstructorDescriptor alreadyResolved = trace.get(BindingContext.CONSTRUCTOR, javaClass.getPsi());
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        boolean isAnnotation = javaClass.isAnnotationType();

        if (javaClass.isInterface() && !isAnnotation) return null;

        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                containingClass,
                Collections.<AnnotationDescriptor>emptyList(),
                true);

        List<TypeParameterDescriptor> typeParameters = containingClass.getTypeConstructor().getParameters();

        List<ValueParameterDescriptor> valueParameters;
        if (isAnnotation) {
            TypeVariableResolver typeVariableResolver = new TypeVariableResolver(typeParameters, containingClass);
            valueParameters = resolveAnnotationParameters(javaClass, constructorDescriptor, typeVariableResolver);
        }
        else {
            valueParameters = Collections.emptyList();
        }

        constructorDescriptor.initialize(typeParameters, valueParameters, getConstructorVisibility(containingClass), javaClass.isStatic());

        trace.record(BindingContext.CONSTRUCTOR, javaClass.getPsi(), constructorDescriptor);

        return constructorDescriptor;
    }

    @NotNull
    private List<ValueParameterDescriptor> resolveAnnotationParameters(
            @NotNull JavaClass javaClass,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        // A constructor for an annotation type takes all the "methods" in the @interface as parameters
        List<ValueParameterDescriptor> result = Lists.newArrayList();

        int index = 0;
        for (Iterator<JavaMethod> iterator = javaClass.getMethods().iterator(); iterator.hasNext(); ) {
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
                    Collections.<AnnotationDescriptor>emptyList(),
                    method.getName(),
                    typeTransformer.transformToType(returnType, typeVariableResolver),
                    method.getAnnotationParameterDefaultValue() != null,
                    varargElementType));

            index++;
        }

        return result;
    }

    @NotNull
    private static Visibility getConstructorVisibility(@NotNull ClassDescriptor classDescriptor) {
        Visibility visibility = classDescriptor.getVisibility();
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        return visibility;
    }

    @NotNull
    private ConstructorDescriptor resolveConstructor(
            @NotNull JavaMethod constructor,
            @NotNull ClassDescriptor classDescriptor,
            boolean isStaticClass
    ) {
        ConstructorDescriptor alreadyResolved = trace.get(BindingContext.CONSTRUCTOR, constructor.getPsi());
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false);

        List<TypeParameterDescriptor> typeParameters = classDescriptor.getTypeConstructor().getParameters();

        JavaValueParameterResolver.ValueParameters valueParameters = valueParameterResolver.resolveValueParameters(
                constructorDescriptor, constructor,
                new TypeVariableResolver(typeParameters, classDescriptor)
        );

        if (valueParameters.getReceiverType() != null) {
            throw new IllegalStateException();
        }

        AlternativeMethodSignatureData alternativeMethodSignatureData =
                new AlternativeMethodSignatureData(constructor, valueParameters, null,
                                                   Collections.<TypeParameterDescriptor>emptyList(), false);
        if (alternativeMethodSignatureData.isAnnotated() && !alternativeMethodSignatureData.hasErrors()) {
            valueParameters = alternativeMethodSignatureData.getValueParameters();
        }
        else if (alternativeMethodSignatureData.hasErrors()) {
            trace.record(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, constructorDescriptor,
                         Collections.singletonList(alternativeMethodSignatureData.getError()));
        }

        constructorDescriptor.initialize(typeParameters, valueParameters.getDescriptors(), constructor.getVisibility(), isStaticClass);

        trace.record(BindingContext.CONSTRUCTOR, constructor.getPsi(), constructorDescriptor);
        return constructorDescriptor;
    }

    @Nullable
    private ConstructorDescriptor resolveSamAdapter(@NotNull ConstructorDescriptor original) {
        return isSamAdapterNecessary(original)
               ? (ConstructorDescriptor) recordSamAdapter(original, createSamAdapterConstructor(original), trace)
               : null;
    }
}