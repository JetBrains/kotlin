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

package org.jetbrains.jet.buildtools.core;

import jet.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.CompilerPlugin;
import org.jetbrains.jet.cli.common.util.CompilerPathUtil;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.*;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.codegen.BuiltinToJavaTypesMapping;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper class for Kotlin bytecode compiler.
 */
public class BytecodeCompiler {

    private List<CompilerPlugin> compilerPlugins = new ArrayList<CompilerPlugin>();

    public BytecodeCompiler () {
    }


    /**
     * Creates new instance of {@link org.jetbrains.jet.cli.jvm.compiler.K2JVMCompileEnvironmentConfiguration} instance using the arguments specified.
     *
     * @param stdlib    path to "kotlin-runtime.jar", only used if not null and not empty
     * @param classpath compilation classpath, only used if not null and not empty
     *
     * @return compile environment instance
     */
    private K2JVMCompileEnvironmentConfiguration env( String stdlib, String[] classpath ) {
        List<File> classpathItems = new ArrayList<File>();
        classpathItems.add(PathUtil.findRtJar());
        if ((stdlib != null) && (stdlib.trim().length() > 0)) {
            classpathItems.add(new File(stdlib));
        }
        else {
            classpathItems.add(CompilerPathUtil.getRuntimePath());
        }
        if ((classpath != null) && (classpath.length > 0)) {
            for (String path : classpath) {
                classpathItems.add(new File(path));
            }
        }
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.putUserData(JVMConfigurationKeys.CLASSPATH_KEY, classpathItems.toArray(new File[classpathItems.size()]));
        configuration.putUserData(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, new File[]{CompilerPathUtil.getJdkAnnotationsPath()});

        JetCoreEnvironment environment = new JetCoreEnvironment(CompileEnvironmentUtil.createMockDisposable(), configuration
        );
        K2JVMCompileEnvironmentConfiguration
                env = new K2JVMCompileEnvironmentConfiguration(environment, MessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR, false,
                                                               BuiltinsScopeExtensionMode.ALL, false, BuiltinToJavaTypesMapping.ENABLED);

        // lets register any compiler plugins
        env.getCompilerPlugins().addAll(getCompilerPlugins());

        return env;
    }


    /**
     * Retrieves compilation error message.
     *
     * @param  source          compilation source
     * @param  exceptionThrown whether compilation failed due to exception thrown
     *
     * @return compilation error message
     */
    private static String errorMessage( @NotNull String source, boolean exceptionThrown ) {
        return String.format( "[%s] compilation failed" +
                              ( exceptionThrown ? "" : ", see \"ERROR:\" messages above for more details." ),
                              new File( source ).getAbsolutePath());
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param src       compilation source (directory or file)
     * @param output    compilation destination directory
     * @param stdlib    "kotlin-runtime.jar" path
     * @param classpath compilation classpath, can be <code>null</code> or empty
     */
    public void sourcesToDir ( @NotNull String src, @NotNull String output, @Nullable String stdlib, @Nullable String[] classpath ) {
        try {
            K2JVMCompileEnvironmentConfiguration configuration = env(stdlib, classpath);
            configuration.getEnvironment().addSources(src);

            boolean success = KotlinToJVMBytecodeCompiler.compileBunchOfSources(configuration, null, new File(output), true);
            if ( ! success ) {
                throw new CompileEnvironmentException( errorMessage( src, false ));
            }
        }
        catch ( Exception e ) {
            throw new CompileEnvironmentException( errorMessage( src, true ), e );
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
    public void sourcesToJar ( @NotNull String src, @NotNull String jar, boolean includeRuntime, @Nullable String stdlib, @Nullable String[] classpath ) {
        try {
            K2JVMCompileEnvironmentConfiguration configuration = env(stdlib, classpath);
            configuration.getEnvironment().addSources(src);

            boolean success = KotlinToJVMBytecodeCompiler.compileBunchOfSources(configuration, new File(jar), null, includeRuntime);
            if ( ! success ) {
                throw new CompileEnvironmentException( errorMessage( src, false ));
            }
        }
        catch ( Exception e ) {
            throw new CompileEnvironmentException( errorMessage( src, true ), e );
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
    public void moduleToJar ( @NotNull String module, @NotNull String jar, boolean includeRuntime, @Nullable String stdlib, @Nullable String[] classpath ) {
        try {
            K2JVMCompileEnvironmentConfiguration env = env(stdlib, classpath);
            List<Module> modules = CompileEnvironmentUtil.loadModuleScript(module, env.getMessageCollector());
            File directory = new File(module).getParentFile();
            boolean success = KotlinToJVMBytecodeCompiler.compileModules(env, modules, directory, new File(jar), null, includeRuntime);
            if ( ! success ) {
                throw new CompileEnvironmentException( errorMessage( module, false ));
            }
        }
        catch ( Exception e ) {
            throw new CompileEnvironmentException( errorMessage( module, true ), e );
        }
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }

    public void setCompilerPlugins(List<CompilerPlugin> compilerPlugins) {
        this.compilerPlugins = compilerPlugins;
    }
}
