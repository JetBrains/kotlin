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

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.config.ZippedLibrarySourcesConfig;
import org.jetbrains.k2js.facade.K2JSTranslator;

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

        if (arguments.srcdir == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify sources location via -srcdir", NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        JetCoreEnvironment environmentForJS = getEnvironment(arguments, rootDisposable);
        Config config = getConfig(arguments, environmentForJS.getProject());
        if (analyzeAndReportErrors(messageCollector, environmentForJS.getSourceFiles(), config)) {
            return ExitCode.COMPILATION_ERROR;
        }

        String outputFile = arguments.outputFile;
        if (outputFile == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output file via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }

        return translateAndGenerateOutputFile(messageCollector, environmentForJS, config, outputFile);
    }

    @NotNull
    private static JetCoreEnvironment getEnvironment(K2JSCompilerArguments arguments, Disposable rootDisposable) {
        final JetCoreEnvironment environmentForJS = JetCoreEnvironment.getCoreEnvironmentForJS(rootDisposable);
        environmentForJS.addSources(arguments.srcdir);
        return environmentForJS;
    }

    @NotNull
    private static ExitCode translateAndGenerateOutputFile(@NotNull PrintingMessageCollector messageCollector,
            @NotNull JetCoreEnvironment environmentForJS, @NotNull Config config, @NotNull String outputFile) {
        try {
            K2JSTranslator.translateWithCallToMainAndSaveToFile(environmentForJS.getSourceFiles(), outputFile, config);
        }
        catch (Exception e) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Exception while translating:\n" + e.getMessage(),
                                    CompilerMessageLocation.NO_LOCATION);
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
                BindingContext context = AnalyzerFacadeForJS
                        .analyzeFiles(sources, Predicates.<PsiFile>alwaysTrue(), config);
                return AnalyzeExhaust.success(context, JetStandardLibrary.getInstance());
            }
        }, sources);
        return analyzerWithCompilerReport.hasErrors();
    }

    @NotNull
    private static Config getConfig(@NotNull K2JSCompilerArguments arguments, @NotNull Project project) {
        if (arguments.libzip == null) {
            return Config.getEmptyConfig(project, arguments.target);
        }
        return new ZippedLibrarySourcesConfig(project, arguments.libzip, EcmaVersion.fromString(arguments.target));
    }
}
