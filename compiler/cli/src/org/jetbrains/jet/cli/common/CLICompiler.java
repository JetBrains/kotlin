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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.sampullara.cli.Args;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentException;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.config.CompilerConfiguration;

import java.io.PrintStream;

import static org.jetbrains.jet.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.jet.cli.common.ExitCode.INTERNAL_ERROR;
import static org.jetbrains.jet.cli.common.ExitCode.OK;

public abstract class CLICompiler<A extends CompilerArguments> {

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

    /**
     * Allow derived classes to add additional command line arguments
     */
    protected void usage(@NotNull PrintStream target) {
        // We should say something like
        //   Args.usage(target, K2JVMCompilerArguments.class);
        // but currently cli-parser we are using does not support that
        // a corresponding patch has been sent to the authors
        // For now, we are using this:
        PrintStream oldErr = System.err;
        System.setErr(target);
        try {
            // TODO: use proper argv0
            Args.usage(createArguments());
        }
        finally {
            System.setErr(oldErr);
        }
    }

    /**
     * Strategy method to configure the environment, allowing compiler
     * based tools to customise their own plugins
     */
    //TODO: add parameter annotations when KT-1863 is resolved
    protected void configureEnvironment(@NotNull CompilerConfiguration configuration, @NotNull A arguments) {
        configuration.addAll(CLIConfigurationKeys.COMPILER_PLUGINS, arguments.getCompilerPlugins());
    }

    @NotNull
    protected abstract A createArguments();

    /**
     * Executes the compiler on the parsed arguments
     */
    @NotNull
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull A arguments) {
        if (arguments.isHelp()) {
            usage(errStream);
            return OK;
        }

        MessageRenderer messageRenderer = getMessageRenderer(arguments);
        errStream.print(messageRenderer.renderPreamble());

        printVersionIfNeeded(errStream, arguments, messageRenderer);

        PrintingMessageCollector printingCollector = new PrintingMessageCollector(errStream, messageRenderer, arguments.isVerbose());

        try {
            return exec(printingCollector, arguments);
        }
        finally {
            errStream.print(messageRenderer.renderConclusion());
        }
    }

    @NotNull
    public ExitCode exec(@NotNull MessageCollector messageCollector, @NotNull A arguments) {
        GroupingMessageCollector groupingCollector = new GroupingMessageCollector(messageCollector);
        try {
            Disposable rootDisposable = CompileEnvironmentUtil.createMockDisposable();
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

    //TODO: can't declare parameters as not null due to KT-1863
    @NotNull
    protected abstract ExitCode doExecute(A arguments, MessageCollector messageCollector, Disposable rootDisposable);

    //TODO: can we make it private?
    @NotNull
    protected MessageRenderer getMessageRenderer(@NotNull A arguments) {
        return arguments.isTags() ? MessageRenderer.TAGS : MessageRenderer.PLAIN;
    }

    protected void printVersionIfNeeded(@NotNull PrintStream errStream,
            @NotNull A arguments,
            @NotNull MessageRenderer messageRenderer) {
        if (arguments.isVersion()) {
            String versionMessage = messageRenderer.render(CompilerMessageSeverity.INFO,
                                                           "Kotlin Compiler version " + CompilerVersion.VERSION,
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
            ExitCode rc = compiler.exec(System.out, args);
            if (rc != OK) {
                System.err.println("exec() finished with " + rc + " return code");
            }
            if (Boolean.parseBoolean(System.getProperty("kotlin.print.cmd.args"))) {
                System.out.println("Command line arguments:");
                for (String arg : args) {
                    System.out.println(arg);
                }
            }
            return rc;
        }
        catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            return INTERNAL_ERROR;
        }
    }
}
