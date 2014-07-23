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

package org.jetbrains.jet.cli.common;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentException;
import org.jetbrains.jet.config.CompilerConfiguration;

import java.io.PrintStream;
import java.util.List;

import static org.jetbrains.jet.cli.common.ExitCode.*;

public abstract class CLICompiler<A extends CommonCompilerArguments> {
    @NotNull
    private List<CompilerPlugin> compilerPlugins = Lists.newArrayList();

    @NotNull
    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }

    public void setCompilerPlugins(@NotNull List<CompilerPlugin> compilerPlugins) {
        this.compilerPlugins = compilerPlugins;
    }

    @NotNull
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull String... args) {
        return exec(errStream, MessageRenderer.PLAIN_WITH_RELATIVE_PATH, args);
    }

    @SuppressWarnings("UnusedDeclaration") // Used via reflection in CompilerRunnerUtil#invokeExecMethod
    @NotNull
    public ExitCode execAndOutputHtml(@NotNull PrintStream errStream, @NotNull String... args) {
        return exec(errStream, MessageRenderer.TAGS, args);
    }

    @Nullable
    private A parseArguments(@NotNull PrintStream errStream, @NotNull MessageRenderer messageRenderer, @NotNull String[] args) {
        try {
            A arguments = createArguments();
            arguments.freeArgs = Args.parse(arguments, args);
            return arguments;
        }
        catch (IllegalArgumentException e) {
            errStream.println(e.getMessage());
            usage(errStream, false);
        }
        catch (Throwable t) {
            errStream.println(messageRenderer.render(
                    CompilerMessageSeverity.EXCEPTION,
                    OutputMessageUtil.renderException(t),
                    CompilerMessageLocation.NO_LOCATION)
            );
        }
        return null;
    }

    /**
     * Allow derived classes to add additional command line arguments
     */
    protected void usage(@NotNull PrintStream target, boolean extraHelp) {
        Usage.print(target, createArguments(), extraHelp);
    }

    /**
     * Strategy method to configure the environment, allowing compiler
     * based tools to customise their own plugins
     */
    protected void configureEnvironment(@NotNull CompilerConfiguration configuration, @NotNull A arguments) {
        configuration.addAll(CLIConfigurationKeys.COMPILER_PLUGINS, compilerPlugins);
    }

    @NotNull
    protected abstract A createArguments();

    @NotNull
    private ExitCode exec(@NotNull PrintStream errStream, @NotNull MessageRenderer messageRenderer, @NotNull String[] args) {
        A arguments = parseArguments(errStream, messageRenderer, args);
        if (arguments == null) {
            return INTERNAL_ERROR;
        }

        if (arguments.help || arguments.extraHelp) {
            usage(errStream, arguments.extraHelp);
            return OK;
        }

        errStream.print(messageRenderer.renderPreamble());

        printVersionIfNeeded(errStream, arguments, messageRenderer);

        MessageCollector collector = new PrintingMessageCollector(errStream, messageRenderer, arguments.verbose);

        if (arguments.suppressWarnings) {
            collector = new FilteringMessageCollector(collector, Predicates.equalTo(CompilerMessageSeverity.WARNING));
        }

        try {
            return exec(collector, arguments);
        }
        finally {
            errStream.print(messageRenderer.renderConclusion());
        }
    }

    @NotNull
    public ExitCode exec(@NotNull MessageCollector messageCollector, @NotNull A arguments) {
        GroupingMessageCollector groupingCollector = new GroupingMessageCollector(messageCollector);
        try {
            Disposable rootDisposable = Disposer.newDisposable();
            try {
                MessageSeverityCollector severityCollector = new MessageSeverityCollector(groupingCollector);
                ExitCode code = doExecute(arguments, severityCollector, rootDisposable);
                return severityCollector.anyReported(CompilerMessageSeverity.ERROR) ? COMPILATION_ERROR : code;
            }
            finally {
                Disposer.dispose(rootDisposable);
            }
        }
        catch (Throwable t) {
            groupingCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(t),
                                     CompilerMessageLocation.NO_LOCATION);
            return INTERNAL_ERROR;
        }
        finally {
            groupingCollector.flush();
        }
    }

    @NotNull
    protected abstract ExitCode doExecute(@NotNull A arguments, @NotNull MessageCollector messageCollector, @NotNull Disposable rootDisposable);

    protected void printVersionIfNeeded(
            @NotNull PrintStream errStream,
            @NotNull A arguments,
            @NotNull MessageRenderer messageRenderer
    ) {
        if (arguments.version) {
            String versionMessage = messageRenderer.render(CompilerMessageSeverity.INFO,
                                                           "Kotlin Compiler version " + KotlinVersion.VERSION,
                                                           CompilerMessageLocation.NO_LOCATION);
            errStream.println(versionMessage);
        }
    }

    /**
     * Useful main for derived command line tools
     */
    public static void doMain(@NotNull CLICompiler compiler, @NotNull String[] args) {
        // We depend on swing (indirectly through PSI or something), so we want to declare headless mode,
        // to avoid accidentally starting the UI thread
        System.setProperty("java.awt.headless", "true");
        ExitCode exitCode = doMainNoExit(compiler, args);
        if (exitCode != OK) {
            System.exit(exitCode.getCode());
        }
    }

    @NotNull
    public static ExitCode doMainNoExit(@NotNull CLICompiler compiler, @NotNull String[] args) {
        try {
            return compiler.exec(System.err, args);
        }
        catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            return INTERNAL_ERROR;
        }
    }
}
