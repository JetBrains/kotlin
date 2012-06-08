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
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceLikeBuilderDummy;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.TraceBasedRedeclarationHandler;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.Progress;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class ReplInterpreter {

    private int lineNumber = 0;
    @Nullable
    private JetScope lastLineScope;
    private List<EarlierLine> earlierLines = Lists.newArrayList();
    private final ReplClassLoader classLoader;

    @NotNull
    private final InjectorForTopDownAnalyzerForJvm injector;
    @NotNull
    private final JetCoreEnvironment jetCoreEnvironment;
    @NotNull
    private final BindingTraceContext trace;
    @NotNull
    private final ModuleDescriptor module;

    public ReplInterpreter(@NotNull Disposable disposable, @NotNull CompilerDependencies compilerDependencies, @NotNull List<File> extraClasspath) {
        // TODO: add extraClasspath to jetCoreEnvironment
        jetCoreEnvironment = new JetCoreEnvironment(disposable, compilerDependencies);
        Project project = jetCoreEnvironment.getProject();
        trace = new BindingTraceContext();
        module = new ModuleDescriptor(Name.special("<repl>"));
        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(),
                false,
                true,
                Collections.<AnalyzerScriptParameter>emptyList());
        injector = new InjectorForTopDownAnalyzerForJvm(project, topDownAnalysisParameters, trace, module, compilerDependencies);

        if (extraClasspath.size() > 0) {
            URL[] urls = new URL[extraClasspath.size()];
            for (int i = 0; i < extraClasspath.size(); ++i) {
                try {
                    urls[i] = extraClasspath.get(i).toURI().toURL();
                } catch (Exception e) {
                    throw ExceptionUtils.rethrow(e);
                }
            }
            classLoader = new ReplClassLoader(new URLClassLoader(urls));
        }
        else {
            classLoader = new ReplClassLoader();
        }
    }

    public Object eval(@NotNull String line) {
        ++lineNumber;

        JvmClassName scriptClassName = JvmClassName.byInternalName("Line" + lineNumber);

        LightVirtualFile virtualFile = new LightVirtualFile("line" + lineNumber + ".ktscript", JetLanguage.INSTANCE, line);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(jetCoreEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);

        AnalyzingUtils.checkForSyntacticErrors(psiFile);

        injector.getTopDownAnalyzer().prepareForTheNextReplLine();

        ScriptDescriptor scriptDescriptor = doAnalyze(psiFile);

        Progress backendProgress = new Progress() {
            @Override
            public void log(String message) {
            }
        };

        List<Pair<ScriptDescriptor, JvmClassName>> earierScripts = Lists.newArrayList();

        for (EarlierLine earlierLine : earlierLines) {
            earierScripts.add(Pair.create(earlierLine.getScriptDescriptor(), earlierLine.getClassName()));
        }

        GenerationState generationState = new GenerationState(jetCoreEnvironment.getProject(), ClassBuilderFactories.binaries(false), backendProgress,
                AnalyzeExhaust.success(trace.getBindingContext(), JetStandardLibrary.getInstance()), Collections.singletonList(psiFile),
                jetCoreEnvironment.getCompilerDependencies().getCompilerSpecialMode());
        generationState.compileScript(psiFile.getScript(), scriptClassName, earierScripts, CompilationErrorHandler.THROW_EXCEPTION);

        for (String file : generationState.getFactory().files()) {
            classLoader.addClass(JvmClassName.byInternalName(file.replaceFirst("\\.class$", "")), generationState.getFactory().asBytes(file));
        }

        try {
            Class<?> scriptClass = classLoader.loadClass(scriptClassName.getFqName().getFqName());

            Class<?>[] constructorParams = new Class<?>[earlierLines.size()];
            Object[] constructorArgs = new Object[earlierLines.size()];

            for (int i = 0; i < earlierLines.size(); ++i) {
                constructorParams[i] = earlierLines.get(i).getScriptClass();
                constructorArgs[i] = earlierLines.get(i).getScriptInstance();
            }

            Constructor<?> scriptInstanceConstructor = scriptClass.getConstructor(constructorParams);
            Object scriptInstance = scriptInstanceConstructor.newInstance(constructorArgs);
            Field rvField = scriptClass.getDeclaredField("rv");
            rvField.setAccessible(true);
            Object rv = rvField.get(scriptInstance);

            earlierLines.add(new EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance, scriptClassName));

            return rv;
        } catch (Throwable e) {
            PrintWriter writer = new PrintWriter(System.err);
            classLoader.dumpClasses(writer);
            writer.flush();
            throw ExceptionUtils.rethrow(e);
        }
    }

    private ScriptDescriptor doAnalyze(@NotNull JetFile psiFile) {
        final WritableScope scope = new WritableScopeImpl(
                JetScope.EMPTY, module,
                new TraceBasedRedeclarationHandler(trace), "Root scope in analyzeNamespace");

        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        NamespaceDescriptorImpl rootNs = injector.getNamespaceFactory().createNamespaceDescriptorPathIfNeeded(FqName.ROOT);

        // map "jet" namespace into JetStandardLibrary/Classes
        // @see DefaultModuleConfiguraiton#extendNamespaceScope
        injector.getNamespaceFactory().createNamespaceDescriptorPathIfNeeded(JetStandardClasses.STANDARD_CLASSES_FQNAME);

        // Import a scope that contains all top-level namespaces that come from dependencies
        // This makes the namespaces visible at all, does not import themselves
        scope.importScope(rootNs.getMemberScope());

        if (lastLineScope != null) {
            scope.importScope(lastLineScope);
        }

        scope.changeLockLevel(WritableScope.LockLevel.READING);

        // dummy builder is used because "root" is module descriptor,
        // namespaces added to module explicitly in
        injector.getTopDownAnalyzer().doProcess(scope, new NamespaceLikeBuilderDummy(), Collections.singletonList(psiFile));

        // TODO: print, not throw
        AnalyzingUtils.throwExceptionOnErrors(trace.getBindingContext());

        ScriptDescriptor scriptDescriptor = injector.getTopDownAnalysisContext().getScripts().get(psiFile.getScript());
        lastLineScope = trace.get(BindingContext.SCRIPT_SCOPE, scriptDescriptor);
        if (lastLineScope == null) {
            throw new IllegalStateException("last line scope is not initialized");
        }

        return scriptDescriptor;
    }
}
