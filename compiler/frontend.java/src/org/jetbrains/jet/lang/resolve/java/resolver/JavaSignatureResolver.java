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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.TypeUsage;
import org.jetbrains.jet.lang.resolve.java.TypeVariableResolver;
import org.jetbrains.jet.lang.resolve.java.TypeVariableResolvers;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public final class JavaSignatureResolver {

    @NotNull
    private JavaSemanticServices semanticServices;

    @Inject
    public void setJavaSemanticServices(@NotNull JavaSemanticServices javaSemanticServices) {
        this.semanticServices = javaSemanticServices;
    }

    public static class TypeParameterDescriptorInitialization {
        @NotNull
        private final TypeParameterDescriptorImpl descriptor;
        private final PsiTypeParameter psiTypeParameter;

        private TypeParameterDescriptorInitialization(
                @NotNull TypeParameterDescriptorImpl descriptor,
                @NotNull PsiTypeParameter psiTypeParameter
        ) {
            this.descriptor = descriptor;
            this.psiTypeParameter = psiTypeParameter;
        }

        @NotNull
        public TypeParameterDescriptorImpl getDescriptor() {
            return descriptor;
        }
    }


    private static List<TypeParameterDescriptorInitialization> makeUninitializedTypeParameters(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull PsiTypeParameter[] typeParameters
    ) {
        List<TypeParameterDescriptorInitialization> result = Lists.newArrayList();
        for (PsiTypeParameter typeParameter : typeParameters) {
            TypeParameterDescriptorInitialization typeParameterDescriptor =
                    makeUninitializedTypeParameter(containingDeclaration, typeParameter);
            result.add(typeParameterDescriptor);
        }
        return result;
    }

    @NotNull
    private static TypeParameterDescriptorInitialization makeUninitializedTypeParameter(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull PsiTypeParameter psiTypeParameter
    ) {
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

    private void initializeTypeParameter(
            TypeParameterDescriptorInitialization typeParameter,
            TypeVariableResolver typeVariableByPsiResolver
    ) {
        TypeParameterDescriptorImpl typeParameterDescriptor = typeParameter.descriptor;
        PsiClassType[] referencedTypes = typeParameter.psiTypeParameter.getExtendsList().getReferencedTypes();
        if (referencedTypes.length == 0) {
            typeParameterDescriptor.addUpperBound(KotlinBuiltIns.getInstance().getNullableAnyType());
        }
        else if (referencedTypes.length == 1) {
            typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer()
                                                          .transformToType(referencedTypes[0], TypeUsage.UPPER_BOUND,
                                                                           typeVariableByPsiResolver));
        }
        else {
            for (PsiClassType referencedType : referencedTypes) {
                typeParameterDescriptor.addUpperBound(semanticServices.getTypeTransformer()
                                                              .transformToType(referencedType, TypeUsage.UPPER_BOUND,
                                                                               typeVariableByPsiResolver));
            }
        }
        typeParameterDescriptor.setInitialized();
    }

    public void initializeTypeParameters(
            List<TypeParameterDescriptorInitialization> typeParametersInitialization,
            @NotNull DeclarationDescriptor typeParametersOwner,
            @NotNull String context
    ) {
        List<TypeParameterDescriptor> prevTypeParameters = Lists.newArrayList();

        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        for (TypeParameterDescriptorInitialization typeParameterDescriptor : typeParametersInitialization) {
            typeParameters.add(typeParameterDescriptor.descriptor);
        }

        for (TypeParameterDescriptorInitialization psiTypeParameter : typeParametersInitialization) {
            prevTypeParameters.add(psiTypeParameter.descriptor);
            initializeTypeParameter(psiTypeParameter,
                                    TypeVariableResolvers
                                            .typeVariableResolverFromTypeParameters(typeParameters, typeParametersOwner, context));
        }
    }


    @NotNull
    public static List<TypeParameterDescriptorInitialization> createUninitializedClassTypeParameters(
            PsiClass psiClass, ClassDescriptor classDescriptor
    ) {
        List<TypeParameterDescriptorInitialization> result = Lists.newArrayList();
        for (PsiTypeParameter typeParameter : psiClass.getTypeParameters()) {
            TypeParameterDescriptorInitialization typeParameterDescriptor = makeUninitializedTypeParameter(classDescriptor, typeParameter);
            result.add(typeParameterDescriptor);
        }
        return result;
    }


    public List<TypeParameterDescriptor> resolveMethodTypeParameters(
            @NotNull PsiMethodWrapper method,
            @NotNull DeclarationDescriptor functionDescriptor
    ) {

        PsiMethod psiMethod = method.getPsiMethod();
        List<TypeParameterDescriptorInitialization> typeParametersIntialization =
                makeUninitializedTypeParameters(functionDescriptor, psiMethod.getTypeParameters());

        PsiClass psiMethodContainingClass = psiMethod.getContainingClass();
        assert psiMethodContainingClass != null;
        String context = "method " + method.getName() + " in class " + psiMethodContainingClass.getQualifiedName();
        initializeTypeParameters(typeParametersIntialization, functionDescriptor, context);

        List<TypeParameterDescriptor> typeParameters = Lists.newArrayListWithCapacity(typeParametersIntialization.size());

        for (TypeParameterDescriptorInitialization tpdi : typeParametersIntialization) {
            typeParameters.add(tpdi.descriptor);
        }

        return typeParameters;
    }
}
