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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

public class JavaBridgeConfiguration implements ModuleConfiguration {

    public static final List<ImportPath> DEFAULT_JAVA_IMPORTS = ImmutableList.of(new ImportPath("java.lang.*"));

    private JavaSemanticServices javaSemanticServices;
    private ModuleConfiguration delegateConfiguration;

    @Inject
    public void setJavaSemanticServices(@NotNull JavaSemanticServices javaSemanticServices) {
        this.javaSemanticServices = javaSemanticServices;
    }

    @PostConstruct
    public void init() {
        this.delegateConfiguration = DefaultModuleConfiguration.createStandardConfiguration();
    }

    @Override
    public List<ImportPath> getDefaultImports() {
        Set<ImportPath> imports = Sets.newLinkedHashSet(DEFAULT_JAVA_IMPORTS);
        imports.addAll(delegateConfiguration.getDefaultImports());

        return Lists.newArrayList(imports);
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        JetScope javaPackageScope = javaSemanticServices.getDescriptorResolver().getJavaPackageScope(namespaceDescriptor);
        if (javaPackageScope != null) {
            namespaceMemberScope.importScope(javaPackageScope);
        }
        delegateConfiguration.extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
    }

    @NotNull
    @Override
    public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
        return JavaToKotlinClassMap.getInstance();
    }
}
