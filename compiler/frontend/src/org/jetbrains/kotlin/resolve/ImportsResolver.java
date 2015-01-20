/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ScriptDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.LookupMode;

public class ImportsResolver {
    private ModuleDescriptor moduleDescriptor;
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    private BindingTrace trace;
    private JetImportsFactory importsFactory;

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setQualifiedExpressionResolver(@NotNull QualifiedExpressionResolver qualifiedExpressionResolver) {
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
    }

    @Inject
    public void setImportsFactory(@NotNull JetImportsFactory importsFactory) {
        this.importsFactory = importsFactory;
    }

    public void processTypeImports(@NotNull TopDownAnalysisContext c) {
        processImports(c, LookupMode.ONLY_CLASSES_AND_PACKAGES);
    }

    public void processMembersImports(@NotNull TopDownAnalysisContext c) {
        processImports(c, LookupMode.EVERYTHING);
    }

    private void processImports(@NotNull TopDownAnalysisContext c, @NotNull LookupMode lookupMode) {
        for (JetFile file : c.getFiles()) {
            if (file.isScript()) continue;
            WritableScope fileScope = c.getFileScopes().get(file);
            processImportsInFile(lookupMode, fileScope, Lists.newArrayList(file.getImportDirectives()), file.getPackageFqName().isRoot());
        }
        // SCRIPT: process script import directives
        for (JetScript script : c.getScripts().keySet()) {
            WritableScope scriptScope = ((ScriptDescriptorImpl) c.getScripts().get(script)).getScopeForBodyResolution();
            processImportsInFile(lookupMode, scriptScope, script.getContainingJetFile().getImportDirectives(), true);
        }
    }

    private void processImportsInFile(@NotNull LookupMode lookupMode, WritableScope scope, List<JetImportDirective> directives, boolean inRootPackage) {
        processImportsInFile(lookupMode, scope, directives, moduleDescriptor, trace, qualifiedExpressionResolver, importsFactory, inRootPackage);
    }

    private static void processImportsInFile(
            LookupMode lookupMode,
            @NotNull WritableScope fileScope,
            @NotNull List<JetImportDirective> importDirectives,
            @NotNull ModuleDescriptor module,
            @NotNull BindingTrace trace,
            @NotNull QualifiedExpressionResolver qualifiedExpressionResolver,
            @NotNull JetImportsFactory importsFactory,
            boolean inRootPackage
    ) {
        @NotNull JetScope rootScope = JetModuleUtil.getSubpackagesOfRootScope(module);

        Importer importer = new Importer();
        if (lookupMode == LookupMode.EVERYTHING) {
            fileScope.clearImports();
        }

        for (ImportPath defaultImportPath : module.getDefaultImports()) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                    trace, "transient trace to resolve default imports"); //not to trace errors of default imports

            JetImportDirective defaultImportDirective = importsFactory.createImportDirective(defaultImportPath);
            qualifiedExpressionResolver.processImportReference(defaultImportDirective, rootScope, fileScope, importer,
                                                               temporaryTrace, lookupMode);
        }

        Map<JetImportDirective, Collection<? extends DeclarationDescriptor>> resolvedDirectives = Maps.newHashMap();

        JetScope rootScopeForFile = JetModuleUtil.getImportsResolutionScope(module, inRootPackage);

        for (JetImportDirective importDirective : importDirectives) {
            Collection<? extends DeclarationDescriptor> descriptors =
                    qualifiedExpressionResolver.processImportReference(importDirective, rootScopeForFile, fileScope, importer,
                                                                       trace, lookupMode);
            if (!descriptors.isEmpty()) {
                resolvedDirectives.put(importDirective, descriptors);
            }

            if (lookupMode != LookupMode.ONLY_CLASSES_AND_PACKAGES) {
                checkPlatformTypesMappedToKotlin(module, trace, importDirective, descriptors);
            }
        }
        importer.doImport(fileScope);

        if (lookupMode == LookupMode.EVERYTHING) {
            for (JetImportDirective importDirective : importDirectives) {
                reportConflictingImport(importDirective, fileScope, resolvedDirectives.get(importDirective), trace);
            }
        }
    }

    public static void checkPlatformTypesMappedToKotlin(
            @NotNull ModuleDescriptor module,
            @NotNull BindingTrace trace,
            @NotNull JetImportDirective importDirective,
            @NotNull Collection<? extends DeclarationDescriptor> descriptors
    ) {
        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference != null) {
            for (DeclarationDescriptor descriptor : descriptors) {
                reportPlatformClassMappedToKotlin(module, trace, importedReference, descriptor);
            }
        }
    }

    public static void reportPlatformClassMappedToKotlin(
            @NotNull ModuleDescriptor module,
            @NotNull BindingTrace trace,
            @NotNull JetElement element,
            @NotNull DeclarationDescriptor descriptor
    ) {
        if (!(descriptor instanceof ClassDescriptor)) return;

        PlatformToKotlinClassMap platformToKotlinMap = module.getPlatformToKotlinClassMap();
        Collection<ClassDescriptor> kotlinAnalogsForClass = platformToKotlinMap.mapPlatformClass((ClassDescriptor) descriptor);
        if (!kotlinAnalogsForClass.isEmpty()) {
            trace.report(PLATFORM_CLASS_MAPPED_TO_KOTLIN.on(element, kotlinAnalogsForClass));
        }
    }

    public static void reportConflictingImport(
            @NotNull JetImportDirective importDirective,
            @NotNull JetScope fileScope,
            @Nullable Collection<? extends DeclarationDescriptor> resolvedTo,
            @NotNull BindingTrace trace
    ) {

        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null || resolvedTo == null) return;

        Name aliasName = JetPsiUtil.getAliasName(importDirective);
        if (aliasName == null) return;

        if (resolvedTo.size() != 1) return;

        DeclarationDescriptor target = resolvedTo.iterator().next();
        if (target instanceof ClassDescriptor) {
            if (fileScope.getClassifier(aliasName) == null) {
                trace.report(CONFLICTING_IMPORT.on(importedReference, aliasName.asString()));
            }
        }
        else if (target instanceof PackageViewDescriptor) {
            if (fileScope.getPackage(aliasName) == null) {
                trace.report(CONFLICTING_IMPORT.on(importedReference, aliasName.asString()));
            }
        }
    }
}
