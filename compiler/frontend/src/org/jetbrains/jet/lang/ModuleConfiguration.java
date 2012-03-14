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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface ModuleConfiguration {
    ModuleConfiguration EMPTY = new ModuleConfiguration() {
        @Override
        public void addDefaultImports(@NotNull WritableScope rootScope, @NotNull Collection<JetImportDirective> directives) {
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        }

        @Override
        public NamespaceDescriptor getTopLevelNamespace(@NotNull String shortName) {
            return null;
        }

        @Override
        public void addAllTopLevelNamespacesTo(@NotNull Collection<? super NamespaceDescriptor> topLevelNamespaces) {
        }
    };

    void addDefaultImports(@NotNull WritableScope rootScope, @NotNull Collection<JetImportDirective> directives);

    /**
     *
     * This method is called every time a namespace descriptor is created. Use it to add extra descriptors to the namespace, e.g. merge a Java package with a Kotlin one
     */
    void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope);

    /** This method is called only if no namespace with the same short name is declared in the module itself, or to merge namespaces */
    @Nullable
    NamespaceDescriptor getTopLevelNamespace(@NotNull String shortName);

    /** Add all the top-level namespaces from the dependencies of this module into the given collection */
    void addAllTopLevelNamespacesTo(@NotNull Collection<? super NamespaceDescriptor> topLevelNamespaces);
}
