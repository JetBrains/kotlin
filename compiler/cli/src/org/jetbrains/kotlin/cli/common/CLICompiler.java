/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import kotlin.collections.ArraysKt;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.jetbrains.kotlin.cli.common.ExitCode.*;
import static org.jetbrains.kotlin.cli.common.environment.UtilKt.setIdeaIoUseFallback;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*;

public abstract class CLICompiler<A extends CommonCompilerArguments> {

    @NotNull
    public ExitCode exec(@NotNull PrintStream errStream, @NotNull String... args) {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_RELATIVE_PATHS, args);
    }

    // Used in CompilerRunnerUtil#invokeExecMethod, in Eclipse plugin (KotlinCLICompiler) and in kotlin-gradle-plugin (GradleCompilerRunner)
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
    private A parseArguments(@NotNull MessageCollector messageCollector, @NotNull String[] args) {
        try {
            A arguments = createArguments();
            parseArguments(args, arguments);
            return arguments;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Throwable t) {
            messageCollector.report(EXCEPTION, OutputMessageUtil.renderException(t), null);
            return null;
        }
    }

    // Used in kotlin-maven-plugin (KotlinCompileMojoBase) and in kotlin-gradle-plugin (KotlinJvmOptionsImpl, KotlinJsOptionsImpl)
    public void parseArguments(@NotNull String[] args, @NotNull A arguments) {
        ParseCommandLineArgumentsKt.parseCommandLineArguments(args, arguments);
        String message = ParseCommandLineArgumentsKt.validateArguments(arguments.errors);
        if (message != null) {
            throw new IllegalArgumentException(message);
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

        MessageCollector parseArgumentsCollector = new PrintingMessageCollector(errStream, messageRenderer, false);
        A arguments;
        try {
            arguments = parseArguments(parseArgumentsCollector, args);
            if (arguments == null) return INTERNAL_ERROR;
        }
        catch (IllegalArgumentException e) {
            parseArgumentsCollector.report(ERROR, e.getMessage(), null);
            parseArgumentsCollector.report(INFO, "Use -help for more information", null);
            return COMPILATION_ERROR;
        }

        if (arguments.help || arguments.extraHelp) {
            Usage.print(errStream, this, arguments);
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

    // Used in kotlin-maven-plugin (KotlinCompileMojoBase)
    @NotNull
    public ExitCode exec(@NotNull MessageCollector messageCollector, @NotNull Services services, @NotNull A arguments) {
        printVersionIfNeeded(messageCollector, arguments);

        if (arguments.suppressWarnings) {
            messageCollector = new FilteringMessageCollector(messageCollector, Predicate.isEqual(WARNING));
        }

        reportArgumentParseProblems(messageCollector, arguments.errors);

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
                    messageCollector.report(INFO, "Compilation was canceled", null);
                    return ExitCode.OK;
                }
                catch (RuntimeException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof CompilationCanceledException) {
                        messageCollector.report(INFO, "Compilation was canceled", null);
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
            MessageCollectorUtil.reportException(groupingCollector, t);
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
        if (arguments.intellijPluginRoot != null) {
            configuration.put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, arguments.intellijPluginRoot);
        }
        if (arguments.reportOutputFiles) {
            configuration.put(CommonConfigurationKeys.REPORT_OUTPUT_FILES, true);
        }
        @SuppressWarnings("deprecation")
        CompilerJarLocator locator = services.get(CompilerJarLocator.class);
        if (locator != null) {
            configuration.put(CLIConfigurationKeys.COMPILER_JAR_LOCATOR, locator);
        }

        setupLanguageVersionSettings(configuration, arguments);
    }

    private static void setupLanguageVersionSettings(
            @NotNull CompilerConfiguration configuration, @NotNull CommonCompilerArguments arguments
    ) {
        LanguageVersion languageVersion = parseVersion(configuration, arguments.languageVersion, "language");
        LanguageVersion apiVersion = parseVersion(configuration, arguments.apiVersion, "API");

        if (languageVersion == null) {
            // If only "-api-version" is specified, language version is assumed to be the latest stable
            languageVersion = LanguageVersion.LATEST_STABLE;
        }

        if (apiVersion == null) {
            // If only "-language-version" is specified, API version is assumed to be equal to the language version
            // (API version cannot be greater than the language version)
            apiVersion = languageVersion;
        }
        else {
            configuration.put(CLIConfigurationKeys.IS_API_VERSION_EXPLICIT, true);
        }

        if (apiVersion.compareTo(languageVersion) > 0) {
            configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                    ERROR,
                    "-api-version (" + apiVersion.getVersionString() + ") cannot be greater than " +
                    "-language-version (" + languageVersion.getVersionString() + ")",
                    null
            );
        }

        if (!languageVersion.isStable()) {
            configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                    STRONG_WARNING,
                    "Language version " + languageVersion.getVersionString() + " is experimental, there are " +
                    "no backwards compatibility guarantees for new language and library features",
                    null
            );
        }

        Map<LanguageFeature, LanguageFeature.State> extraLanguageFeatures = new HashMap<>(0);
        if (arguments.multiPlatform) {
            extraLanguageFeatures.put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED);
        }

        LanguageFeature.State coroutinesState = chooseCoroutinesApplicabilityLevel(configuration, arguments);
        if (coroutinesState != null) {
            extraLanguageFeatures.put(LanguageFeature.Coroutines, coroutinesState);
        }

        LanguageVersionSettingsImpl settings =
                new LanguageVersionSettingsImpl(languageVersion, ApiVersion.createByLanguageVersion(apiVersion), extraLanguageFeatures);
        settings.switchFlag(AnalysisFlags.getSkipMetadataVersionCheck(), arguments.skipMetadataVersionCheck);
        settings.switchFlag(AnalysisFlags.getMultiPlatformDoNotCheckImpl(), arguments.noCheckImpl);
        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, settings);
    }

    @Nullable
    private static LanguageFeature.State chooseCoroutinesApplicabilityLevel(
            @NotNull CompilerConfiguration configuration,
            @NotNull CommonCompilerArguments arguments
    ) {
        switch (arguments.coroutinesState) {
            case CommonCompilerArguments.ERROR:
                return LanguageFeature.State.ENABLED_WITH_ERROR;
            case CommonCompilerArguments.ENABLE:
                return LanguageFeature.State.ENABLED;
            case CommonCompilerArguments.WARN:
                return null;
            default:
                String message = "Invalid value of -Xcoroutines (should be: enable, warn or error): " + arguments.coroutinesState;
                configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(ERROR, message, null);
                return null;
        }
    }

    @Nullable
    private static LanguageVersion parseVersion(
            @NotNull CompilerConfiguration configuration, @Nullable String value, @NotNull String versionOf
    ) {
        if (value == null) return null;

        LanguageVersion version = LanguageVersion.fromVersionString(value);
        if (version != null) {
            return version;
        }

        List<String> versionStrings = ArraysKt.map(LanguageVersion.values(), LanguageVersion::getDescription);
        String message = "Unknown " + versionOf + " version: " + value + "\n" +
                         "Supported " + versionOf + " versions: " + StringsKt.join(versionStrings, ", ");
        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(ERROR, message, null);
        return null;
    }

    protected abstract void setupPlatformSpecificArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull A arguments, @NotNull Services services
    );

    private static void reportArgumentParseProblems(@NotNull MessageCollector collector, @NotNull ArgumentParseErrors errors) {
        for (String flag : errors.getUnknownExtraFlags()) {
            collector.report(STRONG_WARNING, "Flag is not supported by this version of the compiler: " + flag, null);
        }
        for (String argument : errors.getExtraArgumentsPassedInObsoleteForm()) {
            collector.report(STRONG_WARNING, "Advanced option value is passed in an obsolete form. Please use the '=' character " +
                                             "to specify the value: " + argument + "=...", null);
        }
        for (Map.Entry<String, String> argument : errors.getDuplicateArguments().entrySet()) {
            collector.report(STRONG_WARNING, "Argument " + argument.getKey() + " is passed multiple times. " +
                                             "Only the last value will be used: " + argument.getValue(), null);
        }
    }

    @NotNull
    protected abstract ExitCode doExecute(
            @NotNull A arguments,
            @NotNull CompilerConfiguration configuration,
            @NotNull Disposable rootDisposable
    );

    private void printVersionIfNeeded(@NotNull MessageCollector messageCollector, @NotNull A arguments) {
        if (arguments.version) {
            messageCollector.report(
                    CompilerMessageSeverity.INFO,
                    executableScriptFileName() + " " + KotlinCompilerVersion.VERSION +
                    " (JRE " + System.getProperty("java.runtime.version") + ")",
                    null
            );
        }
    }

    @NotNull
    public abstract String executableScriptFileName();

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
