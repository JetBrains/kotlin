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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.search.ProjectScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorToString;
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.codegen.ClassBuilderFactories;
import org.jetbrains.kotlin.codegen.CompilationErrorHandler;
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContextImpl;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.di.InjectorForReplWithJava;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.parsing.JetParserDefinition;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider;
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.*;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.utils.UtilsPackage;
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

import static org.jetbrains.kotlin.codegen.AsmUtil.asmTypeByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.registerClassNameForScript;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;

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
    private final LazyTopDownAnalyzerForTopLevel topDownAnalyzer;
    private final ResolveSession resolveSession;
    private final ScriptMutableDeclarationProviderFactory scriptDeclarationFactory;

    public ReplInterpreter(@NotNull Disposable disposable, @NotNull CompilerConfiguration configuration) {
        JetCoreEnvironment environment =
                JetCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        Project project = environment.getProject();
        this.psiFileFactory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        this.trace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();
        this.module = TopDownAnalyzerFacadeForJVM.createJavaModule("<repl>");

        GlobalContextImpl context = ContextPackage.GlobalContext();

        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                context.getStorageManager(),
                context.getExceptionTracker(),
                false,
                true
        );

        scriptDeclarationFactory = new ScriptMutableDeclarationProviderFactory();

        ScopeProvider.AdditionalFileScopeProvider scopeProvider = new ScopeProvider.AdditionalFileScopeProvider() {
            @NotNull
            @Override
            public List<JetScope> scopes(@NotNull JetFile file) {
                return lastLineScope != null ? new SmartList<JetScope>(lastLineScope) : Collections.<JetScope>emptyList();
            }
        };

        InjectorForReplWithJava injector = new InjectorForReplWithJava(
                project,
                topDownAnalysisParameters,
                trace,
                module,
                scriptDeclarationFactory,
                ProjectScope.getAllScope(project),
                scopeProvider
        );

        this.topDownAnalysisContext = new TopDownAnalysisContext(topDownAnalysisParameters, DataFlowInfo.EMPTY);
        this.topDownAnalyzer = injector.getLazyTopDownAnalyzerForTopLevel();
        this.resolveSession = injector.getResolveSession();

        module.initialize(new CompositePackageFragmentProvider(
                Arrays.asList(
                        injector.getResolveSession().getPackageFragmentProvider(),
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
        psiFile.getScript().putUserData(ScriptPriorities.PRIORITY_KEY, lineNumber);

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
            return LineResult.successful(rv, returnType != null && KotlinBuiltIns.isUnit(returnType));
        }
        catch (Throwable e) {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
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
        scriptDeclarationFactory.setDelegateFactory(
                new FileBasedDeclarationProviderFactory(topDownAnalysisContext.getStorageManager(), Collections.singletonList(psiFile)));

        TopDownAnalysisContext context = topDownAnalyzer.analyzeDeclarations(
                topDownAnalysisContext.getTopDownAnalysisParameters(),
                Collections.singletonList(psiFile)
        );

        if (trace.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile) == null) {
            trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, psiFile, resolveSession.getPackageFragment(FqName.ROOT));
        }

        boolean hasErrors = AnalyzerWithCompilerReport.reportDiagnostics(trace.getBindingContext().getDiagnostics(), messageCollector);
        if (hasErrors) {
            return null;
        }

        ScriptDescriptor scriptDescriptor = context.getScripts().get(psiFile.getScript());
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

            PsiElement jetScript = descriptorToDeclaration(earlierDescriptor);
            if (jetScript != null) {
                registerClassNameForScript(state.getBindingTrace(), (JetScript) jetScript, earlierClassType);
                earlierScriptDescriptors.add(earlierDescriptor);
            }
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
        public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull JetClassLikeInfo classLikeInfo) {
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
