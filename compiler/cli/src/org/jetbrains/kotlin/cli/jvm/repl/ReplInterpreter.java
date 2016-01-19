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

package org.jetbrains.kotlin.cli.jvm.repl;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.search.ProjectScope;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.DiagnosticMessageReporter;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.cli.jvm.config.ModuleNameKt;
import org.jetbrains.kotlin.cli.jvm.repl.di.ContainerForReplWithJava;
import org.jetbrains.kotlin.cli.jvm.repl.di.InjectionKt;
import org.jetbrains.kotlin.cli.jvm.repl.di.ReplLastLineScopeProvider;
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder;
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplIdeDiagnosticMessageHolder;
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplSystemInWrapper;
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplTerminalDiagnosticMessageHolder;
import org.jetbrains.kotlin.codegen.ClassBuilderFactories;
import org.jetbrains.kotlin.codegen.CompilationErrorHandler;
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.parsing.KotlinParserDefinition;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtScript;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.*;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.script.*;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

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

public class ReplInterpreter {
    private static final String SCRIPT_RESULT_FIELD_NAME = "$$result";

    private int lineNumber = 0;

    @Nullable
    private LexicalScope lastLineScope;
    private final List<EarlierLine> earlierLines = Lists.newArrayList();
    private final List<String> previousIncompleteLines = Lists.newArrayList();
    private final ReplClassLoader classLoader;

    private final PsiFileFactoryImpl psiFileFactory;
    private final BindingTraceContext trace;
    private final ModuleDescriptorImpl module;

    private final TopDownAnalysisContext topDownAnalysisContext;
    private final LazyTopDownAnalyzerForTopLevel topDownAnalyzer;
    private final ResolveSession resolveSession;
    private final ScriptMutableDeclarationProviderFactory scriptDeclarationFactory;

    private final boolean ideMode;
    private final ReplSystemInWrapper replReader;
    private final static KotlinScriptDefinition REPL_LINE_AS_SCRIPT_DEFINITION = new KotlinScriptDefinition() {
        @NotNull
        @Override
        public List<ScriptParameter> getScriptParameters(@NotNull ScriptDescriptor scriptDescriptor) {
            return Collections.emptyList();
        }

        @Override
        public boolean isScript(@NotNull PsiFile file) {
            return StandardScriptDefinition.INSTANCE.isScript(file);
        }

        @NotNull
        @Override
        public Name getScriptName(@NotNull KtScript script) {
            return StandardScriptDefinition.INSTANCE.getScriptName(script);
        }
    };

    public ReplInterpreter(
            @NotNull Disposable disposable,
            @NotNull CompilerConfiguration configuration,
            boolean ideMode,
            @Nullable ReplSystemInWrapper replReader
    ) {
        configuration.add(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, REPL_LINE_AS_SCRIPT_DEFINITION);

        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        Project project = environment.getProject();

        this.psiFileFactory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        this.trace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();
        MutableModuleContext moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, ModuleNameKt
                .getModuleName(environment));
        this.module = moduleContext.getModule();

        scriptDeclarationFactory = new ScriptMutableDeclarationProviderFactory();

        ContainerForReplWithJava container = InjectionKt.createContainerForReplWithJava(
                moduleContext,
                trace,
                scriptDeclarationFactory,
                ProjectScope.getAllScope(project),
                new ReplLastLineScopeProvider() {
                    @Nullable
                    @Override
                    public LexicalScope getLastLineScope() {
                        return lastLineScope;
                    }
                },
                new JvmPackagePartProvider(environment)
        );

        this.topDownAnalysisContext = new TopDownAnalysisContext(TopDownAnalysisMode.LocalDeclarations, DataFlowInfoFactory.EMPTY,
                                                                 container.getResolveSession().getDeclarationScopeProvider());
        this.topDownAnalyzer = container.getLazyTopDownAnalyzerForTopLevel();
        this.resolveSession = container.getResolveSession();

        moduleContext.initializeModuleContents(new CompositePackageFragmentProvider(
                Arrays.asList(
                        container.getResolveSession().getPackageFragmentProvider(),
                        container.getJavaDescriptorResolver().getPackageFragmentProvider()
                )
        ));

        List<URL> classpath = Lists.newArrayList();
        for (File file : JvmContentRootsKt.getJvmClasspathRoots(configuration)) {
            try {
                classpath.add(file.toURI().toURL());
            }
            catch (MalformedURLException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }
        }

        this.classLoader = new ReplClassLoader(new URLClassLoader(classpath.toArray(new URL[classpath.size()]), null));

        this.ideMode = ideMode;
        this.replReader = replReader;
    }

    private static void prepareForTheNextReplLine(@NotNull TopDownAnalysisContext c) {
        c.getScripts().clear();
    }

    public enum LineResultType {
        SUCCESS,
        COMPILE_ERROR,
        RUNTIME_ERROR,
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

        @NotNull
        private static LineResult error(@NotNull String errorText, @NotNull LineResultType errorType) {
            if (errorText.isEmpty()) {
                errorText = "<unknown error>";
            }
            else if (!errorText.endsWith("\n")) {
                errorText += "\n";
            }

            return new LineResult(null, false, errorText, errorType);
        }

        @NotNull
        public static LineResult successful(Object value, boolean unit) {
            return new LineResult(value, unit, null, LineResultType.SUCCESS);
        }

        @NotNull
        public static LineResult compileError(@NotNull String errorText) {
            return error(errorText, LineResultType.COMPILE_ERROR);
        }

        @NotNull
        public static LineResult runtimeError(@NotNull String errorText) {
            return error(errorText, LineResultType.RUNTIME_ERROR);
        }

        public static LineResult incomplete() {
            return new LineResult(null, false, null, LineResultType.INCOMPLETE);
        }
    }

    @NotNull
    private DiagnosticMessageHolder createDiagnosticHolder() {
        return ideMode ? new ReplIdeDiagnosticMessageHolder()
                       : new ReplTerminalDiagnosticMessageHolder();
    }

    @NotNull
    public LineResult eval(@NotNull String line) {
        ++lineNumber;

        FqName scriptFqName = new FqName("Line" + lineNumber);

        StringBuilder fullText = new StringBuilder();
        for (String prevLine : previousIncompleteLines) {
            fullText.append(prevLine).append("\n");
        }
        fullText.append(line);

        LightVirtualFile virtualFile = new LightVirtualFile("line" + lineNumber + KotlinParserDefinition.STD_SCRIPT_EXT, KotlinLanguage.INSTANCE, fullText.toString());
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        KtFile psiFile = (KtFile) psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false);
        assert psiFile != null : "Script file not analyzed at line " + lineNumber + ": " + fullText;

        DiagnosticMessageHolder errorHolder = createDiagnosticHolder();

        AnalyzerWithCompilerReport.SyntaxErrorReport syntaxErrorReport = AnalyzerWithCompilerReport.Companion
                .reportSyntaxErrors(psiFile, errorHolder);

        if (syntaxErrorReport.isHasErrors() && syntaxErrorReport.isAllErrorsAtEof()) {
            if (ideMode) {
                return LineResult.compileError(errorHolder.getRenderedDiagnostics());
            }
            else {
                previousIncompleteLines.add(line);
                return LineResult.incomplete();
            }
        }

        previousIncompleteLines.clear();

        if (syntaxErrorReport.isHasErrors()) {
            return LineResult.compileError(errorHolder.getRenderedDiagnostics());
        }

        prepareForTheNextReplLine(topDownAnalysisContext);
        trace.clearDiagnostics();

        //noinspection ConstantConditions
        psiFile.getScript().putUserData(ScriptPriorities.PRIORITY_KEY, lineNumber);

        ScriptDescriptor scriptDescriptor = doAnalyze(psiFile, errorHolder);
        if (scriptDescriptor == null) {
            return LineResult.compileError(errorHolder.getRenderedDiagnostics());
        }

        List<ScriptDescriptor> earlierScripts = Lists.newArrayList();

        for (EarlierLine earlierLine : earlierLines) {
            earlierScripts.add(earlierLine.getScriptDescriptor());
        }

        GenerationState state = new GenerationState(psiFile.getProject(), ClassBuilderFactories.BINARIES,
                                                    module, trace.getBindingContext(), Collections.singletonList(psiFile));

        compileScript(psiFile.getScript(), earlierScripts, state, CompilationErrorHandler.THROW_EXCEPTION);

        for (OutputFile outputFile : state.getFactory().asList()) {
            if(outputFile.getRelativePath().endsWith(".class")) {
                classLoader.addClass(JvmClassName.byInternalName(outputFile.getRelativePath().replaceFirst("\\.class$", "")),
                                     outputFile.asByteArray());
            }
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
                setReplScriptExecuting(true);
                scriptInstance = scriptInstanceConstructor.newInstance(constructorArgs);
            }
            catch (Throwable e) {
                return LineResult.runtimeError(renderStackTrace(e.getCause()));
            } finally {
                setReplScriptExecuting(false);
            }

            Field rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME);
            rvField.setAccessible(true);
            Object rv = rvField.get(scriptInstance);

            earlierLines.add(new EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance));

            return LineResult.successful(rv, !state.getReplSpecific().getHasResult());
        }
        catch (Throwable e) {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            PrintWriter writer = new PrintWriter(System.err);
            classLoader.dumpClasses(writer);
            writer.flush();
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    private void setReplScriptExecuting(boolean isExecuting) {
        if (replReader != null) {
            replReader.setReplScriptExecuting(isExecuting);
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

        // throw away last element which contains Line1.kts<init>(Unknown source)
        List<StackTraceElement> resultingTrace = newTrace.subList(0, newTrace.size() - 1);

        cause.setStackTrace(resultingTrace.toArray(new StackTraceElement[resultingTrace.size()]));
        return Throwables.getStackTraceAsString(cause);
    }

    @Nullable
    private ScriptDescriptor doAnalyze(@NotNull KtFile psiFile, @NotNull DiagnosticMessageReporter errorReporter) {
        scriptDeclarationFactory.setDelegateFactory(
                new FileBasedDeclarationProviderFactory(resolveSession.getStorageManager(), Collections.singletonList(psiFile)));

        TopDownAnalysisContext context = topDownAnalyzer.analyzeDeclarations(
                topDownAnalysisContext.getTopDownAnalysisMode(),
                Collections.singletonList(psiFile)
        );

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile, resolveSession.getPackageFragment(FqName.ROOT));
        }

        boolean hasErrors = AnalyzerWithCompilerReport.Companion
                .reportDiagnostics(trace.getBindingContext().getDiagnostics(), errorReporter, false);
        if (hasErrors) {
            return null;
        }

        LazyScriptDescriptor scriptDescriptor = context.getScripts().get(psiFile.getScript());
        lastLineScope = scriptDescriptor.getScopeForInitializerResolution();
        return scriptDescriptor;
    }

    public void dumpClasses(@NotNull PrintWriter out) {
        classLoader.dumpClasses(out);
    }

    public static void compileScript(
            @NotNull KtScript script,
            @NotNull List<ScriptDescriptor> earlierScripts,
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        state.getReplSpecific().setScriptResultFieldName(SCRIPT_RESULT_FIELD_NAME);
        state.getReplSpecific().setEarlierScriptsForReplInterpreter(new ArrayList<ScriptDescriptor>(earlierScripts));

        state.beforeCompile();
        KotlinCodegenFacade.generatePackage(
                state,
                script.getContainingKtFile().getPackageFqName(),
                Collections.singleton(script.getContainingKtFile()),
                errorHandler
        );
    }

    private static class ScriptMutableDeclarationProviderFactory implements DeclarationProviderFactory {
        private DeclarationProviderFactory delegateFactory;
        private AdaptablePackageMemberDeclarationProvider rootPackageProvider;

        public void setDelegateFactory(DeclarationProviderFactory delegateFactory) {
            this.delegateFactory = delegateFactory;

            PackageMemberDeclarationProvider provider = delegateFactory.getPackageMemberDeclarationProvider(FqName.ROOT);
            if (rootPackageProvider == null) {
                assert provider != null;
                rootPackageProvider = new AdaptablePackageMemberDeclarationProvider(provider);
            }
            else {
                rootPackageProvider.addDelegateProvider(provider);
            }
        }

        @NotNull
        @Override
        public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull KtClassLikeInfo classLikeInfo) {
            return delegateFactory.getClassMemberDeclarationProvider(classLikeInfo);
        }

        @Nullable
        @Override
        public PackageMemberDeclarationProvider getPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
            if (packageFqName.isRoot()) {
                return rootPackageProvider;
            }

            return this.delegateFactory.getPackageMemberDeclarationProvider(packageFqName);
        }

        @Override
        public void diagnoseMissingPackageFragment(KtFile file) {
            this.delegateFactory.diagnoseMissingPackageFragment(file);
        }

        public static class AdaptablePackageMemberDeclarationProvider extends DelegatePackageMemberDeclarationProvider {
            @NotNull
            private PackageMemberDeclarationProvider delegateProvider;

            public AdaptablePackageMemberDeclarationProvider(@NotNull PackageMemberDeclarationProvider delegateProvider) {
                super(delegateProvider);
                this.delegateProvider = delegateProvider;
            }

            public void addDelegateProvider(PackageMemberDeclarationProvider provider) {
                delegateProvider = new CombinedPackageMemberDeclarationProvider(Lists.newArrayList(provider, delegateProvider));

                setDelegate(delegateProvider);
            }
        }
    }
}
