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

package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.Collection;

/**
 * @author svtk
 */
public class DefaultModuleConfiguration implements ModuleConfiguration {
    public static final ImportPath[] DEFAULT_JET_IMPORTS = new ImportPath[] {
            new ImportPath("kotlin.*"), new ImportPath("kotlin.io.*"), new ImportPath("jet.*"), };

    private final Project project;
    private final @NotNull BuiltinsScopeExtensionMode builtinsScopeExtensionMode;

    public static DefaultModuleConfiguration createStandardConfiguration(Project project, @NotNull BuiltinsScopeExtensionMode builtinsScopeExtensionMode) {
        return new DefaultModuleConfiguration(project, builtinsScopeExtensionMode);
    }

    private DefaultModuleConfiguration(@NotNull Project project, @NotNull BuiltinsScopeExtensionMode builtinsScopeExtensionMode) {
        this.project = project;
        this.builtinsScopeExtensionMode = builtinsScopeExtensionMode;
    }

    @Override
    public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
        for (ImportPath defaultJetImport : DEFAULT_JET_IMPORTS) {
            directives.add(JetPsiFactory.createImportDirective(project, defaultJetImport));
        }
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        if (DescriptorUtils.getFQName(namespaceDescriptor).equalsTo(JetStandardClasses.STANDARD_CLASSES_FQNAME)) {
            switch (builtinsScopeExtensionMode) {
                case ALL:
                    namespaceMemberScope.importScope(JetStandardLibrary.getInstance().getLibraryScope());
                    break;
                case ONLY_STANDARD_CLASSES:
                    namespaceMemberScope.importScope(JetStandardClasses.STANDARD_CLASSES);
                    break;
            }

        }
    }
}
