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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.List;

/**
 * @author svtk
 */
/*package*/ interface Importer {

    void addAllUnderImport(@NotNull DeclarationDescriptor descriptor);

    void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull String aliasName);

    Importer DO_NOTHING = new Importer() {
        @Override
        public void addAllUnderImport(@NotNull DeclarationDescriptor descriptor) {
        }

        @Override
        public void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull String aliasName) {
        }
    };

    class StandardImporter implements Importer {
        private final WritableScope namespaceScope;
        private final boolean firstPhase;

        public StandardImporter(WritableScope namespaceScope, boolean firstPhase) {
            this.namespaceScope = namespaceScope;
            this.firstPhase = firstPhase;
        }

        @Override
        public void addAllUnderImport(@NotNull DeclarationDescriptor descriptor) {
            importAllUnderDeclaration(descriptor);
        }

        @Override
        public void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull String aliasName) {
            importDeclarationAlias(descriptor, aliasName);
        }

        protected void importAllUnderDeclaration(@NotNull DeclarationDescriptor descriptor) {
            if (descriptor instanceof NamespaceDescriptor) {
                namespaceScope.importScope(((NamespaceDescriptor) descriptor).getMemberScope());
            }
            if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.OBJECT) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                namespaceScope.importScope(classDescriptor.getUnsubstitutedInnerClassesScope());
                ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
                if (classObjectDescriptor != null) {
                    namespaceScope.importScope(classObjectDescriptor.getUnsubstitutedInnerClassesScope());
                }
            }
        }

        protected void importDeclarationAlias(@NotNull DeclarationDescriptor descriptor, @NotNull String aliasName) {
            if (descriptor instanceof ClassifierDescriptor) {
                namespaceScope.importClassifierAlias(aliasName, (ClassifierDescriptor) descriptor);
            }
            else if (descriptor instanceof NamespaceDescriptor) {
                namespaceScope.importNamespaceAlias(aliasName, (NamespaceDescriptor) descriptor);
            }
            else if (descriptor instanceof FunctionDescriptor) {
                namespaceScope.importFunctionAlias(aliasName, (FunctionDescriptor) descriptor);
            }
            else if (descriptor instanceof VariableDescriptor) {
                namespaceScope.importVariableAlias(aliasName, (VariableDescriptor) descriptor);
            }
        }

    }

    class DelayedImporter extends StandardImporter {
        private final List<Pair<DeclarationDescriptor, String>> imports = Lists.newArrayList();

        public DelayedImporter(@NotNull WritableScope namespaceScope, boolean firstPhase) {
            super(namespaceScope, firstPhase);
        }

        @Override
        public void addAllUnderImport(@NotNull DeclarationDescriptor descriptor) {
            imports.add(Pair.<DeclarationDescriptor, String>create(descriptor, null));
        }

        @Override
        public void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull String aliasName) {
            imports.add(Pair.create(descriptor, aliasName));
        }

        public void processImports() {
            for (Pair<DeclarationDescriptor, String> anImport : imports) {
                DeclarationDescriptor descriptor = anImport.getFirst();
                String aliasName = anImport.getSecond();
                boolean allUnderImport = aliasName == null;
                if (allUnderImport) {
                    importAllUnderDeclaration(descriptor);
                }
                else {
                    importDeclarationAlias(descriptor, aliasName);
                }
            }
        }
    }
}
