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

package org.jetbrains.jet.compiler;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.compiler.messages.CompilerMessageLocation;
import org.jetbrains.jet.compiler.messages.CompilerMessageSeverity;
import org.jetbrains.jet.compiler.messages.MessageCollector;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.utils.Progress;

import java.util.Collection;
import java.util.List;

/**
 * @author yole
 * @author abreslav
 */
public class KotlinToJVMBytecodeCompiler {

    @Nullable
    public static GenerationState analyzeAndGenerate(
            JetCoreEnvironment environment,
            CompilerDependencies dependencies,

            final MessageCollector messageCollector,

            boolean stubs
    ) {
        AnalyzeExhaust exhaust = analyze(environment, dependencies, messageCollector, stubs);

        if (exhaust == null) {
            return null;
        }

        exhaust.throwIfError();

        return generate(environment, dependencies, messageCollector, exhaust, stubs);
    }

    @Nullable
    private static AnalyzeExhaust analyze(
            JetCoreEnvironment environment,
            CompilerDependencies dependencies,
            final MessageCollector messageCollector,
            boolean stubs) {
        final Ref<Boolean> hasErrors = new Ref<Boolean>(false);
        final MessageCollector messageCollectorWrapper = new MessageCollector() {

            @Override
            public void report(@NotNull CompilerMessageSeverity severity,
                    @NotNull String message,
                    @NotNull CompilerMessageLocation location) {
                if (CompilerMessageSeverity.ERRORS.contains(severity)) {
                    hasErrors.set(true);
                }
                messageCollector.report(severity, message, location);
            }
        };

        // Report syntax errors
        for (JetFile file : environment.getSourceFiles()) {
            file.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitErrorElement(PsiErrorElement element) {
                    String description = element.getErrorDescription();
                    String message = StringUtil.isEmpty(description) ? "Syntax error" : description;
                    Diagnostic diagnostic = DiagnosticFactory.create(Severity.ERROR, message).on(element);
                    reportDiagnostic(messageCollectorWrapper, diagnostic);
                }
            });
        }

        // Analyze and report semantic errors
        Predicate<PsiFile> filesToAnalyzeCompletely =
                stubs ? Predicates.<PsiFile>alwaysFalse() : Predicates.<PsiFile>alwaysTrue();
        AnalyzeExhaust exhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                environment.getProject(), environment.getSourceFiles(), filesToAnalyzeCompletely, JetControlFlowDataTraceFactory.EMPTY,
                dependencies);

        for (Diagnostic diagnostic : exhaust.getBindingContext().getDiagnostics()) {
            reportDiagnostic(messageCollectorWrapper, diagnostic);
        }

        reportIncompleteHierarchies(messageCollectorWrapper, exhaust);

        return hasErrors.get() ? null : exhaust;
    }

    @NotNull
    private static GenerationState generate(
            JetCoreEnvironment environment,
            CompilerDependencies dependencies,

            final MessageCollector messageCollector,

            AnalyzeExhaust exhaust,

            boolean stubs) {
        Project project = environment.getProject();
        Progress backendProgress = new Progress() {
            @Override
            public void log(String message) {
                messageCollector.report(CompilerMessageSeverity.LOGGING, message, CompilerMessageLocation.NO_LOCATION);
            }
        };
        GenerationState generationState = new GenerationState(project, ClassBuilderFactories.binaries(stubs), backendProgress,
                                                              exhaust, environment.getSourceFiles(), dependencies.getCompilerSpecialMode());
        generationState.compileCorrectFiles(CompilationErrorHandler.THROW_EXCEPTION);

        List<CompilerPlugin> plugins = environment.getCompilerPlugins();
        if (plugins != null) {
            CompilerPluginContext context = new CompilerPluginContext(project, exhaust.getBindingContext(), environment.getSourceFiles());
            for (CompilerPlugin plugin : plugins) {
                plugin.processFiles(context);
            }
        }
        return generationState;
    }

    private static void reportDiagnostic(MessageCollector collector, Diagnostic diagnostic) {
        DiagnosticUtils.LineAndColumn lineAndColumn = DiagnosticUtils.getLineAndColumn(diagnostic);
        VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
        String path = virtualFile == null ? null : virtualFile.getPath();
        collector.report(convertSeverity(diagnostic.getSeverity()), diagnostic.getMessage(), CompilerMessageLocation.create(path, lineAndColumn.getLine(), lineAndColumn.getColumn()));
    }

    private static CompilerMessageSeverity convertSeverity(Severity severity) {
        switch (severity) {
            case INFO:
                return CompilerMessageSeverity.INFO;
            case ERROR:
                return CompilerMessageSeverity.ERROR;
            case WARNING:
                return CompilerMessageSeverity.WARNING;
        }
        throw new IllegalStateException("Unknown severity: " + severity);
    }

    private static void reportIncompleteHierarchies(MessageCollector collector, AnalyzeExhaust exhaust) {
        Collection<ClassDescriptor> incompletes = exhaust.getBindingContext().getKeys(BindingContext.INCOMPLETE_HIERARCHY);
        if (!incompletes.isEmpty()) {
            StringBuilder message = new StringBuilder("The following classes have incomplete hierarchies:\n");
            for (ClassDescriptor incomplete : incompletes) {
                String fqName = DescriptorUtils.getFQName(incomplete).getFqName();
                message.append("    ").append(fqName).append("\n");
            }
            collector.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION);
        }
    }
}
