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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterListOwner;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class JavaDescriptorSignatureResolver {

    @NotNull
    private JavaDescriptorResolver javaDescriptorResolver;
    @NotNull
    private JavaSemanticServices semanticServices;

    @Inject
    public void setJavaDescriptorResolver(@NotNull JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
    }

    @Inject
    public void setJavaSemanticServices(@NotNull JavaSemanticServices javaSemanticServices) {
        this.semanticServices = javaSemanticServices;
    }



    private enum TypeParameterDescriptorOrigin {
        JAVA,
        KOTLIN,
    }

    public static class TypeParameterDescriptorInitialization {
        @NotNull
        private final TypeParameterDescriptorOrigin origin;
        @NotNull
        final TypeParameterDescriptorImpl descriptor;
        final PsiTypeParameter psiTypeParameter;
        @Nullable
        private final List<JetType> upperBoundsForKotlin;
        @Nullable
        private final List<JetType> lowerBoundsForKotlin;

        private TypeParameterDescriptorInitialization(@NotNull TypeParameterDescriptorImpl descriptor, @NotNull PsiTypeParameter psiTypeParameter) {
            this.origin = TypeParameterDescriptorOrigin.JAVA;
            this.descriptor = descriptor;
            this.psiTypeParameter = psiTypeParameter;
            this.upperBoundsForKotlin = null;
            this.lowerBoundsForKotlin = null;
        }

        private TypeParameterDescriptorInitialization(@NotNull TypeParameterDescriptorImpl descriptor, @NotNull PsiTypeParameter psiTypeParameter,
                List<JetType> upperBoundsForKotlin, List<JetType> lowerBoundsForKotlin) {
            this.origin = TypeParameterDescriptorOrigin.KOTLIN;
            this.descriptor = descriptor;
            this.psiTypeParameter = psiTypeParameter;
            this.upperBoundsForKotlin = upperBoundsForKotlin;
            this.lowerBoundsForKotlin = lowerBoundsForKotlin;
        }
    }



    @NotNull
    private static PsiTypeParameter getPsiTypeParameterByName(PsiTypeParameterListOwner clazz, String name) {
        for (PsiTypeParameter typeParameter : clazz.getTypeParameters()) {
            if (typeParameter.getName().equals(name)) {
                return typeParameter;
            }
        }
        throw new IllegalStateException("PsiTypeParameter '" + name + "' is not found");
    }



    private abstract class JetSignatureTypeParameterVisitor extends JetSignatureExceptionsAdapter {

        @NotNull
        private final PsiTypeParameterListOwner psiOwner;
        @NotNull
        private final String name;
        @NotNull
        private final TypeVariableResolver typeVariableResolver;
        @NotNull
        private final TypeParameterDescriptorImpl typeParameterDescriptor;

        protected JetSignatureTypeParameterVisitor(PsiTypeParameterListOwner psiOwner,
                String name, TypeVariableResolver typeVariableResolver, TypeParameterDescriptorImpl typeParameterDescriptor)
        {
            if (name.isEmpty()) {
                throw new IllegalStateException();
            }

            this.psiOwner = psiOwner;
            this.name = name;
            this.typeVariableResolver = typeVariableResolver;
            this.typeParameterDescriptor = typeParameterDescriptor;
        }

        List<JetType> upperBounds = new ArrayList<JetType>();
        List<JetType> lowerBounds = new ArrayList<JetType>();

        @Override
        public JetSignatureVisitor visitClassBound() {
            return new JetTypeJetSignatureReader(semanticServices, JetStandardLibrary.getInstance(), typeVariableResolver) {
                @Override
                protected void done(@NotNull JetType jetType) {
                    if (JavaDescriptorResolver.isJavaLangObject(jetType)) {
                        return;
                    }
                    upperBounds.add(jetType);
                }
            };
        }

        @Override
        public JetSignatureVisitor visitInterfaceBound() {
            return new JetTypeJetSignatureReader(semanticServices, JetStandardLibrary.getInstance(), typeVariableResolver) {
                @Override
                protected void done(@NotNull JetType jetType) {
                    upperBounds.add(jetType);
                }
            };
        }

        @Override
        public void visitFormalTypeParameterEnd() {
            PsiTypeParameter psiTypeParameter = getPsiTypeParameterByName(psiOwner, name);
            TypeParameterDescriptorInitialization typeParameterDescriptorInitialization = new TypeParameterDescriptorInitialization(typeParameterDescriptor, psiTypeParameter, upperBounds, lowerBounds);
            done(typeParameterDescriptorInitialization);
        }

        protected abstract void done(@NotNull TypeParameterDescriptorInitialization typeParameterDescriptor);
    }

    private class JetSignatureTypeParametersVisitor extends JetSignatureExceptionsAdapter {
        @NotNull
        private final DeclarationDescriptor containingDeclaration;
        @NotNull
        private final PsiTypeParameterListOwner psiOwner;

        private final List<TypeParameterDescriptor> previousTypeParameters = new ArrayList<TypeParameterDescriptor>();
        // note changes state in this method
        private final TypeVariableResolver typeVariableResolver;


        private JetSignatureTypeParametersVisitor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameterListOwner psiOwner, @NotNull String context) {
            this.containingDeclaration = containingDeclaration;
            this.psiOwner = psiOwner;

            this.typeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(
                    previousTypeParameters,
                    containingDeclaration,
                    context);
        }

        private int formalTypeParameterIndex = 0;


        List<TypeParameterDescriptorInitialization> r = new ArrayList<TypeParameterDescriptorInitialization>();

        @Override
        public JetSignatureVisitor visitFormalTypeParameter(final String name, final TypeInfoVariance variance, boolean reified) {
            TypeParameterDescriptorImpl typeParameter = TypeParameterDescriptorImpl.createForFurtherModification(
                    containingDeclaration,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO: wrong
                    reified,
                    JetSignatureUtils.translateVariance(variance),
                    Name.identifier(name),
                    formalTypeParameterIndex++);

            previousTypeParameters.add(typeParameter);

            return new JetSignatureTypeParameterVisitor(psiOwner, name, typeVariableResolver, typeParameter) {
                @Override
                protected void done(@NotNull TypeParameterDescriptorInitialization typeParameterDescriptor) {
                    r.add(typeParameterDescriptor);
                    previousTypeParameters.add(typeParameterDescriptor.descriptor);
                }
            };
        }
    }

        /**
     * @see #resolveMethodTypeParametersFromJetSignature(String, com.intellij.psi.PsiMethod, org.jetbrains.jet.lang.descriptors.DeclarationDescriptor)
     */
    private List<TypeParameterDescriptorInitialization> resolveClassTypeParametersFromJetSignature(String jetSignature,
            final PsiClass clazz, final ClassDescriptor classDescriptor) {
        String context = "class " + clazz.getQualifiedName();
        JetSignatureTypeParametersVisitor jetSignatureTypeParametersVisitor = new JetSignatureTypeParametersVisitor(classDescriptor, clazz, context) {
            @Override
            public JetSignatureVisitor visitSuperclass() {
                // TODO
                return new JetSignatureAdapter();
            }

            @Override
            public JetSignatureVisitor visitInterface() {
                // TODO
                return new JetSignatureAdapter();
            }
        };
        new JetSignatureReader(jetSignature).accept(jetSignatureTypeParametersVisitor);
        return jetSignatureTypeParametersVisitor.r;
    }


        private List<TypeParameterDescriptorInitialization> makeUninitializedTypeParameters(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter[] typeParameters) {
        List<TypeParameterDescriptorInitialization> result = Lists.newArrayList();
        for (PsiTypeParameter typeParameter : typeParameters) {
            TypeParameterDescriptorInitialization typeParameterDescriptor = makeUninitializedTypeParameter(containingDeclaration, typeParameter);
            result.add(typeParameterDescriptor);
        }
        return result;
    }

    @NotNull
    private TypeParameterDescriptorInitialization makeUninitializedTypeParameter(@NotNull DeclarationDescriptor containingDeclaration, @NotNull PsiTypeParameter psiTypeParameter) {
        TypeParameterDescriptorImpl typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDeclaration,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                false,
                Variance.INVARIANT,
                Name.identifier(psiTypeParameter.getName()),
                psiTypeParameter.getIndex()
        );
        return new TypeParameterDescriptorInitialization(typeParameterDescriptor, psiTypeParameter);
    }

    private void initializeTypeParameter(TypeParameterDescriptorInitialization typeParameter, TypeVariableResolver typeVariableByPsiResolver) {
        TypeParameterDescriptorImpl typeParameterDescriptor = typeParameter.descriptor;
        if (typeParameter.origin == TypeParameterDescriptorOrigin.KOTLIN) {
            List<?> upperBounds = typeParameter.upperBoundsForKotlin;
            if (upperBounds.size() == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            }
            else {
                for (JetType upperBound : typeParameter.upperBoundsForKotlin) {
                    typeParameterDescriptor.addUpperBound(upperBound);
                }
            }

            // TODO: lower bounds
        }
        else {
            PsiClassType[] referencedTypes = typeParameter.psiTypeParameter.getExtendsList().getReferencedTypes();
            if (referencedTypes.length == 0){
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getNullableAnyType());
            }
            else if (referencedTypes.length == 1) {
                typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedTypes[0], JavaTypeTransformer.TypeUsage.UPPER_BOUND, typeVariableByPsiResolver));
            }
            else {
                for (PsiClassType referencedType : referencedTypes) {
                    typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer().transformToType(referencedType, JavaTypeTransformer.TypeUsage.UPPER_BOUND, typeVariableByPsiResolver));
                }
            }
        }
        typeParameterDescriptor.setInitialized();
    }

    void initializeTypeParameters(List<TypeParameterDescriptorInitialization> typeParametersInitialization, @NotNull DeclarationDescriptor typeParametersOwner, @NotNull String context) {
        List<TypeParameterDescriptor> prevTypeParameters = new ArrayList<TypeParameterDescriptor>();

        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        for (TypeParameterDescriptorInitialization typeParameterDescriptor : typeParametersInitialization) {
            typeParameters.add(typeParameterDescriptor.descriptor);
        }

        for (TypeParameterDescriptorInitialization psiTypeParameter : typeParametersInitialization) {
            prevTypeParameters.add(psiTypeParameter.descriptor);
            initializeTypeParameter(psiTypeParameter,
                    TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, typeParametersOwner, context));
        }
    }


    List<TypeParameterDescriptorInitialization> createUninitializedClassTypeParameters(PsiClass psiClass, JavaDescriptorResolver.ResolverBinaryClassData classData) {
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);

        if (jetClassAnnotation.signature().length() > 0) {
            return resolveClassTypeParametersFromJetSignature(
                    jetClassAnnotation.signature(), psiClass, classData.classDescriptor);
        }

        return makeUninitializedTypeParameters(classData.classDescriptor, psiClass.getTypeParameters());
    }


    List<TypeParameterDescriptor> resolveMethodTypeParameters(
            @NotNull PsiMethodWrapper method,
            @NotNull DeclarationDescriptor functionDescriptor) {

        List<TypeParameterDescriptorInitialization> typeParametersIntialization;
        if (method.getJetMethod().typeParameters().length() > 0) {
            typeParametersIntialization = resolveMethodTypeParametersFromJetSignature(
                    method.getJetMethod().typeParameters(), method.getPsiMethod(), functionDescriptor);
        }
        else {
            typeParametersIntialization = makeUninitializedTypeParameters(functionDescriptor, method.getPsiMethod().getTypeParameters());
        }

        String context = "method " + method.getName() + " in class " + method.getPsiMethod().getContainingClass().getQualifiedName();
        initializeTypeParameters(typeParametersIntialization, functionDescriptor, context);

        List<TypeParameterDescriptor> typeParameters = Lists.newArrayListWithCapacity(typeParametersIntialization.size());

        for (TypeParameterDescriptorInitialization tpdi : typeParametersIntialization) {
            typeParameters.add(tpdi.descriptor);
        }

        return typeParameters;
    }

    /**
     * @see #resolveClassTypeParametersFromJetSignature(String, com.intellij.psi.PsiClass, org.jetbrains.jet.lang.descriptors.ClassDescriptor)
     */
    private List<TypeParameterDescriptorInitialization> resolveMethodTypeParametersFromJetSignature(String jetSignature,
            final PsiMethod method, final DeclarationDescriptor functionDescriptor)
    {
        String context = "method " + method.getName() + " in class " + method.getContainingClass().getQualifiedName();
        JetSignatureTypeParametersVisitor jetSignatureTypeParametersVisitor = new JetSignatureTypeParametersVisitor(functionDescriptor, method, context);
        new JetSignatureReader(jetSignature).acceptFormalTypeParametersOnly(jetSignatureTypeParametersVisitor);
        return jetSignatureTypeParametersVisitor.r;
    }




}
