/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import kotlin.Function0;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetCodeFragment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetImportList;
import org.jetbrains.jet.lang.psi.debugText.DebugTextPackage;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.JetModuleUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode;

public class LazyImportScope implements JetScope, LazyEntity {
    private final ResolveSession resolveSession;
    private final PackageViewDescriptor packageDescriptor;
    private final ImportsProvider importsProvider;
    private final JetScope rootScope;
    private final BindingTrace traceForImportResolve;
    private final String debugName;

    private static class ImportResolveStatus {
        private final LookupMode lookupMode;
        private final JetScope scope;
        private final Collection<? extends DeclarationDescriptor> descriptors;

        ImportResolveStatus(LookupMode lookupMode, JetScope scope, Collection<? extends DeclarationDescriptor> descriptors) {
            this.lookupMode = lookupMode;
            this.scope = scope;
            this.descriptors = descriptors;
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
                            "Scope for import '" + DebugTextPackage.getDebugText(directive) + "' resolve in " + toString());
                    directiveImportScope.changeLockLevel(WritableScope.LockLevel.BOTH);

                    Importer.StandardImporter importer = new Importer.StandardImporter(directiveImportScope);
                    directiveUnderResolve = directive;

                    Collection<? extends DeclarationDescriptor> descriptors;
                    try {
                        descriptors = resolveSession.getQualifiedExpressionResolver().processImportReference(
                                directive,
                                rootScope,
                                packageDescriptor.getMemberScope(),
                                importer,
                                traceForImportResolve,
                                resolveSession.getModuleDescriptor(),
                                mode);
                        if (mode == LookupMode.EVERYTHING) {
                            ImportsResolver.checkPlatformTypesMappedToKotlin(
                                    packageDescriptor.getModule(),
                                    traceForImportResolve,
                                    directive,
                                    descriptors
                            );
                        }
                    }
                    finally {
                        directiveUnderResolve = null;
                        directiveImportScope.changeLockLevel(WritableScope.LockLevel.READING);
                    }

                    importResolveStatus = new ImportResolveStatus(mode, directiveImportScope, descriptors);
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
            @NotNull String debugName,
            boolean inRootPackage
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

        this.rootScope = JetModuleUtil.getImportsResolutionScope(resolveSession.getModuleDescriptor(), inRootPackage);
    }

    public static LazyImportScope createImportScopeForFile(
            @NotNull ResolveSession resolveSession,
            @NotNull PackageViewDescriptor packageDescriptor,
            @NotNull JetFile jetFile,
            @NotNull BindingTrace traceForImportResolve,
            @NotNull String debugName
    ) {
        List<JetImportDirective> importDirectives;
        if (jetFile instanceof JetCodeFragment) {
            JetImportList importList = ((JetCodeFragment) jetFile).importsAsImportList();
            importDirectives = importList != null ? importList.getImports() : Collections.<JetImportDirective>emptyList();
        }
        else {
            importDirectives = jetFile.getImportDirectives();
        }

        return new LazyImportScope(
                resolveSession,
                packageDescriptor,
                Lists.reverse(importDirectives),
                traceForImportResolve,
                debugName,
                packageDescriptor.getFqName().isRoot());
    }

    @Override
    public void forceResolveAllContents() {
        for (JetImportDirective importDirective : importsProvider.getAllImports()) {
            forceResolveImportDirective(importDirective);
        }
    }

    public void forceResolveImportDirective(@NotNull JetImportDirective importDirective) {
        getImportScope(importDirective, LookupMode.EVERYTHING);

        ImportResolveStatus status = importedScopesProvider.invoke(importDirective).importResolveStatus;
        if (status != null && !status.descriptors.isEmpty()) {
            JetScope fileScope = resolveSession.getScopeProvider().getFileScope(importDirective.getContainingJetFile());
            ImportsResolver.reportUselessImport(importDirective, fileScope, status.descriptors, traceForImportResolve);
        }
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
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName) {
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
