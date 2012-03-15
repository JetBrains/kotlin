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
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class JavaBridgeConfiguration implements ModuleConfiguration {

    public static final ImportPath[] DEFAULT_JAVA_IMPORTS = new ImportPath[] { new ImportPath("java.lang.*") };

    public static ModuleConfiguration createJavaBridgeConfiguration(@NotNull Project project, @NotNull BindingTrace trace, ModuleConfiguration delegateConfiguration) {
        return new JavaBridgeConfiguration(project, trace, delegateConfiguration);
    }

    private final Project project;
    private final JavaSemanticServices javaSemanticServices;
    private final ModuleConfiguration delegateConfiguration;

    private JavaBridgeConfiguration(Project project, BindingTrace trace, ModuleConfiguration delegateConfiguration) {
        this.project = project;
        this.javaSemanticServices = new JavaSemanticServices(project, trace);
        this.delegateConfiguration = delegateConfiguration;
    }

    @Override
    public void addDefaultImports(@NotNull WritableScope rootScope, @NotNull Collection<JetImportDirective> directives) {
        for (ImportPath importPath : DEFAULT_JAVA_IMPORTS) {
            directives.add(JetPsiFactory.createImportDirective(project, importPath));
        }
        delegateConfiguration.addDefaultImports(rootScope, directives);
    }

    public static JavaNamespaceDescriptor createNamespaceDescriptor(String name, FqName qualifiedName) {
        return new JavaNamespaceDescriptor(null, Collections.<AnnotationDescriptor>emptyList(), name, qualifiedName, true);
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        namespaceMemberScope.importScope(new JavaPackageScope(DescriptorUtils.getFQName(namespaceDescriptor).toSafe(), namespaceDescriptor, javaSemanticServices));
        delegateConfiguration.extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
    }


}
