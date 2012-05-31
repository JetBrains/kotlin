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

package org.jetbrains.jet.cli.js;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.*;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.facade.MainCallParameters;

import java.util.List;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;

/**
 * @author Pavel Talanov
 */
public class K2JSCompiler extends CLICompiler<K2JSCompilerArguments, K2JSCompileEnvironmentConfiguration> {

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

        if (arguments.srcdir == null && arguments.sourceFiles == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify sources location via -srcdir", NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        JetCoreEnvironment environmentForJS = JetCoreEnvironment.getCoreEnvironmentForJS(rootDisposable);
        Project project = environmentForJS.getProject();
        Config config = getConfig(arguments, project);
        if (analyzeAndReportErrors(messageCollector, environmentForJS.getSourceFiles(), config)) {
            return ExitCode.COMPILATION_ERROR;
        }

        if (arguments.srcdir != null) {
            environmentForJS.addSources(arguments.srcdir);
        }
        if (arguments.sourceFiles != null) {
            for (String sourceFile : arguments.sourceFiles) {
                environmentForJS.addSources(sourceFile);
            }
        }
        ClassPathLibrarySourcesLoader sourceLoader = new ClassPathLibrarySourcesLoader(project);
        List<JetFile> sourceFiles = sourceLoader.findSourceFiles();
        environmentForJS.getSourceFiles().addAll(sourceFiles);

        if (arguments.isVerbose()) {
            Iterable<String> fileNames = Iterables.transform(environmentForJS.getSourceFiles(), new Function<JetFile, String>() {
                @Override
                public String apply(@Nullable JetFile file) {
                    return file.getName();
                }
            });
            System.out.println("Compiling source files: " + Joiner.on(", ").join(fileNames));
        }

        String outputFile = arguments.outputFile;
        if (outputFile == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output file via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }


        MainCallParameters mainCallParameters = arguments.createMainCallParameters();
        return translateAndGenerateOutputFile(mainCallParameters, messageCollector, environmentForJS, config, outputFile);
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
            e.printStackTrace();
            return ExitCode.INTERNAL_ERROR;
        }
        return ExitCode.OK;
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
        if (arguments.libzip == null) {
            // lets discover the JS library definitions on the classpath
            return new ClassPathLibraryDefintionsConfig(project, ecmaVersion);
        }
        return new ZippedLibrarySourcesConfig(project, arguments.libzip, ecmaVersion);
    }
}
