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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
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
import org.jetbrains.kotlin.js.facade.K2JSTranslator;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.TranslationResult;
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.kotlin.cli.common.ExitCode.OK;
import static org.jetbrains.kotlin.cli.common.UtilsKt.checkKotlinPackageUsage;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*;

public class K2JSCompiler extends CLICompiler<K2JSCompilerArguments> {
    private static final Map<String, ModuleKind> moduleKindMap = new HashMap<>();

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
        MessageCollector messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        if (arguments.freeArgs.isEmpty()) {
            if (arguments.version) {
                return OK;
            }
            messageCollector.report(ERROR, "Specify at least one source file or directory", null);
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
            messageCollector.report(ERROR, "Specify output file via -output", null);
            return ExitCode.COMPILATION_ERROR;
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR;
        }

        if (sourcesFiles.isEmpty()) {
            messageCollector.report(ERROR, "No source files", null);
            return COMPILATION_ERROR;
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles);
        }

        File outputFile = new File(arguments.outputFile);

        configuration.put(CommonConfigurationKeys.MODULE_NAME, FileUtil.getNameWithoutExtension(outputFile));

        JsConfig config = new JsConfig(project, configuration);
        if (config.checkLibFilesAndReportErrors(new JsConfig.Reporter() {
            @Override
            public void error(@NotNull String message) {
                messageCollector.report(ERROR, message, null);
            }

            @Override
            public void warning(@NotNull String message) {
                messageCollector.report(STRONG_WARNING, message, null);
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
                messageCollector.report(ERROR, "Output prefix file '" + arguments.outputPrefix + "' not found", null);
                return ExitCode.COMPILATION_ERROR;
            }
        }

        File outputPostfixFile = null;
        if (arguments.outputPostfix != null) {
            outputPostfixFile = new File(arguments.outputPostfix);
            if (!outputPostfixFile.exists()) {
                messageCollector.report(ERROR, "Output postfix file '" + arguments.outputPostfix + "' not found", null);
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
            messageCollector.report(ERROR, "Cannot open output file '" + outputFile.getPath() + "': is a directory", null);
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
        Iterable<String> fileNames = CollectionsKt.map(sourceFiles, file -> {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                return FileUtil.toSystemDependentName(virtualFile.getPath());
            }
            return file.getName() + "(no virtual file)";
        });
        messageCollector.report(LOGGING, "Compiling source files: " + StringsKt.join(fileNames, ", "), null);
    }

    private static AnalyzerWithCompilerReport analyzeAndReportErrors(
            @NotNull MessageCollector messageCollector, @NotNull List<KtFile> sources, @NotNull JsConfig config
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

        List<String> libraries = new SmartList<>();
        if (!arguments.noStdlib) {
            libraries.add(0, PathUtil.getKotlinPathsForCompiler().getJsStdLibJarPath().getAbsolutePath());
        }

        if (arguments.libraries != null) {
            ContainerUtil.addAll(libraries, ArraysKt.filterNot(arguments.libraries.split(File.pathSeparator), String::isEmpty));
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries);


        if (arguments.typedArrays) {
            configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true);
        }

        configuration.put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, arguments.friendModulesDisabled);

        if (!arguments.friendModulesDisabled && arguments.friendModules != null) {
            List<String> friendPaths = ArraysKt.filterNot(arguments.friendModules.split(File.pathSeparator), String::isEmpty);
            configuration.put(JSConfigurationKeys.FRIEND_PATHS, friendPaths);
        }

        String moduleKindName = arguments.moduleKind;
        ModuleKind moduleKind = moduleKindName != null ? moduleKindMap.get(moduleKindName) : ModuleKind.PLAIN;
        if (moduleKind == null) {
            messageCollector.report(
                    ERROR, "Unknown module kind: " + moduleKindName + ". Valid values are: plain, amd, commonjs, umd", null
            );
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
