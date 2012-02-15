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
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author svtk
 */
public class StandardConfiguration implements Configuration {
    public static final String[] DEFAULT_JET_IMPORTS = new String[] { "std.*", "std.io.*" };

    private Project project;

    public static StandardConfiguration createStandardConfiguration(Project project) {
        return new StandardConfiguration(project);
    }

    private StandardConfiguration(Project project) {
        this.project = project;
    }

    @Override
    public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope, @NotNull Importer importer) {
        ImportsResolver.ImportResolver importResolver = new ImportsResolver.ImportResolver(trace, true);
        for (String defaultJetImport : DEFAULT_JET_IMPORTS) {
            importResolver.processImportReference(JetPsiFactory.createImportDirective(project, defaultJetImport), rootScope, importer);
        }
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
    }

}
