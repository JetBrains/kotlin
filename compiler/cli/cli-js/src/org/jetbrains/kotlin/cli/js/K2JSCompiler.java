/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js;

import com.intellij.openapi.Disposable;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CLICompiler;
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArgumentsKt;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.KotlinPaths;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
    static String calculateSourceMapSourceRoot(
            @NotNull MessageCollector messageCollector,
            @NotNull K2JSCompilerArguments arguments
    ) {
        File commonPath = null;
        List<File> pathToRoot = new ArrayList<>();
        Map<File, Integer> pathToRootIndexes = new HashMap<>();

        try {
            for (String path : arguments.getFreeArgs()) {
                File file = new File(path).getCanonicalFile();
                if (commonPath == null) {
                    commonPath = file;

                    while (file != null) {
                        pathToRoot.add(file);
                        file = file.getParentFile();
                    }
                    Collections.reverse(pathToRoot);

                    for (int i = 0; i < pathToRoot.size(); ++i) {
                        pathToRootIndexes.put(pathToRoot.get(i), i);
                    }
                }
                else {
                    while (file != null) {
                        Integer existingIndex = pathToRootIndexes.get(file);
                        if (existingIndex != null) {
                            existingIndex = Math.min(existingIndex, pathToRoot.size() - 1);
                            pathToRoot.subList(existingIndex + 1, pathToRoot.size()).clear();
                            commonPath = pathToRoot.get(pathToRoot.size() - 1);
                            break;
                        }
                        file = file.getParentFile();
                    }
                    if (file == null) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            String text = ExceptionUtil.getThrowableText(e);
            messageCollector.report(CompilerMessageSeverity.ERROR, "IO error occurred calculating source root:\n" + text, null);
            return ".";
        }

        return commonPath != null ? commonPath.getPath() : ".";
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
