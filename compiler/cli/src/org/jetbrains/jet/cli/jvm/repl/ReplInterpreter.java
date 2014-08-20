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

package org.jetbrains.jet.cli.jvm.repl;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
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
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.messages.MessageCollectorToString;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassBuilderFactories;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.KotlinCodegenFacade;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PackageLikeBuilderDummy;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.storage.ExceptionTracker;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.utils.UtilsPackage;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.asmTypeByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.registerClassNameForScript;

public class ReplInterpreter {
    private int lineNumber = 0;

    @Nullable
    private JetScope lastLineScope;
    private final List<EarlierLine> earlierLines = Lists.newArrayList();
    private final List<String> previousIncompleteLines = Lists.newArrayList();
    private final ReplClassLoader classLoader;

    private final PsiFileFactoryImpl psiFileFactory;
    private final BindingTraceContext trace;
    private final ModuleDescriptorImpl module;
    private final TopDownAnalysisContext topDownAnalysisContext;
    private final TopDownAnalyzer topDownAnalyzer;

    public ReplInterpreter(@NotNull Disposable disposable, @NotNull CompilerConfiguration configuration) {
        JetCoreEnvironment environment = JetCoreEnvironment.createForProduction(disposable, configuration);
        Project project = environment.getProject();
        this.psiFileFactory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        this.trace = new BindingTraceContext();
        this.module = AnalyzerFacadeForJVM.createJavaModule("<repl>");
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.createForLocalDeclarations(
                new LockBasedStorageManager(),
                new ExceptionTracker(), // dummy
                Predicates.<PsiFile>alwaysTrue()
        );
        InjectorForTopDownAnalyzerForJvm injector = new InjectorForTopDownAnalyzerForJvm(project, topDownAnalysisParameters, trace, module);
        this.topDownAnalysisContext = new TopDownAnalysisContext(topDownAnalysisParameters);
        this.topDownAnalyzer = injector.getTopDownAnalyzer();

        module.initialize(new CompositePackageFragmentProvider(
                Arrays.asList(
                        topDownAnalyzer.getPackageFragmentProvider(),
                        injector.getJavaDescriptorResolver().getPackageFragmentProvider()
                )
        ));
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();

        List<URL> classpath = Lists.newArrayList();
        for (File file : configuration.getList(JVMConfigurationKeys.CLASSPATH_KEY)) {
            try {
                classpath.add(file.toURI().toURL());
            }
            catch (MalformedURLException e) {
                throw UtilsPackage.rethrow(e);
            }
        }

        this.classLoader = new ReplClassLoader(new URLClassLoader(classpath.toArray(new URL[classpath.size()])));
    }

    private static void prepareForTheNextReplLine(@NotNull TopDownAnalysisContext c) {
        c.getScripts().clear();
    }

    public enum LineResultType {
        SUCCESS,
        ERROR,
        INCOMPLETE,
    }

    public static class LineResult {
        private final Object value;
        private final boolean unit;
        private final String errorText;
        private final LineResultType type;

        private LineResult(Object value, boolean unit, String errorText, @NotNull LineResultType type) {
            this.value = value;
            this.unit = unit;
            this.errorText = errorText;
            this.type = type;
        }

        @NotNull
        public LineResultType getType() {
            return type;
        }

        private void checkSuccessful() {
            if (getType() != LineResultType.SUCCESS) {
                throw new IllegalStateException("it is error");
            }
        }

        public Object getValue() {
            checkSuccessful();
            return value;
        }

        public boolean isUnit() {
            checkSuccessful();
            return unit;
        }

        @NotNull
        public String getErrorText() {
            return errorText;
        }

        public static LineResult successful(Object value, boolean unit) {
            return new LineResult(value, unit, null, LineResultType.SUCCESS);
        }

        public static LineResult error(@NotNull String errorText) {
            if (errorText.isEmpty()) {
                errorText = "<unknown error>";
            }
            else if (!errorText.endsWith("\n")) {
                errorText += "\n";
            }
            return new LineResult(null, false, errorText, LineResultType.ERROR);
        }

        public static LineResult incomplete() {
            return new LineResult(null, false, null, LineResultType.INCOMPLETE);
        }
    }

    @NotNull
    public LineResult eval(@NotNull String line) {
        ++lineNumber;

        FqName scriptFqName = new FqName("Line" + lineNumber);
        Type scriptClassType = asmTypeByFqNameWithoutInnerClasses(scriptFqName);

        StringBuilder fullText = new StringBuilder();
        for (String prevLine : previousIncompleteLines) {
            fullText.append(prevLine).append("\n");
        }
        fullText.append(line);

        LightVirtualFile virtualFile = new LightVirtualFile("line" + lineNumber + JetParserDefinition.STD_SCRIPT_EXT, JetLanguage.INSTANCE, fullText.toString());
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        JetFile psiFile = (JetFile) psiFileFactory.trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
        assert psiFile != null : "Script file not analyzed at line " + lineNumber + ": " + fullText;

        MessageCollectorToString errorCollector = new MessageCollectorToString();

        AnalyzerWithCompilerReport.SyntaxErrorReport syntaxErrorReport =
                AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorCollector);

        if (syntaxErrorReport.isHasErrors() && syntaxErrorReport.isAllErrorsAtEof()) {
            previousIncompleteLines.add(line);
            return LineResult.incomplete();
        }

        previousIncompleteLines.clear();

        if (syntaxErrorReport.isHasErrors()) {
            return LineResult.error(errorCollector.getString());
        }

        prepareForTheNextReplLine(topDownAnalysisContext);
        trace.clearDiagnostics();

        //noinspection ConstantConditions
        psiFile.getScript().putUserData(ScriptHeaderResolver.PRIORITY_KEY, lineNumber);

        ScriptDescriptor scriptDescriptor = doAnalyze(psiFile, errorCollector);
        if (scriptDescriptor == null) {
            return LineResult.error(errorCollector.getString());
        }

        List<Pair<ScriptDescriptor, Type>> earlierScripts = Lists.newArrayList();

        for (EarlierLine earlierLine : earlierLines) {
            earlierScripts.add(Pair.create(earlierLine.getScriptDescriptor(), earlierLine.getClassType()));
        }

        GenerationState state = new GenerationState(psiFile.getProject(), ClassBuilderFactories.BINARIES,
                                                    module, trace.getBindingContext(), Collections.singletonList(psiFile));

        compileScript(psiFile.getScript(), scriptClassType, earlierScripts, state, CompilationErrorHandler.THROW_EXCEPTION);

        for (OutputFile outputFile : state.getFactory().asList()) {
            classLoader.addClass(JvmClassName.byInternalName(outputFile.getRelativePath().replaceFirst("\\.class$", "")), outputFile.asByteArray());
        }

        try {
            Class<?> scriptClass = classLoader.loadClass(scriptFqName.asString());

            Class<?>[] constructorParams = new Class<?>[earlierLines.size()];
            Object[] constructorArgs = new Object[earlierLines.size()];

            for (int i = 0; i < earlierLines.size(); ++i) {
                constructorParams[i] = earlierLines.get(i).getScriptClass();
                constructorArgs[i] = earlierLines.get(i).getScriptInstance();
            }

            Constructor<?> scriptInstanceConstructor = scriptClass.getConstructor(constructorParams);
            Object scriptInstance;
            try {
                scriptInstance = scriptInstanceConstructor.newInstance(constructorArgs);
            }
            catch (Throwable e) {
                return LineResult.error(renderStackTrace(e.getCause()));
            }
            Field rvField = scriptClass.getDeclaredField("rv");
            rvField.setAccessible(true);
            Object rv = rvField.get(scriptInstance);

            earlierLines.add(new EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance, scriptClassType));

            JetType returnType = scriptDescriptor.getScriptCodeDescriptor().getReturnType();
            return LineResult.successful(rv, returnType != null && KotlinBuiltIns.getInstance().isUnit(returnType));
        }
        catch (Throwable e) {
            PrintWriter writer = new PrintWriter(System.err);
            classLoader.dumpClasses(writer);
            writer.flush();
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    private static String renderStackTrace(@NotNull Throwable cause) {
        StackTraceElement[] oldTrace = cause.getStackTrace();
        List<StackTraceElement> newTrace = new ArrayList<StackTraceElement>();
        boolean skip = true;
        for (int i = oldTrace.length - 1; i >= 0; i--) {
            StackTraceElement element = oldTrace[i];
            // All our code happens in the script constructor, and no reflection/native code happens in constructors.
            // So we ignore everything in the stack trace until the first constructor
            if (element.getMethodName().equals("<init>")) {
                skip = false;
            }
            if (!skip) {
                newTrace.add(element);
            }
        }
        Collections.reverse(newTrace);
        cause.setStackTrace(newTrace.toArray(new StackTraceElement[newTrace.size()]));
        return Throwables.getStackTraceAsString(cause);
    }

    @Nullable
    private ScriptDescriptor doAnalyze(@NotNull JetFile psiFile, @NotNull MessageCollector messageCollector) {
        WritableScope scope = new WritableScopeImpl(
                JetScope.EMPTY, module,
                new TraceBasedRedeclarationHandler(trace), "Root scope in analyzePackage"
        );

        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        // Import a scope that contains all top-level packages that come from dependencies
        // This makes the packages visible at all, does not import themselves
        scope.importScope(module.getPackage(FqName.ROOT).getMemberScope());

        if (lastLineScope != null) {
            scope.importScope(lastLineScope);
        }

        scope.changeLockLevel(WritableScope.LockLevel.READING);

        // dummy builder is used because "root" is module descriptor,
        // packages added to module explicitly in
        topDownAnalyzer.doProcess(topDownAnalysisContext, scope, new PackageLikeBuilderDummy(), Collections.singletonList(psiFile));

        boolean hasErrors = AnalyzerWithCompilerReport.reportDiagnostics(trace.getBindingContext().getDiagnostics(), messageCollector);
        if (hasErrors) {
            return null;
        }

        ScriptDescriptor scriptDescriptor = topDownAnalysisContext.getScripts().get(psiFile.getScript());
        lastLineScope = trace.get(BindingContext.SCRIPT_SCOPE, scriptDescriptor);
        if (lastLineScope == null) {
            throw new IllegalStateException("last line scope is not initialized");
        }

        return scriptDescriptor;
    }

    public void dumpClasses(@NotNull PrintWriter out) {
        classLoader.dumpClasses(out);
    }

    private static void registerEarlierScripts(
            @NotNull GenerationState state,
            @NotNull List<Pair<ScriptDescriptor, Type>> earlierScripts
    ) {
        List<ScriptDescriptor> earlierScriptDescriptors = new ArrayList<ScriptDescriptor>(earlierScripts.size());
        for (Pair<ScriptDescriptor, Type> pair : earlierScripts) {
            ScriptDescriptor earlierDescriptor = pair.first;
            Type earlierClassType = pair.second;

            registerClassNameForScript(state.getBindingTrace(), earlierDescriptor, earlierClassType);
            earlierScriptDescriptors.add(earlierDescriptor);
        }
        state.setEarlierScriptsForReplInterpreter(earlierScriptDescriptors);
    }

    public static void compileScript(
            @NotNull JetScript script,
            @NotNull Type classType,
            @NotNull List<Pair<ScriptDescriptor, Type>> earlierScripts,
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        registerEarlierScripts(state, earlierScripts);
        registerClassNameForScript(state.getBindingTrace(), script, classType);

        state.beforeCompile();
        KotlinCodegenFacade.generatePackage(
                state,
                script.getContainingJetFile().getPackageFqName(),
                Collections.singleton(script.getContainingJetFile()),
                errorHandler
        );
    }

}
