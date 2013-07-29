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
import com.intellij.psi.*;
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
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
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
        PsiClass psiClass = javaClass.getPsiClass();

        Collection<ConstructorDescriptor> constructors = Lists.newArrayList();

        List<TypeParameterDescriptor> typeParameters = containingClass.getTypeConstructor().getParameters();

        TypeVariableResolver typeVariableResolver =
                new TypeVariableResolver(typeParameters, containingClass, "class " + psiClass.getQualifiedName());

        PsiMethod[] psiConstructors = psiClass.getConstructors();

        boolean isStatic = psiClass.hasModifierProperty(PsiModifier.STATIC);
        if (containingClass.getKind() == ClassKind.OBJECT || containingClass.getKind() == ClassKind.CLASS_OBJECT) {
            constructors.add(DescriptorResolver.createPrimaryConstructorForObject(containingClass));
        }
        else if (psiConstructors.length == 0) {
            if (trace.get(BindingContext.CONSTRUCTOR, psiClass) != null) {
                constructors.add(trace.get(BindingContext.CONSTRUCTOR, psiClass));
            }
            else {
                Visibility constructorVisibility = getConstructorVisibility(containingClass);
                // We need to create default constructors for classes and abstract classes.
                // Example:
                // class Kotlin() : Java() {}
                // abstract public class Java {}
                if (!psiClass.isInterface()) {
                    ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                            containingClass,
                            Collections.<AnnotationDescriptor>emptyList(),
                            true);
                    constructorDescriptor
                            .initialize(typeParameters, Collections.<ValueParameterDescriptor>emptyList(), constructorVisibility, isStatic);
                    constructors.add(constructorDescriptor);
                    trace.record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
                }
                if (psiClass.isAnnotationType()) {
                    // A constructor for an annotation type takes all the "methods" in the @interface as parameters
                    ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                            containingClass,
                            Collections.<AnnotationDescriptor>emptyList(),
                            true);

                    List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();
                    PsiMethod[] methods = psiClass.getMethods();
                    for (int i = 0; i < methods.length; i++) {
                        PsiMethod method = methods[i];
                        if (method instanceof PsiAnnotationMethod) {
                            PsiAnnotationMethod annotationMethod = (PsiAnnotationMethod) method;
                            assert annotationMethod.getParameterList().getParameters().length == 0;

                            PsiType returnType = annotationMethod.getReturnType();

                            // We take the following heuristical convention:
                            // if the last method of the @interface is an array, we convert it into a vararg
                            JetType varargElementType = null;
                            if (i == methods.length - 1 && (returnType instanceof PsiArrayType)) {
                                varargElementType = typeTransformer
                                        .transformToType(((PsiArrayType) returnType).getComponentType(), typeVariableResolver);
                            }

                            assert returnType != null;
                            valueParameters.add(new ValueParameterDescriptorImpl(
                                    constructorDescriptor,
                                    i,
                                    Collections.<AnnotationDescriptor>emptyList(),
                                    Name.identifier(method.getName()),
                                    typeTransformer.transformToType(returnType, typeVariableResolver),
                                    annotationMethod.getDefaultValue() != null,
                                    varargElementType));
                        }
                    }

                    constructorDescriptor.initialize(typeParameters, valueParameters, constructorVisibility, isStatic);
                    constructors.add(constructorDescriptor);
                    trace.record(BindingContext.CONSTRUCTOR, psiClass, constructorDescriptor);
                }
            }
        }
        else {
            for (PsiMethod psiConstructor : psiConstructors) {
                ConstructorDescriptor constructor = resolveConstructor(psiClass, isStatic, psiConstructor, containingClass);
                if (constructor != null) {
                    constructors.add(constructor);
                    ContainerUtil.addIfNotNull(constructors, resolveSamAdapter(constructor));
                }
            }
        }

        for (ConstructorDescriptor constructor : constructors) {
            ((ConstructorDescriptorImpl) constructor).setReturnType(containingClass.getDefaultType());
        }

        return constructors;
    }

    @NotNull
    private static Visibility getConstructorVisibility(@NotNull ClassDescriptor classDescriptor) {
        Visibility visibility = classDescriptor.getVisibility();
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        return visibility;
    }

    @Nullable
    private ConstructorDescriptor resolveConstructor(
            PsiClass psiClass,
            boolean aStatic,
            PsiMethod psiConstructor,
            ClassDescriptor classDescriptor
    ) {
        PsiMethodWrapper constructor = new PsiMethodWrapper(psiConstructor);

        if (trace.get(BindingContext.CONSTRUCTOR, psiConstructor) != null) {
            return trace.get(BindingContext.CONSTRUCTOR, psiConstructor);
        }

        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false);

        List<TypeParameterDescriptor> typeParameters = classDescriptor.getTypeConstructor().getParameters();

        JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors = valueParameterResolver.resolveParameterDescriptors(
                constructorDescriptor, constructor.getParameters(),
                new TypeVariableResolver(typeParameters, classDescriptor, "constructor of class " + psiClass.getQualifiedName())
        );

        if (valueParameterDescriptors.getReceiverType() != null) {
            throw new IllegalStateException();
        }

        AlternativeMethodSignatureData alternativeMethodSignatureData =
                new AlternativeMethodSignatureData(constructor, valueParameterDescriptors, null,
                                                   Collections.<TypeParameterDescriptor>emptyList(), false);
        if (alternativeMethodSignatureData.isAnnotated() && !alternativeMethodSignatureData.hasErrors()) {
            valueParameterDescriptors = alternativeMethodSignatureData.getValueParameters();
        }
        else if (alternativeMethodSignatureData.hasErrors()) {
            trace.record(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, constructorDescriptor,
                         Collections.singletonList(alternativeMethodSignatureData.getError()));
        }

        constructorDescriptor.initialize(typeParameters,
                                         valueParameterDescriptors.getDescriptors(),
                                         DescriptorResolverUtils.resolveVisibility(psiConstructor),
                                         aStatic);
        trace.record(BindingContext.CONSTRUCTOR, psiConstructor, constructorDescriptor);
        return constructorDescriptor;
    }

    @Nullable
    private ConstructorDescriptor resolveSamAdapter(@NotNull ConstructorDescriptor original) {
        return isSamAdapterNecessary(original)
               ? recordSamAdapter(original, createSamAdapterConstructor(original), trace)
               : null;
    }
}