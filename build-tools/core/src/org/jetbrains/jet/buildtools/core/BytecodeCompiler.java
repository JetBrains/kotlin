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

package org.jetbrains.jet.buildtools.core;

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.sampullara.cli.Args;
import org.apache.tools.ant.BuildException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentException;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.KotlinPathsFromHomeDir;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.ANNOTATIONS_PATH_KEY;
import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.CLASSPATH_KEY;

public class BytecodeCompiler {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @NotNull
    private static CompilerConfiguration createConfiguration(
            @Nullable String stdlib,
            @Nullable String[] classpath,
            @Nullable String[] externalAnnotationsPath,
            @NotNull String[] sourceRoots,
            @NotNull List<String> args
    ) {
        KotlinPaths paths = getKotlinPathsForAntTask();
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addAll(CLASSPATH_KEY, PathUtil.getJdkClassesRoots());
        if ((stdlib != null) && (stdlib.trim().length() > 0)) {
            configuration.add(CLASSPATH_KEY, new File(stdlib));
        }
        else {
            File path = paths.getRuntimePath();
            if (path.exists()) {
                configuration.add(CLASSPATH_KEY, path);
            }
        }
        if ((classpath != null) && (classpath.length > 0)) {
            for (String path : classpath) {
                configuration.add(CLASSPATH_KEY, new File(path));
            }
        }
        if ((externalAnnotationsPath != null) && (externalAnnotationsPath.length > 0)) {
            for (String path : externalAnnotationsPath) {
                configuration.add(ANNOTATIONS_PATH_KEY, new File(path));
            }
        }
        File jdkAnnotationsPath = paths.getJdkAnnotationsPath();
        if (jdkAnnotationsPath.exists()) {
            configuration.add(ANNOTATIONS_PATH_KEY, jdkAnnotationsPath);
        }

        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, Arrays.asList(sourceRoots));
        for (String sourceRoot : sourceRoots) {
            File file = new File(sourceRoot);
            if (!file.isFile() || !"kt".equals(FileUtilRt.getExtension(file.getName()))) {
                configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, file);
            }
        }
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR);

        // TODO: use K2JVMCompiler directly, don't duplicate this code here
        K2JVMCompilerArguments arguments = new K2JVMCompilerArguments();
        try {
            Args.parse(arguments, ArrayUtil.toStringArray(args));
        }
        catch (IllegalArgumentException e) {
            throw new BuildException(e.getMessage());
        }

        K2JVMCompiler.putAdvancedOptions(configuration, arguments);

        return configuration;
    }

    private static String errorMessage(@NotNull String[] source, boolean exceptionThrown) {
        return String.format("Compilation of the following source roots failed:" + LINE_SEPARATOR +
                             getAbsolutePaths(source) +
                             (exceptionThrown ? "" : LINE_SEPARATOR + "see \"ERROR:\" messages above for more details."));
    }

    private static String getAbsolutePaths(String[] source) {
        return StringUtil.join(
                source,
                new Function<String, String>() {
                    @Override
                    public String fun(String s) {
                        return " * " + new File(s).getAbsolutePath();
                    }
                },
                LINE_SEPARATOR
        );
    }

    /**
     * {@code KotlinToJVMBytecodeCompiler#compileBunchOfSources} wrapper.
     * @param src            compilation source (directory or file)
     * @param destination    compilation destination (directory or jar)
     * @param includeRuntime whether Kotlin runtime library is included in destination jar
     * @param stdlib         "kotlin-runtime.jar" path
     * @param args           additional command line arguments to Kotlin compiler
     */
    public static void compileSources(
            @NotNull String[] src,
            @NotNull String destination,
            boolean includeRuntime,
            @Nullable String stdlib,
            @Nullable String[] classpath,
            @Nullable String[] externalAnnotationsPath,
            @NotNull List<String> args
    ) {
        try {
            JetCoreEnvironment environment = JetCoreEnvironment.createForProduction(
                    Disposer.newDisposable(),
                    createConfiguration(stdlib, classpath, externalAnnotationsPath, src, args)
            );

            // TODO: use K2JVMCompiler directly, don't duplicate this code here
            boolean isJar = destination.endsWith(".jar");
            File jar = isJar ? new File(destination) : null;
            File outputDir = isJar ? null : new File(destination);

            boolean success = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, jar, outputDir, includeRuntime);
            if (!success) {
                throw new CompileEnvironmentException(errorMessage(src, false));
            }
        }
        catch (BuildException e) {
            throw e;
        }
        catch (CompileEnvironmentException e) {
            throw e;
        }
        catch (Exception e) {
            throw new CompileEnvironmentException(errorMessage(src, true), e);
        }
    }

    private static KotlinPaths getKotlinPathsForAntTask() {
        return new KotlinPathsFromHomeDir(PathUtil.getJarPathForClass(BytecodeCompiler.class).getParentFile().getParentFile());
    }
}
