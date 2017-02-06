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

package org.jetbrains.kotlin.cli.js;

import com.google.common.base.Joiner;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.facade.K2JSTranslator;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.TranslationResult;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.kotlin.cli.common.ExitCode.OK;
import static org.jetbrains.kotlin.cli.common.UtilsKt.checkKotlinPackageUsage;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;

public class K2JSCompiler extends CLICompiler<K2JSCompilerArguments> {
    private static final Map<String, ModuleKind> moduleKindMap = new HashMap<String, ModuleKind>();

    static {
        moduleKindMap.put(K2JsArgumentConstants.MODULE_PLAIN, ModuleKind.PLAIN);
        moduleKindMap.put(K2JsArgumentConstants.MODULE_COMMONJS, ModuleKind.COMMON_JS);
        moduleKindMap.put(K2JsArgumentConstants.MODULE_AMD, ModuleKind.AMD);
        moduleKindMap.put(K2JsArgumentConstants.MODULE_UMD, ModuleKind.UMD);
    }

    public static void main(String... args) {
        doMain(new K2JSCompiler(), args);
    }

    @NotNull
    @Override
    protected K2JSCompilerArguments createArguments() {
        return new K2JSCompilerArguments();
    }

    @NotNull
    @Override
    protected ExitCode doExecute(
            @NotNull K2JSCompilerArguments arguments, @NotNull CompilerConfiguration configuration, @NotNull Disposable rootDisposable
    ) {
        final MessageCollector messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        if (arguments.freeArgs.isEmpty()) {
            if (arguments.version) {
                return OK;
            }
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify at least one source file or directory", NO_LOCATION);
            return COMPILATION_ERROR;
        }

        ContentRootsKt.addKotlinSourceRoots(configuration, arguments.freeArgs);
        KotlinCoreEnvironment environmentForJS =
                KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES);

        Project project = environmentForJS.getProject();
        List<KtFile> sourcesFiles = environmentForJS.getSourceFiles();

        environmentForJS.getConfiguration().put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage);

        if (!checkKotlinPackageUsage(environmentForJS, sourcesFiles)) return ExitCode.COMPILATION_ERROR;

        if (arguments.outputFile == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output file via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.COMPILATION_ERROR;
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR;
        }

        if (sourcesFiles.isEmpty()) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "No source files", CompilerMessageLocation.NO_LOCATION);
            return COMPILATION_ERROR;
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles);
        }

        File outputFile = new File(arguments.outputFile);

        configuration.put(CommonConfigurationKeys.MODULE_NAME, FileUtil.getNameWithoutExtension(outputFile));

        JsConfig config = new LibrarySourcesConfig(project, configuration);
        if (config.checkLibFilesAndReportErrors(new JsConfig.Reporter() {
            @Override
            public void error(@NotNull String message) {
                messageCollector.report(CompilerMessageSeverity.ERROR, message, CompilerMessageLocation.NO_LOCATION);
            }

            @Override
            public void warning(@NotNull String message) {
                messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, message, CompilerMessageLocation.NO_LOCATION);
            }
        })) {
            return COMPILATION_ERROR;
        }

        AnalyzerWithCompilerReport analyzerWithCompilerReport = analyzeAndReportErrors(messageCollector, sourcesFiles, config);
        if (analyzerWithCompilerReport.hasErrors()) {
            return COMPILATION_ERROR;
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        AnalysisResult analysisResult = analyzerWithCompilerReport.getAnalysisResult();
        assert analysisResult instanceof JsAnalysisResult : "analysisResult should be instance of JsAnalysisResult, but " + analysisResult;
        JsAnalysisResult jsAnalysisResult = (JsAnalysisResult) analysisResult;

        File outputPrefixFile = null;
        if (arguments.outputPrefix != null) {
            outputPrefixFile = new File(arguments.outputPrefix);
            if (!outputPrefixFile.exists()) {
                messageCollector.report(CompilerMessageSeverity.ERROR,
                                        "Output prefix file '" + arguments.outputPrefix + "' not found",
                                        CompilerMessageLocation.NO_LOCATION);
                return ExitCode.COMPILATION_ERROR;
            }
        }

        File outputPostfixFile = null;
        if (arguments.outputPostfix != null) {
            outputPostfixFile = new File(arguments.outputPostfix);
            if (!outputPostfixFile.exists()) {
                messageCollector.report(CompilerMessageSeverity.ERROR,
                                        "Output postfix file '" + arguments.outputPostfix + "' not found",
                                        CompilerMessageLocation.NO_LOCATION);
                return ExitCode.COMPILATION_ERROR;
            }
        }

        MainCallParameters mainCallParameters = createMainCallParameters(arguments.main);
        TranslationResult translationResult;

        K2JSTranslator translator = new K2JSTranslator(config);
        try {
            //noinspection unchecked
            translationResult = translator.translate(sourcesFiles, mainCallParameters, jsAnalysisResult);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        AnalyzerWithCompilerReport.Companion.reportDiagnostics(translationResult.getDiagnostics(), messageCollector);

        if (!(translationResult instanceof TranslationResult.Success)) return ExitCode.COMPILATION_ERROR;

        TranslationResult.Success successResult = (TranslationResult.Success) translationResult;
        OutputFileCollection outputFiles = successResult.getOutputFiles(outputFile, outputPrefixFile, outputPostfixFile);

        if (outputFile.isDirectory()) {
            messageCollector.report(CompilerMessageSeverity.ERROR,
                                    "Cannot open output file '" + outputFile.getPath() + "': is a directory",
                                    CompilerMessageLocation.NO_LOCATION);
            return ExitCode.COMPILATION_ERROR;
        }

        File outputDir = outputFile.getParentFile();
        if (outputDir == null) {
            outputDir = outputFile.getAbsoluteFile().getParentFile();
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        OutputUtilsKt.writeAll(outputFiles, outputDir, messageCollector);

        return OK;
    }

    private static void reportCompiledSourcesList(@NotNull MessageCollector messageCollector, @NotNull List<KtFile> sourceFiles) {
        Iterable<String> fileNames = ContainerUtil.map(sourceFiles, new Function<KtFile, String>() {
            @Override
            public String fun(@Nullable KtFile file) {
                assert file != null;
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile != null) {
                    return FileUtil.toSystemDependentName(virtualFile.getPath());
                }
                return file.getName() + "(no virtual file)";
            }
        });
        messageCollector.report(CompilerMessageSeverity.LOGGING, "Compiling source files: " + Joiner.on(", ").join(fileNames),
                                CompilerMessageLocation.NO_LOCATION);
    }

    private static AnalyzerWithCompilerReport analyzeAndReportErrors(
            @NotNull MessageCollector messageCollector, @NotNull final List<KtFile> sources, @NotNull final JsConfig config
    ) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(messageCollector);
        analyzerWithCompilerReport.analyzeAndReport(sources, new AnalyzerWithCompilerReport.Analyzer() {
            @NotNull
            @Override
            public AnalysisResult analyze() {
                return TopDownAnalyzerFacadeForJS.analyzeFiles(sources, config);
            }

            @Override
            public void reportEnvironmentErrors() {
            }
        });
        return analyzerWithCompilerReport;
    }

    @Override
    protected void setupPlatformSpecificArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull K2JSCompilerArguments arguments,
            @NotNull Services services
    ) {
        MessageCollector messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        if (arguments.target != null) {
            assert arguments.target == "v5" : "Unsupported ECMA version: " + arguments.target;
        }
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.defaultVersion());

        if (arguments.sourceMap) {
            configuration.put(JSConfigurationKeys.SOURCE_MAP, true);
        }
        if (arguments.metaInfo) {
            configuration.put(JSConfigurationKeys.META_INFO, true);
        }

        List<String> libraries = new SmartList<String>();
        if (!arguments.noStdlib) {
            libraries.add(0, PathUtil.getKotlinPathsForCompiler().getJsStdLibJarPath().getAbsolutePath());
        }

        if (arguments.libraries != null) {
            ContainerUtil.addAllNotNull(libraries, arguments.libraries.split(File.pathSeparator));
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries);

        String moduleKindName = arguments.moduleKind;
        ModuleKind moduleKind = moduleKindName != null ? moduleKindMap.get(moduleKindName) : ModuleKind.PLAIN;
        if (moduleKind == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Unknown module kind: " + moduleKindName + ". " +
                                                                   "Valid values are: plain, amd, commonjs, umd",
                                    CompilerMessageLocation.NO_LOCATION);
        }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind);
    }

    private static MainCallParameters createMainCallParameters(String main) {
        if (K2JsArgumentConstants.NO_CALL.equals(main)) {
            return MainCallParameters.noCall();
        }
        else {
            return MainCallParameters.mainWithoutArguments();
        }
    }
}
