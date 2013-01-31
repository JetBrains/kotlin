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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.PrintingMessageCollector;
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
    protected ExitCode doExecute(K2JSCompilerArguments arguments, PrintingMessageCollector messageCollector, Disposable rootDisposable) {
        if (arguments.sourceFiles == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify sources location via -sourceFiles", NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, Arrays.asList(arguments.sourceFiles));
        JetCoreEnvironment environmentForJS = new JetCoreEnvironment(rootDisposable, configuration);

        Project project = environmentForJS.getProject();

        ClassPathLibrarySourcesLoader sourceLoader = new ClassPathLibrarySourcesLoader(project);
        List<JetFile> sourceFiles = sourceLoader.findSourceFiles();
        environmentForJS.getSourceFiles().addAll(sourceFiles);

        if (arguments.isVerbose()) {
            reportCompiledSourcesList(messageCollector, environmentForJS);
        }

        Config config = getConfig(arguments, project);
        if (analyzeAndReportErrors(messageCollector, environmentForJS.getSourceFiles(), config)) {
            return COMPILATION_ERROR;
        }

        String outputFile = arguments.outputFile;
        if (outputFile == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output file via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        MainCallParameters mainCallParameters = arguments.createMainCallParameters();
        return translateAndGenerateOutputFile(mainCallParameters, messageCollector, environmentForJS, config, outputFile);
    }

    private static void reportCompiledSourcesList(@NotNull PrintingMessageCollector messageCollector,
            @NotNull JetCoreEnvironment environmentForJS) {
        List<JetFile> files = environmentForJS.getSourceFiles();
        Iterable<String> fileNames = Iterables.transform(files, new Function<JetFile, String>() {
            @Override
            public String apply(@Nullable JetFile file) {
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

    @NotNull
    private static ExitCode translateAndGenerateOutputFile(@NotNull MainCallParameters mainCall,
            @NotNull PrintingMessageCollector messageCollector,
            @NotNull JetCoreEnvironment environmentForJS, @NotNull Config config, @NotNull String outputFile) {
        try {
            K2JSTranslator.translateWithMainCallParametersAndSaveToFile(mainCall, environmentForJS.getSourceFiles(), outputFile, config);
        }
        catch (Exception e) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Exception while translating:\n" + e.getMessage(),
                                    CompilerMessageLocation.NO_LOCATION);
            // TODO we should report the exception nicely to the collector so it can report
            // for example inside a mvn plugin we need to see the stack trace
            return ExitCode.INTERNAL_ERROR;
        }
        return messageCollector.anyReported(CompilerMessageSeverity.ERROR) ? COMPILATION_ERROR : OK;
    }

    private static boolean analyzeAndReportErrors(@NotNull PrintingMessageCollector messageCollector,
            @NotNull final List<JetFile> sources, @NotNull final Config config) {
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(messageCollector);
        analyzerWithCompilerReport.analyzeAndReport(new Function0<AnalyzeExhaust>() {
            @Override
            public AnalyzeExhaust invoke() {
                return AnalyzerFacadeForJS.analyzeFiles(sources, Predicates.<PsiFile>alwaysTrue(), config);
            }
        }, sources);
        return analyzerWithCompilerReport.hasErrors();
    }

    @NotNull
    private static Config getConfig(@NotNull K2JSCompilerArguments arguments, @NotNull Project project) {
        EcmaVersion ecmaVersion = EcmaVersion.fromString(arguments.target);
        String moduleId = FileUtil.getNameWithoutExtension(new File(arguments.outputFile));
        if (arguments.libraryFiles != null) {
            return new LibrarySourcesConfig(project, moduleId, Arrays.asList(arguments.libraryFiles), ecmaVersion);
        }
        else {
            // lets discover the JS library definitions on the classpath
            return new ClassPathLibraryDefintionsConfig(project, moduleId, ecmaVersion);
        }
    }
}
