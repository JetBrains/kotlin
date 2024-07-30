/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArgumentsKt;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.KotlinPaths;

import java.util.List;

import static org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class K2JSCompiler extends CLICompiler<K2JSCompilerArguments> {
    private K2JsIrCompiler irCompiler = null;

    @NotNull
    private K2JsIrCompiler getIrCompiler() {
        if (irCompiler == null)
            irCompiler = new K2JsIrCompiler();
        return irCompiler;
    }

    @Override
    protected void addPlatformOptions(@NotNull List<String> $self, @NotNull K2JSCompilerArguments arguments) {}

    public static void main(String... args) {
        doMain(new K2JSCompiler(), args);
    }

    private final K2JSCompilerPerformanceManager performanceManager = new K2JSCompilerPerformanceManager();

    @NotNull
    @Override
    public K2JSCompilerArguments createArguments() {
        return new K2JSCompilerArguments();
    }

    @NotNull
    @Override
    protected ExitCode doExecute(
            @NotNull K2JSCompilerArguments arguments,
            @NotNull CompilerConfiguration configuration,
            @NotNull Disposable rootDisposable,
            @Nullable KotlinPaths paths
    ) {
        MessageCollector messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY);

        boolean useFir = Boolean.TRUE.equals(configuration.get(CommonConfigurationKeys.USE_FIR));
        if (K2JSCompilerArgumentsKt.isIrBackendEnabled(arguments) || useFir) {
           return getIrCompiler().doExecute(arguments, configuration.copy(), rootDisposable, paths);
        }

        messageCollector.report(ERROR, "Old Kotlin/JS compiler is no longer supported. Please migrate to the new JS IR backend", null);
        return COMPILATION_ERROR;
    }

    @Override
    protected void setupPlatformSpecificArgumentsAndServices(
            @NotNull CompilerConfiguration configuration, @NotNull K2JSCompilerArguments arguments,
            @NotNull Services services
    ) {
        if (K2JSCompilerArgumentsKt.isIrBackendEnabled(arguments)) {
            getIrCompiler().setupPlatformSpecificArgumentsAndServices(configuration, arguments, services);
        }
    }

    @NotNull
    @Override
    public CommonCompilerPerformanceManager getDefaultPerformanceManager() {
        return performanceManager;
    }

    @NotNull
    @Override
    public String executableScriptFileName() {
        return "kotlinc-js";
    }

    @NotNull
    @Override
    protected BinaryVersion createMetadataVersion(@NotNull int[] versionArray) {
        return new JsMetadataVersion(versionArray);
    }

    private static final class K2JSCompilerPerformanceManager extends CommonCompilerPerformanceManager {
        public K2JSCompilerPerformanceManager() {
            super("Kotlin to JS Compiler");
        }
    }
}
