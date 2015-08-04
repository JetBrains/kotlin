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

package org.jetbrains.kotlin.cli.common;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.sampullara.cli.Args;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.*;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.progress.CompilationCanceledException;
import org.jetbrains.kotlin.progress.CompilationCanceledStatus;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;

import java.io.PrintStream;
import java.util.List;

import static org.jetbrains.kotlin.cli.common.ExitCode.*;

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
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_RELATIVE_PATHS, args);
    }

    @SuppressWarnings("UnusedDeclaration") // Used via reflection in CompilerRunnerUtil#invokeExecMethod
    @NotNull
    public ExitCode execAndOutputXml(@NotNull PrintStream errStream, @NotNull Services services, @NotNull String... args) {
        K2JVMCompiler.Companion.resetInitStartTime();
        return exec(errStream, services, MessageRenderer.XML, args);
    }

    @SuppressWarnings("UnusedDeclaration") // Used via reflection in KotlinCompilerBaseTask
    @NotNull
    public ExitCode execFullPathsInMessages(@NotNull PrintStream errStream, @NotNull String[] args) {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_FULL_PATHS, args);
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
    private ExitCode exec(
            @NotNull PrintStream errStream,
            @NotNull Services services,
            @NotNull MessageRenderer messageRenderer,
            @NotNull String[] args
    ) {
        A arguments = parseArguments(errStream, messageRenderer, args);
        if (arguments == null) {
            return INTERNAL_ERROR;
        }

        if (arguments.help || arguments.extraHelp) {
            usage(errStream, arguments.extraHelp);
            return OK;
        }

        MessageCollector collector = new PrintingMessageCollector(errStream, messageRenderer, arguments.verbose);

        try {
            AnsiConsole.systemInstall();
            errStream.print(messageRenderer.renderPreamble());
            return exec(collector, services, arguments);
        }
        finally {
            errStream.print(messageRenderer.renderConclusion());
            AnsiConsole.systemUninstall();
        }
    }

    @NotNull
    public ExitCode exec(@NotNull MessageCollector messageCollector, @NotNull Services services, @NotNull A arguments) {
        printVersionIfNeeded(messageCollector, arguments);

        if (arguments.suppressWarnings) {
            messageCollector = new FilteringMessageCollector(messageCollector, Predicates.equalTo(CompilerMessageSeverity.WARNING));
        }

        GroupingMessageCollector groupingCollector = new GroupingMessageCollector(messageCollector);
        try {
            ExitCode exitCode = OK;

            int repeatCount = 1;
            if (arguments.repeat != null) {
                try {
                    repeatCount = Integer.parseInt(arguments.repeat);
                }
                catch (NumberFormatException ignored) {
                }
            }

            CompilationCanceledStatus canceledStatus = services.get(CompilationCanceledStatus.class);
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus);

            for (int i = 0; i < repeatCount; i++) {
                if (i > 0) {
                    K2JVMCompiler.Companion.resetInitStartTime();
                }
                Disposable rootDisposable = Disposer.newDisposable();
                try {
                    MessageSeverityCollector severityCollector = new MessageSeverityCollector(groupingCollector);
                    ExitCode code = doExecute(arguments, services, severityCollector, rootDisposable);
                    exitCode = severityCollector.anyReported(CompilerMessageSeverity.ERROR) ? COMPILATION_ERROR : code;
                }
                catch(CompilationCanceledException e) {
                    messageCollector.report(CompilerMessageSeverity.INFO, "Compilation was canceled", CompilerMessageLocation.NO_LOCATION);
                    return ExitCode.OK;
                }
                catch(RuntimeException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof CompilationCanceledException) {
                        messageCollector
                                .report(CompilerMessageSeverity.INFO, "Compilation was canceled", CompilerMessageLocation.NO_LOCATION);
                        return ExitCode.OK;
                    }
                    else {
                        throw e;
                    }
                }
                finally {
                    Disposer.dispose(rootDisposable);
                }
            }
            return exitCode;
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
    protected abstract ExitCode doExecute(
            @NotNull A arguments,
            @NotNull Services services,
            @NotNull MessageCollector messageCollector,
            @NotNull Disposable rootDisposable
    );

    protected void printVersionIfNeeded(@NotNull MessageCollector messageCollector, @NotNull A arguments) {
        if (!arguments.version) return;

        messageCollector.report(CompilerMessageSeverity.INFO,
                                "Kotlin Compiler version " + KotlinVersion.VERSION,
                                CompilerMessageLocation.NO_LOCATION);
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
