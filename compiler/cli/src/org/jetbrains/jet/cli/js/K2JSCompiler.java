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
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
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
    protected ExitCode doExecute(PrintStream stream, K2JSCompilerArguments arguments, MessageRenderer renderer) {
        PrintingMessageCollector messageCollector = new PrintingMessageCollector(stream, renderer, true);
        if (arguments.module == null) {
            stream.print(renderer.render(CompilerMessageSeverity.ERROR, "Module should be specified", NO_LOCATION));
            return ExitCode.INTERNAL_ERROR;
        }

        File directory = new File(arguments.module).getParentFile();
        List<Module> modules = CompileEnvironmentUtil
                .loadModuleScript(arguments.module, MessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR);
        for (Module module : modules) {
            Disposable rootDisposable = CompileEnvironmentUtil.createMockDisposable();
            final JetCoreEnvironment environmentForJS = JetCoreEnvironment.getCoreEnvironmentForJS(rootDisposable);
            CompileEnvironmentUtil.addSourcesFromModuleToEnvironment(environmentForJS, module, directory);
            AnalyzerWithCompilerReport analyzerWithCompilerReport =
                    new AnalyzerWithCompilerReport(messageCollector);
            final List<JetFile> sources = environmentForJS.getSourceFiles();
            analyzerWithCompilerReport.analyzeAndReport(new Function0<AnalyzeExhaust>() {
                @Override
                public AnalyzeExhaust invoke() {
                    BindingContext context = AnalyzerFacadeForJS
                            .analyzeFiles(sources, Predicates.<PsiFile>alwaysTrue(), new Config(environmentForJS.getProject()) {
                                @NotNull
                                @Override
                                protected List<JetFile> generateLibFiles() {
                                    return Collections.emptyList();
                                }
                            });
                    return AnalyzeExhaust.success(context, JetStandardLibrary.getInstance());
                }
            }, sources);
        }

        stream.print(renderer.render(CompilerMessageSeverity.ERROR, "Greeting", NO_LOCATION));
        return ExitCode.OK;
    }
}
