package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

/**
 * @author svtk
 */
public interface Importer {

    void addAllUnderImport(@NotNull DeclarationDescriptor descriptor);

    void addAliasImport(@NotNull DeclarationDescriptor descriptor, @NotNull String aliasName);

    void addScopeImport(@NotNull JetScope scope);

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

        @Override
        public void addScopeImport(@NotNull JetScope scope) {
            importScope(scope);
        }

        protected void importScope(@NotNull JetScope scope) {
            namespaceScope.importScope(scope);
        }

        protected void importAllUnderDeclaration(@NotNull DeclarationDescriptor descriptor) {
            if (descriptor instanceof NamespaceDescriptor) {
                namespaceScope.importScope(((NamespaceDescriptor) descriptor).getMemberScope());
            }
            if (firstPhase) {
                if (descriptor instanceof ClassDescriptor) {
                    ClassDescriptor objectDescriptor = DescriptorUtils.getObjectIfObjectOrClassObjectDescriptor((ClassDescriptor) descriptor);
                    if (objectDescriptor != null) {
                        Collection<? extends DeclarationDescriptor> innerClassesAndObjects = objectDescriptor.getInnerClassesAndObjects();
                        for (DeclarationDescriptor innerClassOrObject : innerClassesAndObjects) {
                            namespaceScope.importClassifierAlias(innerClassOrObject.getName(), (ClassifierDescriptor) innerClassOrObject);
                        }
                    }
                }
                return;
            }
            if (descriptor instanceof VariableDescriptor) {
                JetType type = ((VariableDescriptor) descriptor).getOutType();
                namespaceScope.importScope(type.getMemberScope());
            }
            else if (descriptor instanceof ClassDescriptor) {
                JetType classObjectType = ((ClassDescriptor) descriptor).getClassObjectType();
                if (classObjectType != null) {
                    namespaceScope.importScope(classObjectType.getMemberScope());
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
        private final List<JetScope> scopesToImport = Lists.newArrayList();
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

        @Override
        public void addScopeImport(@NotNull JetScope scope) {
            scopesToImport.add(scope);
        }

        public void processImports() {
            for (JetScope scope : scopesToImport) {
                importScope(scope);
            }
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
