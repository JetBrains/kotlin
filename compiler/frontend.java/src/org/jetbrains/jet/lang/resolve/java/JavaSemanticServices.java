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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;

public class JavaSemanticServices {
    @NotNull
    private JavaTypeTransformer typeTransformer;
    @NotNull
    private JavaDescriptorResolver descriptorResolver;
    @NotNull
    private PsiClassFinder psiClassFinder;
    @NotNull
    private BindingTrace trace;
    @NotNull
    private PsiDeclarationProviderFactory psiDeclarationProviderFactory;

    @Inject
    public void setTypeTransformer(@NotNull JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setDescriptorResolver(@NotNull JavaDescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setPsiClassFinder(@NotNull PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @NotNull
    public JavaTypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    @Inject
    public void setPsiDeclarationProviderFactory(PsiDeclarationProviderFactory psiDeclarationProviderFactory) {
        this.psiDeclarationProviderFactory = psiDeclarationProviderFactory;
    }

    @NotNull
    public JavaDescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @Nullable
    public ClassDescriptor getKotlinClassDescriptor(@NotNull FqName qualifiedName) {
        return trace.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qualifiedName);
    }

    @NotNull
    public PsiClassFinder getPsiClassFinder() {
        return psiClassFinder;
    }

    @NotNull
    public PsiDeclarationProviderFactory getPsiDeclarationProviderFactory() {
        return psiDeclarationProviderFactory;
    }

    @Nullable
    public ClassDescriptor getKotlinBuiltinClassDescriptor(@NotNull FqName qualifiedName) {
        if (qualifiedName.firstSegmentIs(Name.identifier("jet")) && qualifiedName.pathSegments().size() == 2) {
            return (ClassDescriptor) KotlinBuiltIns.getInstance().getBuiltInsScope().getClassifier(qualifiedName.pathSegments().get(1));
        }
        else {
            return null;
        }
    }

    @Nullable
    public NamespaceDescriptor getKotlinNamespaceDescriptor(@NotNull FqName qualifiedName) {
        return trace.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, qualifiedName);
    }
}
