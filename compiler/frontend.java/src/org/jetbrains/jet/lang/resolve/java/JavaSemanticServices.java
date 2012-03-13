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

/**
 * @author abreslav
 */
public class JavaSemanticServices {
    private final JavaTypeTransformer typeTransformer;
    private final JavaDescriptorResolver descriptorResolver;
    private final BindingTrace trace;

    public JavaSemanticServices(Project project, BindingTrace trace) {
        this.trace = trace;
        this.descriptorResolver = new JavaDescriptorResolver(project, this);
        this.typeTransformer = new JavaTypeTransformer(this, JetStandardLibrary.getInstance(), descriptorResolver);
    }

    @NotNull
    public JavaTypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    @NotNull
    public JavaDescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @NotNull
    public BindingTrace getTrace() {
        return trace;
    }

    @Nullable
    public ClassDescriptor getKotlinClassDescriptor(@NotNull FqName qualifiedName) {
        if (qualifiedName.getFqName().startsWith("jet.")) {
            ClassDescriptor r = (ClassDescriptor) JetStandardLibrary.getInstance().getLibraryScope().getClassifier(qualifiedName.getFqName().substring("jet.".length()));
            if (r == null) {
                // TODO: better error
                //throw new IllegalStateException();
            }
            return r;
        }
        return getTrace().get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, qualifiedName);
    }

    @Nullable
    public NamespaceDescriptor getKotlinNamespaceDescriptor(@NotNull FqName qualifiedName) {
        return getTrace().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, qualifiedName);
    }
}
