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
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.diagnostics.Errors.USELESS_HIDDEN_IMPORT;
import static org.jetbrains.jet.lang.diagnostics.Errors.USELESS_SIMPLE_IMPORT;

/**
 * @author abreslav
 * @author svtk
 */
public class ImportsResolver {
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ModuleConfiguration configuration;
    @NotNull
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    @NotNull
    private BindingTrace trace;

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

    public void processTypeImports(@NotNull JetScope rootScope) {
        processImports(true, rootScope);
    }

    public void processMembersImports(@NotNull JetScope rootScope) {
        processImports(false, rootScope);
    }

    private void processImports(boolean onlyClasses, @NotNull JetScope rootScope) {
        for (JetFile file : context.getNamespaceDescriptors().keySet()) {
            WritableScope namespaceScope = context.getNamespaceScopes().get(file);
            processImportsInFile(onlyClasses, namespaceScope, ScopeProvider.getFileImports(file), rootScope);
        }
        for (JetScript script : context.getScripts().keySet()) {
            WritableScope scriptScope = context.getScriptScopes().get(script);
            processImportsInFile(onlyClasses, scriptScope, script.getImportDirectives(), rootScope);
        }
    }

    private void processImportsInFile(boolean classes, WritableScope scope, List<JetImportDirective> directives, JetScope rootScope) {
        processImportsInFile(classes, scope, directives, rootScope, configuration, trace, qualifiedExpressionResolver);
    }

    public static void processImportsInFile(
            boolean onlyClasses,
            @NotNull WritableScope namespaceScope,
            @NotNull List<JetImportDirective> importDirectives,
            @NotNull JetScope rootScope,
            @NotNull ModuleConfiguration configuration,
            @NotNull BindingTrace trace,
            @NotNull QualifiedExpressionResolver qualifiedExpressionResolver
    ) {

        Importer.DelayedImporter delayedImporter = new Importer.DelayedImporter(namespaceScope);
        if (!onlyClasses) {
            namespaceScope.clearImports();
        }
        Map<JetImportDirective, DeclarationDescriptor> resolvedDirectives = Maps.newHashMap();
        Collection<JetImportDirective> defaultImportDirectives = Lists.newArrayList();
        configuration.addDefaultImports(defaultImportDirectives);
        for (JetImportDirective defaultImportDirective : defaultImportDirectives) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace); //not to trace errors of default imports
            qualifiedExpressionResolver.processImportReference(defaultImportDirective, rootScope, namespaceScope, delayedImporter, temporaryTrace, onlyClasses);
        }

        for (JetImportDirective importDirective : importDirectives) {
            Collection<? extends DeclarationDescriptor> descriptors =
                qualifiedExpressionResolver.processImportReference(importDirective, rootScope, namespaceScope, delayedImporter, trace, onlyClasses);
            if (descriptors.size() == 1) {
                resolvedDirectives.put(importDirective, descriptors.iterator().next());
            }
        }
        delayedImporter.processImports();

        if (!onlyClasses) {
            for (JetImportDirective importDirective : importDirectives) {
                reportUselessImport(importDirective, namespaceScope, resolvedDirectives, trace);
            }
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
