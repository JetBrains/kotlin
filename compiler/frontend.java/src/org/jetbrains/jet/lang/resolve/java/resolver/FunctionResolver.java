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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolveData;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.TypeVariableResolver;
import org.jetbrains.jet.lang.resolve.java.TypeVariableResolvers;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.AlternativeMethodSignatureData;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

public class FunctionResolver {
    private final JavaDescriptorResolver javaDescriptorResolver;

    public FunctionResolver(JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
    }

    @Nullable
    public SimpleFunctionDescriptor resolveMethodToFunctionDescriptor(
            @NotNull final PsiClass psiClass, final PsiMethodWrapper method,
            @NotNull JavaDescriptorResolveData.ResolverScopeData scopeData
    ) {

        JavaDescriptorResolver.getResolverScopeData(scopeData);

        PsiType returnPsiType = method.getReturnType();
        if (returnPsiType == null) {
            return null;
        }

        // TODO: ugly
        if (method.getJetMethod().hasPropertyFlag()) {
            return null;
        }

        final PsiMethod psiMethod = method.getPsiMethod();
        final PsiClass containingClass = psiMethod.getContainingClass();
        if (scopeData.isKotlin()) {
            // TODO: unless maybe class explicitly extends Object
            assert containingClass != null;
            String ownerClassName = containingClass.getQualifiedName();
            if (JavaDescriptorResolver.OBJECT_FQ_NAME.getFqName().equals(ownerClassName)) {
                return null;
            }
        }

        if (javaDescriptorResolver.getTrace().get(BindingContext.FUNCTION, psiMethod) != null) {
            return javaDescriptorResolver.getTrace().get(BindingContext.FUNCTION, psiMethod);
        }

        SimpleFunctionDescriptorImpl functionDescriptorImpl = new SimpleFunctionDescriptorImpl(
                scopeData.getClassOrNamespaceDescriptor(),
                javaDescriptorResolver.getAnnotationResolver().resolveAnnotations(psiMethod),
                Name.identifier(method.getName()),
                DescriptorKindUtils.flagsToKind(method.getJetMethod().kind())
        );

        String context = "method " + method.getName() + " in class " + psiClass.getQualifiedName();

        List<TypeParameterDescriptor> methodTypeParameters =
                javaDescriptorResolver.getJavaDescriptorSignatureResolver().resolveMethodTypeParameters(method,
                                                                                                        functionDescriptorImpl);

        TypeVariableResolver methodTypeVariableResolver = TypeVariableResolvers.typeVariableResolverFromTypeParameters(methodTypeParameters,
                                                                                                                       functionDescriptorImpl,
                                                                                                                       context);


        JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors = javaDescriptorResolver
                .resolveParameterDescriptors(functionDescriptorImpl, method.getParameters(), methodTypeVariableResolver);
        JetType returnType = javaDescriptorResolver.makeReturnType(returnPsiType, method, methodTypeVariableResolver);

        // TODO consider better place for this check
        AlternativeMethodSignatureData alternativeMethodSignatureData =
                new AlternativeMethodSignatureData(method, valueParameterDescriptors, returnType, methodTypeParameters);
        if (alternativeMethodSignatureData.isAnnotated() && !alternativeMethodSignatureData.hasErrors()) {
            valueParameterDescriptors = alternativeMethodSignatureData.getValueParameters();
            returnType = alternativeMethodSignatureData.getReturnType();
            methodTypeParameters = alternativeMethodSignatureData.getTypeParameters();
        }
        else if (alternativeMethodSignatureData.hasErrors()) {
            javaDescriptorResolver.getTrace().record(BindingContext.ALTERNATIVE_SIGNATURE_DATA_ERROR, functionDescriptorImpl,
                                                     alternativeMethodSignatureData.getError());
        }

        functionDescriptorImpl.initialize(
                valueParameterDescriptors.getReceiverType(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(scopeData.getClassOrNamespaceDescriptor()),
                methodTypeParameters,
                valueParameterDescriptors.getDescriptors(),
                returnType,
                JavaDescriptorResolver.resolveModality(method, method.isFinal()),
                JavaDescriptorResolver.resolveVisibility(psiMethod, method.getJetMethod()),
                /*isInline = */ false
        );

        if (functionDescriptorImpl.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            BindingContextUtils.recordFunctionDeclarationToDescriptor(javaDescriptorResolver.getTrace(), psiMethod, functionDescriptorImpl);
        }

        if (containingClass != psiClass && !method.isStatic()) {
            throw new IllegalStateException("non-static method in subclass");
        }
        return functionDescriptorImpl;
    }
}