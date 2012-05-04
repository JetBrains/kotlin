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
import com.intellij.psi.PsiFile;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
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

        final JetCoreEnvironment environmentForJS = JetCoreEnvironment.getCoreEnvironmentForJS(rootDisposable);
        environmentForJS.addSources(arguments.srcdir);
        AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(messageCollector);
        final List<JetFile> sources = environmentForJS.getSourceFiles();
        final Config config = Config.getEmptyConfig(environmentForJS.getProject());
        analyzerWithCompilerReport.analyzeAndReport(new Function0<AnalyzeExhaust>() {
            @Override
            public AnalyzeExhaust invoke() {
                BindingContext context = AnalyzerFacadeForJS
                        .analyzeFiles(sources, Predicates.<PsiFile>alwaysTrue(), config);
                return AnalyzeExhaust.success(context, JetStandardLibrary.getInstance());
            }
        }, sources);
        if (analyzerWithCompilerReport.hasErrors()) {

            return ExitCode.COMPILATION_ERROR;
        }

        if (arguments.outputDir != null) {
            try {
                K2JSTranslator.translateWithCallToMainAndSaveToFile(environmentForJS.getSourceFiles(), arguments.outputDir, config, environmentForJS.getProject());
            }
            catch (Exception e) {
                return ExitCode.INTERNAL_ERROR;
            }
        } else {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify output directory via -output", CompilerMessageLocation.NO_LOCATION);
            return ExitCode.INTERNAL_ERROR;
        }
        return ExitCode.OK;
    }
}
