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

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.*;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.KotlinPathsFromHomeDir;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.ANNOTATIONS_PATH_KEY;
import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.CLASSPATH_KEY;


/**
 * Wrapper class for Kotlin bytecode compiler.
 */
public class BytecodeCompiler {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private List<CompilerPlugin> compilerPlugins = new ArrayList<CompilerPlugin>();

    public BytecodeCompiler() {
    }


    /**
     * Creates new instance of {@link JetCoreEnvironment} instance using the arguments specified.
     *
     * @param stdlib    path to "kotlin-runtime.jar", only used if not null and not empty
     * @param classpath compilation classpath, only used if not null and not empty
     * @param sourceRoots
     * @return compile environment instance
     */
    private JetCoreEnvironment env(String stdlib, String[] classpath, String[] sourceRoots) {
        CompilerConfiguration configuration = createConfiguration(stdlib, classpath, sourceRoots);

        return new JetCoreEnvironment(CompileEnvironmentUtil.createMockDisposable(), configuration);
    }

    private CompilerConfiguration createConfiguration(String stdlib, String[] classpath, String[] sourceRoots) {
        KotlinPaths paths = getKotlinPathsForAntTask();
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.add(CLASSPATH_KEY, PathUtil.findRtJar());
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

        // lets register any compiler plugins
        configuration.addAll(CLIConfigurationKeys.COMPILER_PLUGINS, getCompilerPlugins());
        return configuration;
    }

    /**
     * Retrieves compilation error message.
     *
     * @param source          compilation source
     * @param exceptionThrown whether compilation failed due to exception thrown
     * @return compilation error message
     */
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
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param src       compilation source (directories or files)
     * @param output    compilation destination directory
     * @param stdlib    "kotlin-runtime.jar" path
     * @param classpath compilation classpath, can be <code>null</code> or empty
     */
    public void sourcesToDir(@NotNull String[] src, @NotNull String output, @Nullable String stdlib, @Nullable String[] classpath) {
        try {
            JetCoreEnvironment environment = env(stdlib, classpath, src);

            boolean success = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, null, new File(output), true);
            if (!success) {
                throw new CompileEnvironmentException(errorMessage(src, false));
            }
        }
        catch (Exception e) {
            throw new CompileEnvironmentException(errorMessage(src, true), e);
        }
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param src            compilation source (directory or file)
     * @param jar            compilation destination jar
     * @param includeRuntime whether Kotlin runtime library is included in destination jar
     * @param stdlib         "kotlin-runtime.jar" path
     * @param classpath      compilation classpath, can be <code>null</code> or empty
     */
    public void sourcesToJar(@NotNull String[] src,
            @NotNull String jar,
            boolean includeRuntime,
            @Nullable String stdlib,
            @Nullable String[] classpath) {
        try {
            JetCoreEnvironment environment = env(stdlib, classpath, src);

            boolean success = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, new File(jar), null, includeRuntime);
            if (!success) {
                throw new CompileEnvironmentException(errorMessage(src, false));
            }
        }
        catch (Exception e) {
            throw new CompileEnvironmentException(errorMessage(src, true), e);
        }
    }


    /**
     * {@code CompileEnvironment#compileModules} wrapper.
     *
     * @param module         compilation module file
     * @param jar            compilation destination jar
     * @param includeRuntime whether Kotlin runtime library is included in destination jar
     * @param stdlib         "kotlin-runtime.jar" path
     * @param classpath      compilation classpath, can be <code>null</code> or empty
     */
    public void moduleToJar(@NotNull String module,
            @NotNull String jar,
            boolean includeRuntime,
            @Nullable String stdlib,
            @Nullable String[] classpath) {
        try {
            ModuleChunk modules = CompileEnvironmentUtil.loadModuleDescriptions(getKotlinPathsForAntTask(), module,
                                                                                MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR);
            List<String> sourcesRoots = new ArrayList<String>();
            for (Module m : modules.getModules()) {
                sourcesRoots.addAll(m.getSourceFiles());
            }
            CompilerConfiguration configuration = createConfiguration(stdlib, classpath, sourcesRoots.toArray(new String[0]));
            File directory = new File(module).getParentFile();
            boolean success = KotlinToJVMBytecodeCompiler.compileModules(configuration, modules, directory, new File(jar), null, includeRuntime);
            if (!success) {
                throw new CompileEnvironmentException(errorMessage(new String[]{module}, false));
            }
        }
        catch (Exception e) {
            throw new CompileEnvironmentException(errorMessage(new String[]{module}, true), e);
        }
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }

    public void setCompilerPlugins(List<CompilerPlugin> compilerPlugins) {
        this.compilerPlugins = compilerPlugins;
    }

    private static KotlinPaths getKotlinPathsForAntTask() {
        return new KotlinPathsFromHomeDir(PathUtil.getJarPathForClass(BytecodeCompiler.class).getParentFile().getParentFile());
    }

}
