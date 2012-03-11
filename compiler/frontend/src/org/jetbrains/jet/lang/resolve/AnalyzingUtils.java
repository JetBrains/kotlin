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
import com.google.common.collect.Maps;
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
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
            @NotNull ModuleConfiguration configuration,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull BindingTraceContext bindingTraceContext) {

        JetScope libraryScope = JetStandardLibrary.getInstance().getLibraryScope();
        ModuleDescriptor owner = new ModuleDescriptor("<module>");

        final WritableScope scope = new WritableScopeImpl(
                JetScope.EMPTY, owner,
                new TraceBasedRedeclarationHandler(bindingTraceContext)).setDebugName("Root scope in analyzeNamespace");

        scope.importScope(libraryScope);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        TopDownAnalyzer.process(project, bindingTraceContext, scope, new NamespaceLike.Adapter(owner) {

            private Map<String, NamespaceDescriptorImpl> declaredNamespaces = Maps.newHashMap();

            @Override
            public NamespaceDescriptorImpl getNamespace(String name) {
                return declaredNamespaces.get(name);
            }

            @Override
            public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
                scope.addNamespace(namespaceDescriptor);
                declaredNamespaces.put(namespaceDescriptor.getName(), (NamespaceDescriptorImpl) namespaceDescriptor);
            }

            @Override
            public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
                throw new IllegalStateException("A class shouldn't sit right under a module: " + classDescriptor);
            }

            @Override
            public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
                throw new IllegalStateException("An object shouldn't sit right under a module: " + objectDescriptor);
            }

            @Override
            public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                throw new IllegalStateException("A function shouldn't sit right under a module: " + functionDescriptor);
            }

            @Override
            public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
                throw new IllegalStateException("A property shouldn't sit right under a module: " + propertyDescriptor);
            }

            @Override
            public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
                throw new IllegalStateException("Must be guaranteed not to happen by the parser");
            }
        }, files, filesToAnalyzeCompletely, flowDataTraceFactory, configuration);

        return bindingTraceContext.getBindingContext();
    }

}
