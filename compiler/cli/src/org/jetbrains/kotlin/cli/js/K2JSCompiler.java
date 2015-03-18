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
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.serializer.KotlinJavaScriptSerializer;
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
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.kotlin.cli.jvm.compiler.CompilerJarLocator;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.facade.K2JSTranslator;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.TranslationResult;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.List;

import static org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.kotlin.cli.common.ExitCode.OK;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;

public class K2JSCompiler extends CLICompiler<K2JSCompilerArguments> {

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
            @NotNull K2JSCompilerArguments arguments,
            @NotNull Services services,
            @NotNull final MessageCollector messageCollector,
            @NotNull Disposable rootDisposable
    ) {
        if (arguments.freeArgs.isEmpty()) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify at least one source file or directory", NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

        CompilerJarLocator locator = services.get(CompilerJarLocator.class);
        if (locator != null) {
            configuration.put(JVMConfigurationKeys.COMPILER_JAR_LOCATOR, locator);
        }

        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, arguments.freeArgs);
        JetCoreEnvironment environmentForJS =
                JetCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES);

        Project project = environmentForJS.getProject();
        List<JetFile> sourcesFiles = environmentForJS.getSourceFiles();

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles);
        }

        if (arguments.outputFile == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output file via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        File outputFile = new File(arguments.outputFile);

        Config config = getConfig(arguments, project);
        if (config.checkLibFilesAndReportErrors(new Function1<String, Unit>() {
            @Override
            public Unit invoke(String message) {
                messageCollector.report(CompilerMessageSeverity.ERROR, message, CompilerMessageLocation.NO_LOCATION);
                return Unit.INSTANCE$;
            }
        })) {
            return COMPILATION_ERROR;
        }

        AnalyzerWithCompilerReport analyzerWithCompilerReport = analyzeAndReportErrors(messageCollector, sourcesFiles, config);
        if (analyzerWithCompilerReport.hasErrors()) {
            return COMPILATION_ERROR;
        }

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        AnalyzerWithCompilerReport.reportDiagnostics(translationResult.getDiagnostics(), messageCollector);

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
        OutputUtilsPackage.writeAll(outputFiles, outputDir, messageCollector);

        if (arguments.metaInfo != null) {
            new KotlinJavaScriptSerializer().serialize(config.getModuleId(), analysisResult.getModuleDescriptor(), new File(arguments.metaInfo));
        }

        return OK;
    }

    private static void reportCompiledSourcesList(@NotNull MessageCollector messageCollector, @NotNull List<JetFile> sourceFiles) {
        Iterable<String> fileNames = ContainerUtil.map(sourceFiles, new Function<JetFile, String>() {
            @Override
            public String fun(@Nullable JetFile file) {
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

    private static AnalyzerWithCompilerReport analyzeAndReportErrors(@NotNull MessageCollector messageCollector,
            @NotNull final List<JetFile> sources, @NotNull final Config config) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(messageCollector);
        analyzerWithCompilerReport.analyzeAndReport(sources, new Function0<AnalysisResult>() {
            @Override
            public AnalysisResult invoke() {
                return TopDownAnalyzerFacadeForJS.analyzeFiles(sources, config);
            }
        });
        return analyzerWithCompilerReport;
    }

    @NotNull
    private static Config getConfig(@NotNull K2JSCompilerArguments arguments, @NotNull Project project) {
        if (arguments.target != null) {
            assert arguments.target == "v5" : "Unsupported ECMA version: " + arguments.target;
        }
        EcmaVersion ecmaVersion = EcmaVersion.defaultVersion();
        String moduleId = FileUtil.getNameWithoutExtension(new File(arguments.outputFile));
        boolean inlineEnabled = !arguments.noInline;

        List<String> libraryFiles = new SmartList<String>();
        if (!arguments.noStdlib) {
            libraryFiles.add(0, PathUtil.getKotlinPathsForCompiler().getJsStdLibJarPath().getAbsolutePath());
        }

        if (arguments.libraryFiles != null) {
            ContainerUtil.addAllNotNull(libraryFiles, arguments.libraryFiles);
        }

        return new LibrarySourcesConfig(project, moduleId, libraryFiles, ecmaVersion, arguments.sourceMap, inlineEnabled);
    }

    public static MainCallParameters createMainCallParameters(String main) {
        if (K2JsArgumentConstants.NO_CALL.equals(main)) {
            return MainCallParameters.noCall();
        }
        else {
            return MainCallParameters.mainWithoutArguments();
        }
    }
}
