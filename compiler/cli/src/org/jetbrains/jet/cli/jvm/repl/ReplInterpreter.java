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

package org.jetbrains.jet.cli.jvm.repl;

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.GeneratedClassLoader;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.Progress;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;

/**
 * @author Stepan Koltsov
 */
public class ReplInterpreter {

    private int lineNumber = 0;

    @NotNull
    private final InjectorForTopDownAnalyzerForJvm injector;
    @NotNull
    private final JetCoreEnvironment jetCoreEnvironment;
    @NotNull
    private final BindingTraceContext trace;

    public ReplInterpreter(@NotNull Disposable disposable, @NotNull CompilerDependencies compilerDependencies) {
        jetCoreEnvironment = new JetCoreEnvironment(disposable, compilerDependencies);
        Project project = jetCoreEnvironment.getProject();
        trace = new BindingTraceContext();
        ModuleDescriptor module = new ModuleDescriptor(Name.special("<repl>"));
        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(),
                false,
                true,
                Collections.<AnalyzerScriptParameter>emptyList());
        injector = new InjectorForTopDownAnalyzerForJvm(project, topDownAnalysisParameters, trace, module, compilerDependencies);
    }

    public Object eval(@NotNull String line) {
        ++lineNumber;

        LightVirtualFile virtualFile = new LightVirtualFile("line" + lineNumber + ".ktscript", JetLanguage.INSTANCE, line);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        AnalyzingUtils.checkForSyntacticErrors(psiFile);

        injector.getTopDownAnalyzer().prepareForTheNextReplLine();

        injector.getTopDownAnalyzer().analyzeFiles(Collections.singletonList(psiFile), Collections.<AnalyzerScriptParameter>emptyList());

        AnalyzingUtils.throwExceptionOnErrors(trace.getBindingContext());

        Progress backendProgress = new Progress() {
            @Override
            public void log(String message) {
            }
        };

        GenerationState generationState = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactories.binaries(false), backendProgress,
                AnalyzeExhaust.success(trace.getBindingContext(), JetStandardLibrary.getInstance()), Collections.singletonList(psiFile),
                jetCoreEnvironment.getCompilerDependencies().getCompilerSpecialMode());
        generationState.compileCorrectFiles(CompilationErrorHandler.THROW_EXCEPTION);

        try {
            Class<?> scriptClass = new GeneratedClassLoader(generationState.getFactory()).loadClass("Script");
            Constructor<?> scriptInstanceConstructor = scriptClass.getConstructor(new Class<?>[0]);
            Object scriptInstance = scriptInstanceConstructor.newInstance(new Object[0]);
            Field rvField = scriptClass.getDeclaredField("rv");
            rvField.setAccessible(true);
            Object rv = rvField.get(scriptInstance);
            return rv;
        } catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

}
