/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.AppScheduledExecutorService;
import com.sampullara.cli.Args;
import kotlin.Pair;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.*;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentException;
import org.jetbrains.kotlin.cli.jvm.compiler.CompilerJarLocator;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.progress.CompilationCanceledException;
import org.jetbrains.kotlin.progress.CompilationCanceledStatus;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

import static org.jetbrains.kotlin.cli.common.ExitCode.*;

public abstract class CLICompiler<A extends CommonCompilerArguments> {
    static private void setIdeaIoUseFallback() {
        if (SystemInfo.isWindows) {
            Properties properties = System.getProperties();

            properties.setProperty("idea.io.use.nio2", Boolean.TRUE.toString());

            if (!(SystemInfo.isJavaVersionAtLeast("1.7") && !"1.7.0-ea".equals(SystemInfo.JAVA_VERSION))) {
                properties.setProperty("idea.io.use.fallback", Boolean.TRUE.toString());
            }
        }
    }

    @NotNull
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull String... args) {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_RELATIVE_PATHS, args);
    }

    // Used via reflection in CompilerRunnerUtil#invokeExecMethod and in Eclipse plugin (see KotlinCLICompiler)
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public ExitCode execAndOutputXml(@NotNull PrintStream errStream, @NotNull Services services, @NotNull String... args) {
        return exec(errStream, services, MessageRenderer.XML, args);
    }

    // Used via reflection in KotlinCompilerBaseTask
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public ExitCode execFullPathsInMessages(@NotNull PrintStream errStream, @NotNull String[] args) {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_FULL_PATHS, args);
    }

    @Nullable
    private A parseArguments(@NotNull PrintStream errStream, @NotNull MessageRenderer messageRenderer, @NotNull String[] args) {
        try {
            A arguments = createArguments();
            parseArguments(args, arguments);
            return arguments;
        }
        catch (IllegalArgumentException e) {
            errStream.println(e.getMessage());
            Usage.print(errStream, createArguments(), false);
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

    @SuppressWarnings("WeakerAccess") // Used in maven (see KotlinCompileMojoBase.java)
    public void parseArguments(@NotNull String[] args, @NotNull A arguments) {
        Pair<List<String>, List<String>> unparsedArgs =
                CollectionsKt.partition(Args.parse(arguments, args, false), new Function1<String, Boolean>() {
                    @Override
                    public Boolean invoke(String s) {
                        return s.startsWith("-X");
                    }
                });

        arguments.unknownExtraFlags = unparsedArgs.getFirst();
        arguments.freeArgs = unparsedArgs.getSecond();

        for (String argument : arguments.freeArgs) {
            if (argument.startsWith("-")) {
                throw new IllegalArgumentException("Invalid argument: " + argument);
            }
        }
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
        K2JVMCompiler.Companion.resetInitStartTime();

        A arguments = parseArguments(errStream, messageRenderer, args);
        if (arguments == null) {
            return INTERNAL_ERROR;
        }

        if (arguments.help || arguments.extraHelp) {
            Usage.print(errStream, createArguments(), arguments.extraHelp);
            return OK;
        }

        MessageCollector collector = new PrintingMessageCollector(errStream, messageRenderer, arguments.verbose);

        try {
            if (PlainTextMessageRenderer.COLOR_ENABLED) {
                AnsiConsole.systemInstall();
            }

            errStream.print(messageRenderer.renderPreamble());
            return exec(collector, services, arguments);
        }
        finally {
            errStream.print(messageRenderer.renderConclusion());

            if (PlainTextMessageRenderer.COLOR_ENABLED) {
                AnsiConsole.systemUninstall();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") // Used in maven (see KotlinCompileMojoBase.java)
    @NotNull
    public ExitCode exec(@NotNull MessageCollector messageCollector, @NotNull Services services, @NotNull A arguments) {
        printVersionIfNeeded(messageCollector, arguments);

        if (arguments.suppressWarnings) {
            messageCollector = new FilteringMessageCollector(messageCollector, Predicates.equalTo(CompilerMessageSeverity.WARNING));
        }

        reportUnknownExtraFlags(messageCollector, arguments);

        GroupingMessageCollector groupingCollector = new GroupingMessageCollector(messageCollector);

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, groupingCollector);

        setupCommonArgumentsAndServices(configuration, arguments, services);
        setupPlatformSpecificArgumentsAndServices(configuration, arguments, services);

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
                    setIdeaIoUseFallback();
                    ExitCode code = doExecute(arguments, configuration, rootDisposable);
                    exitCode = groupingCollector.hasErrors() ? COMPILATION_ERROR : code;
                }
                catch (CompilationCanceledException e) {
                    messageCollector.report(CompilerMessageSeverity.INFO, "Compilation was canceled", CompilerMessageLocation.NO_LOCATION);
                    return ExitCode.OK;
                }
                catch (RuntimeException e) {
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

    private static void setupCommonArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull CommonCompilerArguments arguments, @NotNull Services services
    ) {
        if (arguments.noInline) {
            configuration.put(CommonConfigurationKeys.DISABLE_INLINE, true);
        }

        CompilerJarLocator locator = services.get(CompilerJarLocator.class);
        if (locator != null) {
            configuration.put(CLIConfigurationKeys.COMPILER_JAR_LOCATOR, locator);
        }

        LanguageVersion languageVersion = parseVersion(configuration, arguments.languageVersion, "language");
        LanguageVersion apiVersion = parseVersion(configuration, arguments.apiVersion, "API");
        if (languageVersion != null || apiVersion != null) {
            if (languageVersion == null) {
                // If only "-api-version" is specified, language version is assumed to be the latest
                languageVersion = LanguageVersion.LATEST;
            }
            if (apiVersion == null) {
                // If only "-language-version" is specified, API version is assumed to be equal to the language version
                // (API version cannot be greater than the language version)
                apiVersion = languageVersion;
            }

            if (apiVersion.compareTo(languageVersion) > 0) {
                configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                        CompilerMessageSeverity.ERROR,
                        "-api-version (" + apiVersion.getVersionString() + ") cannot be greater than " +
                        "-language-version (" + languageVersion.getVersionString() + ")",
                        CompilerMessageLocation.NO_LOCATION
                );
            }

            configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                              new LanguageVersionSettingsImpl(languageVersion, ApiVersion.createByLanguageVersion(apiVersion)));
        }
    }

    private static LanguageVersion parseVersion(
            @NotNull CompilerConfiguration configuration, @Nullable String value, @NotNull String versionOf
    ) {
        if (value == null) return null;

        LanguageVersion version = LanguageVersion.fromVersionString(value);
        if (version != null) {
            return version;
        }

        List<String> versionStrings = ArraysKt.map(LanguageVersion.values(), new Function1<LanguageVersion, String>() {
            @Override
            public String invoke(LanguageVersion version) {
                return version.getVersionString();
            }
        });
        String message = "Unknown " + versionOf + " version: " + value + "\n" +
                         "Supported " + versionOf + " versions: " + StringsKt.join(versionStrings, ", ");
        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                CompilerMessageSeverity.ERROR, message, CompilerMessageLocation.NO_LOCATION
        );

        return null;
    }

    protected abstract void setupPlatformSpecificArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull A arguments, @NotNull Services services
    );

    private void reportUnknownExtraFlags(@NotNull MessageCollector collector, @NotNull A arguments) {
        for (String flag : arguments.unknownExtraFlags) {
            collector.report(
                    CompilerMessageSeverity.WARNING,
                    "Flag is not supported by this version of the compiler: " + flag,
                    CompilerMessageLocation.NO_LOCATION
            );
        }
    }

    @NotNull
    protected abstract ExitCode doExecute(
            @NotNull A arguments,
            @NotNull CompilerConfiguration configuration,
            @NotNull Disposable rootDisposable
    );

    private void printVersionIfNeeded(@NotNull MessageCollector messageCollector, @NotNull A arguments) {
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
        try {
            ExitCode exitCode = doMainNoExit(compiler, args);

            if (exitCode != OK) {
                System.exit(exitCode.getCode());
            }
        }
        finally {
            AppScheduledExecutorService service = (AppScheduledExecutorService) AppExecutorUtil.getAppScheduledExecutorService();
            service.shutdownAppScheduledExecutorService();
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
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
