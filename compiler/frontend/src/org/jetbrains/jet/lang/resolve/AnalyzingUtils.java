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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeAdapter;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class AnalyzingUtils {

    public static void checkForSyntacticErrors(@NotNull PsiElement root) {
        root.acceptChildren(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }

            @Override
            public void visitErrorElement(PsiErrorElement element) {
                throw new IllegalArgumentException(element.getErrorDescription() + "; looking at " + element.getNode().getElementType() + " '" + element.getText() + DiagnosticUtils.atLocation(element));
            }
        });
    }
    
    public static List<PsiErrorElement> getSyntaxErrorRanges(@NotNull PsiElement root) {
        final ArrayList<PsiErrorElement> r = new ArrayList<PsiErrorElement>();
        root.acceptChildren(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }

            @Override
            public void visitErrorElement(PsiErrorElement element) {
                r.add(element);
            }
        });
        return r;
    }

    public static void throwExceptionOnErrors(BindingContext bindingContext) {
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            DiagnosticHolder.THROW_EXCEPTION.report(diagnostic);
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------

    public static BindingContext analyzeFiles(
            @NotNull Project project,
            @NotNull ModuleConfiguration configuration,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        return analyzeFilesWithGivenTrace(project, configuration, files, filesToAnalyzeCompletely, flowDataTraceFactory, bindingTraceContext);
    }

    public static BindingContext analyzeFilesWithGivenTrace(
            @NotNull Project project,
            @NotNull final ModuleConfiguration configuration,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull BindingTraceContext bindingTraceContext) {

        final ModuleDescriptor owner = new ModuleDescriptor("<module>");

        final WritableScope scope = new WritableScopeImpl(
                JetScope.EMPTY, owner,
                new TraceBasedRedeclarationHandler(bindingTraceContext)).setDebugName("Root scope in analyzeNamespace");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        // Import the lang package
        scope.importScope(JetStandardLibrary.getInstance().getLibraryScope());

        // Import a scope that contains all top-level namespaces that come from dependencies
        // This makes the namespaces visible at all, does not import themselves
        scope.importScope(new JetScopeAdapter(JetScope.EMPTY) {
            @Override
            public NamespaceDescriptor getNamespace(@NotNull String name) {
                // Is it a top-level namespace coming from the dependencies?
                NamespaceDescriptor topLevelNamespaceFromConfiguration = configuration.getTopLevelNamespace(name);
                if (topLevelNamespaceFromConfiguration != null) {
                    return topLevelNamespaceFromConfiguration;
                }
                // Should be null, we are delegating to EMPTY
                return super.getNamespace(name);
            }

            @NotNull
            @Override
            public Collection<DeclarationDescriptor> getAllDescriptors() {
                List<DeclarationDescriptor> allDescriptors = Lists.newArrayList();
                configuration.addAllTopLevelNamespacesTo(allDescriptors);
                return allDescriptors;
            }
        });

        // dummy builder is used because "root" is module descriptor,
        // namespaces added to module explicitly in
        TopDownAnalyzer.process(project, bindingTraceContext, scope,
            new NamespaceLikeBuilderDummy(), owner, files, filesToAnalyzeCompletely, flowDataTraceFactory, configuration);

        return bindingTraceContext.getBindingContext();
    }

}
