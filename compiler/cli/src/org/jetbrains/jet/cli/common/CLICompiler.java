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
import com.intellij.openapi.util.text.StringUtil;
import com.sampullara.cli.Args;
import com.sampullara.cli.ArgumentUtils;
import org.jetbrains.annotations.NotNull;
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
        A arguments = createArguments();
        if (!parseArguments(errStream, arguments, args)) {
            return INTERNAL_ERROR;
        }
        return exec(errStream, arguments);
    }

    /**
     * Returns true if the arguments can be parsed correctly
     */
    protected boolean parseArguments(@NotNull PrintStream errStream, @NotNull A arguments, @NotNull String[] args) {
        try {
            arguments.freeArgs = Args.parse(arguments, args);
            checkArguments(arguments);
            return true;
        }
        catch (IllegalArgumentException e) {
            errStream.println(e.getMessage());
            usage(errStream);
        }
        catch (Throwable t) {
            // Always use tags
            errStream.println(MessageRenderer.TAGS.renderException(t));
        }
        return false;
    }

    protected void checkArguments(@NotNull A argument) {

    }

    /**
     * Allow derived classes to add additional command line arguments
     */
    protected void usage(@NotNull PrintStream target) {
        Usage.print(target, createArguments());
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

    /**
     * Executes the compiler on the parsed arguments
     */
    @NotNull
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull A arguments) {
        if (arguments.help) {
            usage(errStream);
            return OK;
        }

        MessageRenderer messageRenderer = getMessageRenderer(arguments);
        errStream.print(messageRenderer.renderPreamble());

        printArgumentsIfNeeded(errStream, arguments, messageRenderer);
        printVersionIfNeeded(errStream, arguments, messageRenderer);

        MessageCollector collector = new PrintingMessageCollector(errStream, messageRenderer, arguments.verbose);

        if (arguments.suppressAllWarnings()) {
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
            groupingCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(t),
                                     CompilerMessageLocation.NO_LOCATION);
            return INTERNAL_ERROR;
        }
        finally {
            groupingCollector.flush();
        }
    }

    @NotNull
    protected abstract ExitCode doExecute(@NotNull A arguments, @NotNull MessageCollector messageCollector, @NotNull Disposable rootDisposable);

    //TODO: can we make it private?
    @NotNull
    protected MessageRenderer getMessageRenderer(@NotNull A arguments) {
        return arguments.tags ? MessageRenderer.TAGS : MessageRenderer.PLAIN;
    }

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

    private void printArgumentsIfNeeded(
            @NotNull PrintStream errStream,
            @NotNull A arguments,
            @NotNull MessageRenderer messageRenderer
    ) {
        if (arguments.printArgs) {
            String freeArgs = !arguments.freeArgs.isEmpty() ? " " + StringUtil.join(arguments.freeArgs, " ") : "";

            List<String> argumentsAsList = ArgumentUtils.convertArgumentsToStringList(arguments, createArguments());
            String argumentsAsString = StringUtil.join(argumentsAsList, " ");

            String printArgsMessage = messageRenderer.render(CompilerMessageSeverity.INFO,
                                                             "Invoking " + getClass().getSimpleName() +
                                                             " with arguments " + argumentsAsString + freeArgs,
                                                             CompilerMessageLocation.NO_LOCATION);
            errStream.println(printArgsMessage);
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
            ExitCode rc = compiler.exec(System.out, args);
            if (rc != OK) {
                System.err.println("exec() finished with " + rc + " return code");
            }
            return rc;
        }
        catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            return INTERNAL_ERROR;
        }
    }
}
