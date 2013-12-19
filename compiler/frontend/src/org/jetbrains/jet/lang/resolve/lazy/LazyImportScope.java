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
import com.google.common.collect.Sets;
import jet.Function0;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode;

public class LazyImportScope implements JetScope {
    private final ResolveSession resolveSession;
    private final PackageViewDescriptor packageDescriptor;
    private final ImportsProvider importsProvider;
    private final JetScope rootScope;
    private final BindingTrace traceForImportResolve;
    private final String debugName;

    private static class ImportResolveStatus {
        private final LookupMode lookupMode;
        private final JetScope scope;

        ImportResolveStatus(LookupMode lookupMode, JetScope scope) {
            this.lookupMode = lookupMode;
            this.scope = scope;
        }
    }

    private class ImportDirectiveResolveCache {
        private final JetImportDirective directive;

        @Nullable
        private volatile ImportResolveStatus importResolveStatus;

        private ImportDirectiveResolveCache(JetImportDirective directive) {
            this.directive = directive;
        }

        private JetScope scopeForMode(final LookupMode mode) {
            ImportResolveStatus status = importResolveStatus;
            if (status != null && (status.lookupMode == mode || status.lookupMode == LookupMode.EVERYTHING)) {
                return status.scope;
            }

            return resolveSession.getStorageManager().compute(new Function0<JetScope>() {
                @Override
                public JetScope invoke() {
                    ImportResolveStatus cachedStatus = importResolveStatus;
                    if (cachedStatus != null && (cachedStatus.lookupMode == mode || cachedStatus.lookupMode == LookupMode.EVERYTHING)) {
                        return cachedStatus.scope;
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
                                resolveSession.getModuleDescriptor(),
                                mode);
                    }
                    finally {
                        directiveUnderResolve = null;
                        directiveImportScope.changeLockLevel(WritableScope.LockLevel.READING);
                    }

                    importResolveStatus = new ImportResolveStatus(mode, directiveImportScope);
                    return directiveImportScope;
                }
            });
        }
    }

    private final MemoizedFunctionToNotNull<JetImportDirective, ImportDirectiveResolveCache> importedScopesProvider;

    private JetImportDirective directiveUnderResolve = null;

    public LazyImportScope(
            @NotNull ResolveSession resolveSession,
            @NotNull PackageViewDescriptor packageDescriptor,
            @NotNull List<JetImportDirective> imports,
            @NotNull BindingTrace traceForImportResolve,
            @NotNull String debugName
    ) {
        this.resolveSession = resolveSession;
        this.packageDescriptor = packageDescriptor;
        this.importsProvider = new ImportsProvider(resolveSession.getStorageManager(), imports);
        this.traceForImportResolve = traceForImportResolve;
        this.debugName = debugName;

        this.importedScopesProvider = resolveSession.getStorageManager().createMemoizedFunction(new Function1<JetImportDirective, ImportDirectiveResolveCache>() {
            @Override
            public ImportDirectiveResolveCache invoke(JetImportDirective directive) {
                return new ImportDirectiveResolveCache(directive);
            }
        });

        PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
        if (rootPackage == null) {
            throw new IllegalStateException("Root package not found");
        }
        rootScope = rootPackage.getMemberScope();
    }

    public static LazyImportScope createImportScopeForFile(
            @NotNull ResolveSession resolveSession,
            @NotNull PackageViewDescriptor packageDescriptor,
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
            final Name name,
            final LookupMode lookupMode,
            final JetScopeSelectorUtil.ScopeByNameSelector<D> descriptorSelector
    ) {
        return resolveSession.getStorageManager().compute(new Function0<D>() {
            @Override
            public D invoke() {
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
        });
    }

    @NotNull
    private <D extends DeclarationDescriptor> Collection<D> collectFromImports(
            final Name name,
            final LookupMode lookupMode,
            final JetScopeSelectorUtil.ScopeByNameMultiSelector<D> descriptorsSelector
    ) {
        return resolveSession.getStorageManager().compute(new Function0<Collection<D>>() {
            @Override
            public Collection<D> invoke() {
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
        });
    }

    @NotNull
    private <D extends DeclarationDescriptor> Collection<D> collectFromImports(
            final LookupMode lookupMode,
            final JetScopeSelectorUtil.ScopeDescriptorSelector<D> descriptorsSelector
    ) {
        return resolveSession.getStorageManager().compute(new Function0<Collection<D>>() {
            @Override
            public Collection<D> invoke() {
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
        });
    }

    @NotNull
    private JetScope getImportScope(JetImportDirective directive, LookupMode lookupMode) {
        return importedScopesProvider.invoke(directive).scopeForMode(lookupMode);
    }

    @Nullable
    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return selectFirstFromImports(name, LookupMode.ONLY_CLASSES, JetScopeSelectorUtil.CLASSIFIER_DESCRIPTOR_SCOPE_SELECTOR);
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return selectFirstFromImports(name, LookupMode.ONLY_CLASSES, JetScopeSelectorUtil.PACKAGE_SCOPE_SELECTOR);
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

    @TestOnly
    @Override
    public void printScopeStructure(@NotNull Printer p) {
        p.println(getClass().getSimpleName(), ": ", debugName, " {");
        p.pushIndent();

        p.println("packageDescriptor = ", packageDescriptor);

        p.print("rootScope = ");
        rootScope.printScopeStructure(p.withholdIndentOnce());

        p.popIndent();
        p.println("}");
    }
}
