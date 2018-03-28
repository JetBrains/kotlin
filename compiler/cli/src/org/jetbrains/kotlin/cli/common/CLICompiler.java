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
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.cli.jvm.compiler.CompilerJarLocator;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.progress.CompilationCanceledException;
import org.jetbrains.kotlin.progress.CompilationCanceledStatus;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.cli.common.ExitCode.*;
import static org.jetbrains.kotlin.cli.common.environment.UtilKt.setIdeaIoUseFallback;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*;

public abstract class CLICompiler<A extends CommonCompilerArguments> extends CLITool<A> {

    public static String KOTLIN_HOME_PROPERTY = "kotlin.home";
    public static String KOTLIN_HOME_ENV_VAR = "KOTLIN_HOME";

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

    @NotNull
    @Override
    public ExitCode execImpl(@NotNull MessageCollector messageCollector, @NotNull Services services, @NotNull A arguments) {
        GroupingMessageCollector groupingCollector = new GroupingMessageCollector(messageCollector, arguments.getAllWarningsAsErrors());

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, groupingCollector);

        try {
            setupCommonArgumentsAndServices(configuration, arguments, services);
            setupPlatformSpecificArgumentsAndServices(configuration, arguments, services);
            KotlinPaths paths = computeKotlinPaths(groupingCollector, arguments);
            if (groupingCollector.hasErrors()) {
                return ExitCode.COMPILATION_ERROR;
            }

            CompilationCanceledStatus canceledStatus = services.get(CompilationCanceledStatus.class);
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus);

            Disposable rootDisposable = Disposer.newDisposable();
            try {
                setIdeaIoUseFallback();
                ExitCode code = doExecute(arguments, configuration, rootDisposable, paths);
                return groupingCollector.hasErrors() ? COMPILATION_ERROR : code;
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
        catch (AnalysisResult.CompilationErrorException e) {
            return COMPILATION_ERROR;
        }
        catch (Throwable t) {
            MessageCollectorUtil.reportException(groupingCollector, t);
            return INTERNAL_ERROR;
        }
        finally {
            groupingCollector.flush();
        }
    }

    private void setupCommonArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull A arguments, @NotNull Services services
    ) {
        if (arguments.getNoInline()) {
            configuration.put(CommonConfigurationKeys.DISABLE_INLINE, true);
        }
        if (arguments.getIntellijPluginRoot()!= null) {
            configuration.put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, arguments.getIntellijPluginRoot());
        }
        if (arguments.getReportOutputFiles()) {
            configuration.put(CommonConfigurationKeys.REPORT_OUTPUT_FILES, true);
        }
        @SuppressWarnings("deprecation")
        CompilerJarLocator locator = services.get(CompilerJarLocator.class);
        if (locator != null) {
            configuration.put(CLIConfigurationKeys.COMPILER_JAR_LOCATOR, locator);
        }

        setupLanguageVersionSettings(configuration, arguments);
    }

    private void setupLanguageVersionSettings(@NotNull CompilerConfiguration configuration, @NotNull A arguments) {

        MessageCollector collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, arguments.configureLanguageVersionSettings(collector));
    }

    private static final String kotlinHomeEnvVar = System.getenv(KOTLIN_HOME_ENV_VAR);

    @Nullable
    private static KotlinPaths computeKotlinPaths(@NotNull MessageCollector messageCollector, @NotNull CommonCompilerArguments arguments) {
        KotlinPaths paths;
        String kotlinHomeProperty = System.getProperty(KOTLIN_HOME_PROPERTY);
        File kotlinHome =
                arguments.getKotlinHome() != null ? new File(arguments.getKotlinHome()) :
                kotlinHomeProperty != null        ? new File(kotlinHomeProperty) :
                kotlinHomeEnvVar != null          ? new File(kotlinHomeEnvVar)
                                                  : null;
        if (kotlinHome != null) {
            if (kotlinHome.isDirectory()) {
                paths = new KotlinPathsFromHomeDir(kotlinHome);
            }
            else {
                messageCollector.report(ERROR, "Kotlin home does not exist or is not a directory: " + kotlinHome, null);
                paths = null;
            }
        }
        else {
            paths = PathUtil.getKotlinPathsForCompiler();
        }

        if (paths != null) {
            messageCollector.report(LOGGING, "Using Kotlin home directory " + paths.getHomePath(), null);
        }

        return paths;
    }

    @Nullable
    public static File getLibraryFromHome(
            @Nullable KotlinPaths paths,
            @NotNull Function1<KotlinPaths, File> getLibrary,
            @NotNull String libraryName,
            @NotNull MessageCollector messageCollector,
            @NotNull String noLibraryArgument
    ) {
        if (paths != null) {
            File stdlibJar = getLibrary.invoke(paths);
            if (stdlibJar.exists()) {
                return stdlibJar;
            }
        }

        messageCollector.report(STRONG_WARNING, "Unable to find " + libraryName + " in the Kotlin home directory. " +
                                                "Pass either " + noLibraryArgument + " to prevent adding it to the classpath, " +
                                                "or the correct '-kotlin-home'", null);
        return null;
    }

    protected abstract void setupPlatformSpecificArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull A arguments, @NotNull Services services
    );

    @NotNull
    protected abstract ExitCode doExecute(
            @NotNull A arguments,
            @NotNull CompilerConfiguration configuration,
            @NotNull Disposable rootDisposable,
            @Nullable KotlinPaths paths
    );
}
