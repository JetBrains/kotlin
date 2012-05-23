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
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;

/**
 * @author abreslav
 */
public class JavaBridgeConfiguration implements ModuleConfiguration {

    public static final ImportPath[] DEFAULT_JAVA_IMPORTS = new ImportPath[] { new ImportPath("java.lang.*") };


    @NotNull
    private Project project;
    @NotNull
    private JavaSemanticServices javaSemanticServices;
    @NotNull
    private ModuleConfiguration delegateConfiguration;
    @NotNull
    private CompilerSpecialMode mode;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @Inject
    public void setJavaSemanticServices(@NotNull JavaSemanticServices javaSemanticServices) {
        this.javaSemanticServices = javaSemanticServices;
    }

    @Inject
    public void setMode(@NotNull CompilerSpecialMode mode) {
        this.mode = mode;
    }

    @PostConstruct
    public void init() {
        this.delegateConfiguration = DefaultModuleConfiguration.createStandardConfiguration(project, mode == CompilerSpecialMode.BUILTINS);
    }



    @Override
    public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
        for (ImportPath importPath : DEFAULT_JAVA_IMPORTS) {
            directives.add(JetPsiFactory.createImportDirective(project, importPath));
        }
        delegateConfiguration.addDefaultImports(directives);
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        JetScope javaPackageScope = javaSemanticServices.getDescriptorResolver().getJavaPackageScope(DescriptorUtils.getFQName(namespaceDescriptor).toSafe(), namespaceDescriptor);
        if (javaPackageScope != null) {
            namespaceMemberScope.importScope(javaPackageScope);
        }
        delegateConfiguration.extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
    }


}
