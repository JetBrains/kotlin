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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import javax.inject.Inject;

/**
 * @author abreslav
 */
public class JavaSemanticServices {
    @NotNull
    private JavaTypeTransformer typeTransformer;
    @NotNull
    private JavaDescriptorResolver descriptorResolver;
    @NotNull
    private BindingTrace trace;


    @Inject
    public void setTypeTransformer(@NotNull JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setDescriptorResolver(@NotNull JavaDescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }



    @NotNull
    public JavaTypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    @NotNull
    public JavaDescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @Nullable
    public ClassDescriptor getKotlinClassDescriptor(@NotNull FqName qualifiedName) {
        return trace.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qualifiedName);
    }

    @Nullable
    public ClassDescriptor getKotlinBuiltinClassDescriptor(@NotNull FqName qualifiedName) {
        if (qualifiedName.getFqName().startsWith("jet.")) {
            return (ClassDescriptor) JetStandardLibrary.getInstance().getLibraryScope().getClassifier(qualifiedName.getFqName().substring("jet.".length()));
        } else {
            return null;
        }
    }

    @Nullable
    public NamespaceDescriptor getKotlinNamespaceDescriptor(@NotNull FqName qualifiedName) {
        return trace.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, qualifiedName);
    }
}
