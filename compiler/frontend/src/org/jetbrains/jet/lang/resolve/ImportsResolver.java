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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode;

public class ImportsResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ModuleConfiguration configuration;
    @NotNull
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    @NotNull
    private BindingTrace trace;
    @NotNull
    private JetImportsFactory importsFactory;

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setConfiguration(@NotNull ModuleConfiguration configuration) {
        this.configuration = configuration;
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

    public void processTypeImports(@NotNull JetScope rootScope) {
        processImports(LookupMode.ONLY_CLASSES, rootScope);
    }

    public void processMembersImports(@NotNull JetScope rootScope) {
        processImports(LookupMode.EVERYTHING, rootScope);
    }

    private void processImports(@NotNull LookupMode lookupMode, @NotNull JetScope rootScope) {
        for (JetFile file : context.getNamespaceDescriptors().keySet()) {
            WritableScope namespaceScope = context.getNamespaceScopes().get(file);
            processImportsInFile(lookupMode, namespaceScope, Lists.newArrayList(file.getImportDirectives()), rootScope);
        }
        for (JetScript script : context.getScripts().keySet()) {
            WritableScope scriptScope = context.getScriptScopes().get(script);
            processImportsInFile(lookupMode, scriptScope, script.getImportDirectives(), rootScope);
        }
    }

    private void processImportsInFile(@NotNull LookupMode lookupMode, WritableScope scope, List<JetImportDirective> directives, JetScope rootScope) {
        processImportsInFile(lookupMode, scope, directives, rootScope, configuration, trace, qualifiedExpressionResolver, importsFactory);
    }

    public static void processImportsInFile(
            LookupMode lookupMode,
            @NotNull WritableScope namespaceScope,
            @NotNull List<JetImportDirective> importDirectives,
            @NotNull JetScope rootScope,
            @NotNull ModuleConfiguration configuration,
            @NotNull BindingTrace trace,
            @NotNull QualifiedExpressionResolver qualifiedExpressionResolver,
            @NotNull JetImportsFactory importsFactory
    ) {

        Importer.DelayedImporter delayedImporter = new Importer.DelayedImporter(namespaceScope);
        if (lookupMode == LookupMode.EVERYTHING) {
            namespaceScope.clearImports();
        }

        for (ImportPath defaultImportPath : configuration.getDefaultImports()) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                    trace, "transient trace to resolve default imports"); //not to trace errors of default imports

            JetImportDirective defaultImportDirective = importsFactory.createImportDirective(defaultImportPath);
            qualifiedExpressionResolver.processImportReference(defaultImportDirective, rootScope, namespaceScope, delayedImporter,
                                                               temporaryTrace, configuration, lookupMode);
        }

        Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives = Maps.newHashMap();

        for (JetImportDirective importDirective : importDirectives) {
            Collection<? extends DeclarationDescriptor> descriptors =
                qualifiedExpressionResolver.processImportReference(importDirective, rootScope, namespaceScope, delayedImporter,
                                                                   trace, configuration, lookupMode);
            if (descriptors.size() == 1) {
                resolvedDirectives.put(importDirective, descriptors.iterator().next());
            }

            JetExpression importedReference = importDirective.getImportedReference();
            if (lookupMode != LookupMode.ONLY_CLASSES && importedReference != null) {
                for (DeclarationDescriptor descriptor : descriptors) {
                    reportPlatformClassMappedToKotlin(configuration, trace, importedReference, descriptor);
                }
            }
        }
        delayedImporter.processImports();

        if (lookupMode == LookupMode.EVERYTHING) {
            for (JetImportDirective importDirective : importDirectives) {
                reportUselessImport(importDirective, namespaceScope, resolvedDirectives, trace);
            }
        }
    }

    public static void reportPlatformClassMappedToKotlin(
            @NotNull ModuleConfiguration configuration,
            @NotNull BindingTrace trace,
            @NotNull JetElement element,
            @NotNull DeclarationDescriptor descriptor
    ) {
        if (!(descriptor instanceof ClassDescriptor)) return;

        PlatformToKotlinClassMap platformToKotlinMap = configuration.getPlatformToKotlinClassMap();
        Collection<ClassDescriptor> kotlinAnalogsForClass = platformToKotlinMap.mapPlatformClass((ClassDescriptor) descriptor);
        if (!kotlinAnalogsForClass.isEmpty()) {
            trace.report(PLATFORM_CLASS_MAPPED_TO_KOTLIN.on(element, kotlinAnalogsForClass));
        }
    }

    private static void reportUselessImport(
        @NotNull JetImportDirective importDirective,
        @NotNull WritableScope namespaceScope,
        @NotNull Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives,
        @NotNull BindingTrace trace
    ) {

        JetExpression importedReference = importDirective.getImportedReference();
        if (importedReference == null || !resolvedDirectives.containsKey(importDirective)) {
            return;
        }
        Name aliasName = JetPsiUtil.getAliasName(importDirective);
        if (aliasName == null) {
            return;
        }

        DeclarationDescriptor wasResolved = resolvedDirectives.get(importDirective);
        DeclarationDescriptor isResolved = null;
        if (wasResolved instanceof ClassDescriptor) {
            isResolved = namespaceScope.getClassifier(aliasName);
        }
        else if (wasResolved instanceof VariableDescriptor) {
            isResolved = namespaceScope.getLocalVariable(aliasName);
        }
        else if (wasResolved instanceof NamespaceDescriptor) {
            isResolved = namespaceScope.getNamespace(aliasName);
        }
        if (isResolved != null && isResolved != wasResolved) {
            trace.report(USELESS_HIDDEN_IMPORT.on(importedReference));
        }
        if (!importDirective.isAllUnder() &&
            importedReference instanceof JetSimpleNameExpression &&
            importDirective.getAliasName() == null) {
            trace.report(USELESS_SIMPLE_IMPORT.on(importedReference));
        }
    }
}
