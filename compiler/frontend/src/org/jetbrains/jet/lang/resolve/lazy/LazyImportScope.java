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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode;

public class LazyImportScope implements JetScope {
    private static class ImportResolveStatus {
        private final LookupMode lookupMode;
        private final JetScope scope;

        ImportResolveStatus(LookupMode lookupMode, JetScope scope) {
            this.lookupMode = lookupMode;
            this.scope = scope;
        }
    }

    private final ResolveSession resolveSession;
    private final NamespaceDescriptor packageDescriptor;
    private final ImportsProvider importsProvider;
    private final JetScope rootScope;
    private final BindingTrace traceForImportResolve;
    private final String debugName;

    private final Map<JetImportDirective, ImportResolveStatus> importedScopes = Maps.newHashMap();
    private JetImportDirective directiveUnderResolve = null;

    public LazyImportScope(
            @NotNull ResolveSession resolveSession,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull List<JetImportDirective> imports,
            @NotNull BindingTrace traceForImportResolve,
            @NotNull String debugName
    ) {
        this.resolveSession = resolveSession;
        this.packageDescriptor = packageDescriptor;
        this.importsProvider = new ImportsProvider(imports);
        this.traceForImportResolve = traceForImportResolve;
        this.debugName = debugName;

        NamespaceDescriptor rootPackageDescriptor = resolveSession.getPackageDescriptorByFqName(FqName.ROOT);
        if (rootPackageDescriptor == null) {
            throw new IllegalStateException("Root package not found");
        }
        rootScope = rootPackageDescriptor.getMemberScope();
    }

    public static LazyImportScope createImportScopeForFile(
            @NotNull ResolveSession resolveSession,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull JetFile jetFile,
            @NotNull BindingTrace traceForImportResolve,
            @NotNull String debugName
    ) {
        return new LazyImportScope(
                resolveSession,
                packageDescriptor,
                Lists.reverse(jetFile.getImportDirectives()),
                traceForImportResolve,
                debugName);
    }

    @Nullable
    private <D extends DeclarationDescriptor> D selectFirstFromImports(
            Name name,
            LookupMode lookupMode,
            JetScopeSelectorUtil.ScopeByNameSelector<D> descriptorSelector
    ) {
        for (JetImportDirective directive : importsProvider.getImports(name)) {
            if (directive == directiveUnderResolve) {
                // This is the recursion in imports analysis
                return null;
            }

            D foundDescriptor = descriptorSelector.get(getImportScope(directive, lookupMode), name);
            if (foundDescriptor != null) {
                return foundDescriptor;
            }
        }

        return null;
    }

    @NotNull
    private <D extends DeclarationDescriptor> Collection<D> collectFromImports(
            Name name,
            LookupMode lookupMode,
            JetScopeSelectorUtil.ScopeByNameMultiSelector<D> descriptorsSelector
    ) {
        Set<D> descriptors = Sets.newHashSet();
        for (JetImportDirective directive : importsProvider.getImports(name)) {
            if (directive == directiveUnderResolve) {
                // This is the recursion in imports analysis
                throw new IllegalStateException("Recursion while resolving many imports: " + directive.getText());
            }

            descriptors.addAll(descriptorsSelector.get(getImportScope(directive, lookupMode), name));
        }

        return descriptors;
    }

    @NotNull
    private <D extends DeclarationDescriptor> Collection<D> collectFromImports(
            LookupMode lookupMode,
            JetScopeSelectorUtil.ScopeDescriptorSelector<D> descriptorsSelector
    ) {
        Set<D> descriptors = Sets.newHashSet();
        for (JetImportDirective directive : importsProvider.getAllImports()) {
            if (directive == directiveUnderResolve) {
                // This is the recursion in imports analysis
                throw new IllegalStateException("Recursion while resolving many imports: " + directive.getText());
            }

            descriptors.addAll(descriptorsSelector.get(getImportScope(directive, lookupMode)));
        }

        return descriptors;
    }

    @NotNull
    private JetScope getImportScope(JetImportDirective directive, LookupMode lookupMode) {
        ImportResolveStatus status = importedScopes.get(directive);
        if (status != null && (lookupMode == status.lookupMode || status.lookupMode == LookupMode.EVERYTHING)) {
            return status.scope;
        }

        WritableScope directiveImportScope = new WritableScopeImpl(
                    JetScope.EMPTY, packageDescriptor, RedeclarationHandler.DO_NOTHING,
                    "Scope for import '" + directive.getText() + "' resolve in " + toString());
        directiveImportScope.changeLockLevel(WritableScope.LockLevel.BOTH);

        Importer.StandardImporter importer = new Importer.StandardImporter(directiveImportScope);
        directiveUnderResolve = directive;

        try {
            resolveSession.getInjector().getQualifiedExpressionResolver().processImportReference(
                    directive,
                    rootScope,
                    packageDescriptor.getMemberScope(),
                    importer,
                    traceForImportResolve,
                    resolveSession.getRootModuleDescriptor(),
                    lookupMode);
        }
        finally {
            directiveUnderResolve = null;
            directiveImportScope.changeLockLevel(WritableScope.LockLevel.READING);
            importedScopes.put(directive, new ImportResolveStatus(lookupMode, directiveImportScope));
        }

        return directiveImportScope;
    }

    @Nullable
    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return selectFirstFromImports(name, LookupMode.ONLY_CLASSES, JetScopeSelectorUtil.CLASSIFIER_DESCRIPTOR_SCOPE_SELECTOR);
    }

    @Nullable
    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return selectFirstFromImports(name, LookupMode.ONLY_CLASSES, JetScopeSelectorUtil.NAMED_OBJECT_SCOPE_SELECTOR);
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getObjectDescriptors() {
        return collectFromImports(LookupMode.ONLY_CLASSES, JetScopeSelectorUtil.OBJECTS_SCOPE_SELECTOR);
    }

    @Nullable
    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return selectFirstFromImports(name, LookupMode.ONLY_CLASSES, JetScopeSelectorUtil.NAMESPACE_SCOPE_SELECTOR);
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return collectFromImports(name, LookupMode.EVERYTHING, JetScopeSelectorUtil.NAMED_PROPERTIES_SCOPE_SELECTOR);
    }

    @Nullable
    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return collectFromImports(name, LookupMode.EVERYTHING, JetScopeSelectorUtil.NAMED_FUNCTION_SCOPE_SELECTOR);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return packageDescriptor;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        return null;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return collectFromImports(LookupMode.EVERYTHING, JetScopeSelectorUtil.ALL_DESCRIPTORS_SCOPE_SELECTOR);
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "LazyImportScope: " + debugName;
    }
}
