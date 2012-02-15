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
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collections;

/**
 * @author abreslav
 */
public class JavaBridgeConfiguration implements Configuration {

    public static final String[] DEFAULT_JAVA_IMPORTS = new String[] { "java.lang" };

    public static Configuration createJavaBridgeConfiguration(@NotNull Project project, @NotNull BindingTrace trace, Configuration delegateConfiguration) {
        return new JavaBridgeConfiguration(project, trace, delegateConfiguration);
    }

    private final JavaSemanticServices javaSemanticServices;
    private final Configuration delegateConfiguration;

    private JavaBridgeConfiguration(Project project, BindingTrace trace, Configuration delegateConfiguration) {
        this.javaSemanticServices = new JavaSemanticServices(project, JetSemanticServices.createSemanticServices(project), trace);
        this.delegateConfiguration = delegateConfiguration;
    }

    @Override
    public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope, @NotNull Importer importer) {
        rootScope.importScope(new JavaPackageScope("", createNamespaceDescriptor(JavaDescriptorResolver.JAVA_ROOT, ""), javaSemanticServices));
        for (String importFQN : DEFAULT_JAVA_IMPORTS) {
            NamespaceDescriptor namespaceDescriptor = javaSemanticServices.getDescriptorResolver().resolveNamespace(importFQN);
            if (namespaceDescriptor != null) {
                importer.addScopeImport(namespaceDescriptor.getMemberScope());
            }
        }
        delegateConfiguration.addDefaultImports(trace, rootScope, importer);
    }

    public static JavaNamespaceDescriptor createNamespaceDescriptor(String name, String qualifiedName) {
        return new JavaNamespaceDescriptor(null, Collections.<AnnotationDescriptor>emptyList(), name, qualifiedName, true);
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        namespaceMemberScope.importScope(new JavaPackageScope(DescriptorUtils.getFQName(namespaceDescriptor), namespaceDescriptor, javaSemanticServices));
        delegateConfiguration.extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
    }
}
