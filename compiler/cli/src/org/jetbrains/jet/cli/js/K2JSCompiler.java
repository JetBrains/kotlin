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

package org.jetbrains.jet.cli.js;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JsArgumentConstants;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.*;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.facade.MainCallParameters;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.jet.cli.common.ExitCode.OK;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;

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
            @NotNull MessageCollector messageCollector,
            @NotNull Disposable rootDisposable
    ) {
        if (arguments.sourceFiles == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify sources location via -sourceFiles", NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, Arrays.asList(arguments.sourceFiles));
        JetCoreEnvironment environmentForJS = JetCoreEnvironment.createForProduction(rootDisposable, configuration);

        Project project = environmentForJS.getProject();
        List<JetFile> sourcesFiles = environmentForJS.getSourceFiles();

        ClassPathLibrarySourcesLoader sourceLoader = new ClassPathLibrarySourcesLoader(project);
        List<JetFile> additionalSourceFiles = sourceLoader.findSourceFiles();
        sourcesFiles.addAll(additionalSourceFiles);

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles);
        }

        if (arguments.outputFile == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output file via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        File outputFile = new File(arguments.outputFile);

        Config config = getConfig(arguments, project);
        if (analyzeAndReportErrors(messageCollector, sourcesFiles, config)) {
            return COMPILATION_ERROR;
        }

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

        OutputFileCollection outputFiles = translate(mainCallParameters, config, sourcesFiles, outputFile, outputPrefixFile, outputPostfixFile);

        OutputUtilsPackage.writeAll(outputFiles, outputFile.getParentFile(), messageCollector);

        return OK;
    }

    private static void reportCompiledSourcesList(@NotNull MessageCollector messageCollector, @NotNull List<JetFile> sourceFiles) {
        Iterable<String> fileNames = ContainerUtil.map(sourceFiles, new Function<JetFile, String>() {
            @Override
            public String fun(@Nullable JetFile file) {
                assert file != null;
                VirtualFile virtualFile = file.getVirtualFile();
                if (virtualFile != null) {
                    return FileUtil.toSystemIndependentName(virtualFile.getPath());
                }
                return file.getName() + "(no virtual file)";
            }
        });
        messageCollector.report(CompilerMessageSeverity.LOGGING, "Compiling source files: " + Joiner.on(", ").join(fileNames),
                                CompilerMessageLocation.NO_LOCATION);
    }

    private static OutputFileCollection translate(
            @NotNull MainCallParameters mainCall,
            @NotNull Config config,
            @NotNull List<JetFile> sourceFiles,
            @NotNull File outputFile,
            @Nullable File outputPrefix,
            @Nullable File outputPostfix
    ) {
        try {
            return K2JSTranslator.translateWithMainCallParameters(mainCall, sourceFiles, outputFile, outputPrefix, outputPostfix, config);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean analyzeAndReportErrors(@NotNull MessageCollector messageCollector,
            @NotNull final List<JetFile> sources, @NotNull final Config config) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(messageCollector);
        analyzerWithCompilerReport.analyzeAndReport(sources, new Function0<AnalyzeExhaust>() {
            @Override
            public AnalyzeExhaust invoke() {
                return AnalyzerFacadeForJS.analyzeFiles(sources, Predicates.<PsiFile>alwaysTrue(), config);
            }
        });
        return analyzerWithCompilerReport.hasErrors();
    }

    @NotNull
    private static Config getConfig(@NotNull K2JSCompilerArguments arguments, @NotNull Project project) {
        if (arguments.target != null) {
            assert arguments.target == "v5" : "Unsupported ECMA version: " + arguments.target;
        }
        EcmaVersion ecmaVersion = EcmaVersion.defaultVersion();
        String moduleId = FileUtil.getNameWithoutExtension(new File(arguments.outputFile));
        if (arguments.libraryFiles != null) {
            return new LibrarySourcesConfig(project, moduleId, Arrays.asList(arguments.libraryFiles), ecmaVersion, arguments.sourcemap);
        }
        else {
            // lets discover the JS library definitions on the classpath
            return new ClassPathLibraryDefintionsConfig(project, moduleId, ecmaVersion, arguments.sourcemap);
        }
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