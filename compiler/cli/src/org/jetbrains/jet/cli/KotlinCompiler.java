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

package org.jetbrains.jet.cli;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.sampullara.cli.Args;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.*;
import org.jetbrains.jet.compiler.messages.CompilerMessageLocation;
import org.jetbrains.jet.compiler.messages.CompilerMessageSeverity;
import org.jetbrains.jet.compiler.messages.MessageCollector;
import org.jetbrains.jet.compiler.messages.MessageRenderer;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.cli.KotlinCompiler.ExitCode.*;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class KotlinCompiler {

    public enum ExitCode {
        OK(0),
        COMPILATION_ERROR(1),
        INTERNAL_ERROR(2);

        private final int code;

        private ExitCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static void main(String... args) {
        doMain(new KotlinCompiler(), args);
    }

    /**
     * Useful main for derived command line tools
     */
    public static void doMain(KotlinCompiler compiler, String[] args) {
        try {
            ExitCode rc = compiler.exec(System.out, args);
            if (rc != OK) {
                System.err.println("exec() finished with " + rc + " return code");
                System.exit(rc.getCode());
            }
        }
        catch (CompileEnvironmentException e) {
            System.err.println(e.getMessage());
            System.exit(INTERNAL_ERROR.getCode());
        }
    }

    public ExitCode exec(PrintStream errStream, String... args) {
        CompilerArguments arguments = createArguments();
        if (!parseArguments(errStream, arguments, args)) {
            return INTERNAL_ERROR;
        }
        return exec(errStream, arguments);
    }

    /**
     * Executes the compiler on the parsed arguments
     */
    public ExitCode exec(final PrintStream errStream, CompilerArguments arguments) {
        if (arguments.help) {
            usage(errStream);
            return OK;
        }
        System.setProperty("java.awt.headless", "true");

        final MessageRenderer messageRenderer = arguments.tags ? MessageRenderer.TAGS : MessageRenderer.PLAIN;

        errStream.print(messageRenderer.renderPreamble());

        try {
            if (arguments.version) {
                errStream.println(messageRenderer.render(CompilerMessageSeverity.INFO, "Kotlin Compiler version " + CompilerVersion.VERSION, CompilerMessageLocation.NO_LOCATION));
            }

            CompilerSpecialMode mode = parseCompilerSpecialMode(arguments);

            File jdkHeadersJar;
            if (mode.includeJdkHeaders()) {
                if (arguments.jdkHeaders != null) {
                    jdkHeadersJar = new File(arguments.jdkHeaders);
                }
                else {
                    jdkHeadersJar = PathUtil.getAltHeadersPath();
                }
            }
            else {
                jdkHeadersJar = null;
            }
            File runtimeJar;

            if (mode.includeKotlinRuntime()) {
                if (arguments.stdlib != null) {
                    runtimeJar = new File(arguments.stdlib);
                }
                else {
                    runtimeJar = PathUtil.getDefaultRuntimePath();
                }
            }
            else {
                runtimeJar = null;
            }

            CompilerDependencies dependencies = new CompilerDependencies(mode, jdkHeadersJar, runtimeJar);
            PrintingMessageCollector messageCollector = new PrintingMessageCollector(errStream, messageRenderer, arguments.verbose);
            Disposable rootDisposable = CompileEnvironmentUtil.createMockDisposable();

            JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, dependencies);
            CompileEnvironmentConfiguration configuration = new CompileEnvironmentConfiguration(environment, dependencies, messageCollector);

            configuration.getMessageCollector().report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment", CompilerMessageLocation.NO_LOCATION);
            try {
                configureEnvironment(configuration, arguments);

                boolean noErrors;
                if (arguments.module != null) {
                    List<Module> modules = CompileEnvironmentUtil.loadModuleScript(arguments.module, configuration.getMessageCollector());
                    File directory = new File(arguments.module).getParentFile();
                    noErrors = KotlinToJVMBytecodeCompiler.compileModules(configuration, modules,
                                                                          directory, arguments.jar, arguments.outputDir,
                                                                          arguments.includeRuntime);
                }
                else {
                    // TODO ideally we'd unify to just having a single field that supports multiple files/dirs
                    if (arguments.getSourceDirs() != null) {
                        noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSourceDirectories(configuration,
                                                                                               arguments.getSourceDirs(), arguments.jar, arguments.outputDir, arguments.includeRuntime);
                    }
                    else {
                        noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSources(configuration,
                                                                     arguments.src, arguments.jar, arguments.outputDir, arguments.includeRuntime);
                    }
                }
                return noErrors ? OK : COMPILATION_ERROR;
            }
            catch (Throwable t) {
                errStream.println(messageRenderer.renderException(t));
                return INTERNAL_ERROR;
            }
            finally {
                Disposer.dispose(rootDisposable);
                messageCollector.printToErrStream();
            }
        }
        finally {
             errStream.print(messageRenderer.renderConclusion());
        }
    }

    @NotNull
    private CompilerSpecialMode parseCompilerSpecialMode(@NotNull CompilerArguments arguments) {
        if (arguments.mode == null) {
            return CompilerSpecialMode.REGULAR;
        }
        else {
            for (CompilerSpecialMode variant : CompilerSpecialMode.values()) {
                if (arguments.mode.equalsIgnoreCase(variant.name().replaceAll("_", ""))) {
                    return variant;
                }
            }
        }
        // TODO: report properly
        throw new IllegalArgumentException("unknown compiler mode: " + arguments.mode);
    }

    /**
     * Returns true if the arguments can be parsed correctly
     */
    protected boolean parseArguments(PrintStream errStream, CompilerArguments arguments, String[] args) {
        try {
            Args.parse(arguments, args);
            return true;
        }
        catch (IllegalArgumentException e) {
            usage(errStream);
        }
        catch (Throwable t) {
            // Always use tags
            errStream.println(MessageRenderer.TAGS.renderException(t));
        }
        return false;
    }

    protected void usage(PrintStream target) {
        // We should say something like
        //   Args.usage(target, CompilerArguments.class);
        // but currently cli-parser we are using does not support that
        // a corresponding patch has been sent to the authors
        // For now, we are using this:

        PrintStream oldErr = System.err;
        System.setErr(target);
        try {
            // TODO: use proper argv0
            Args.usage(new CompilerArguments());
        } finally {
            System.setErr(oldErr);
        }
    }

    /**
     * Allow derived classes to add additional command line arguments
     */
    protected CompilerArguments createArguments() {
        return new CompilerArguments();
    }

    /**
     * Strategy method to configure the environment, allowing compiler
     * based tools to customise their own plugins
     */
    protected void configureEnvironment(CompileEnvironmentConfiguration configuration, CompilerArguments arguments) {
        // install any compiler plugins
        List<CompilerPlugin> plugins = arguments.getCompilerPlugins();
        if (plugins != null) {
            configuration.getCompilerPlugins().addAll(plugins);
        }

        if (configuration.getCompilerDependencies().getRuntimeJar() != null) {
            CompileEnvironmentUtil.addToClasspath(configuration.getEnvironment(), configuration.getCompilerDependencies().getRuntimeJar());
        }

        if (arguments.classpath != null) {
            final Iterable<String> classpath = Splitter.on(File.pathSeparatorChar).split(arguments.classpath);
            CompileEnvironmentUtil.addToClasspath(configuration.getEnvironment(), Iterables.toArray(classpath, String.class));
        }
    }

    private static class PrintingMessageCollector implements MessageCollector {
        private final boolean verbose;
        private final PrintStream errStream;
        private final MessageRenderer messageRenderer;

        // File path (nullable) -> error message
        private final Multimap<String, String> groupedMessages = LinkedHashMultimap.create();

        public PrintingMessageCollector(PrintStream errStream,
                MessageRenderer messageRenderer,
                boolean verbose) {
            this.verbose = verbose;
            this.errStream = errStream;
            this.messageRenderer = messageRenderer;
        }

        @Override
        public void report(@NotNull CompilerMessageSeverity severity,
                @NotNull String message,
                @NotNull CompilerMessageLocation location) {
            String text = messageRenderer.render(severity, message, location);
            if (severity == CompilerMessageSeverity.LOGGING) {
                if (!verbose) {
                    return;
                }
                errStream.println(text);
            }
            groupedMessages.put(location.getPath(), text);
        }

        public void printToErrStream() {
            if (!groupedMessages.isEmpty()) {
                for (String path : groupedMessages.keySet()) {
                    Collection<String> messageTexts = groupedMessages.get(path);
                    for (String text : messageTexts) {
                        errStream.println(text);
                    }
                }
            }
        }
    }
}
