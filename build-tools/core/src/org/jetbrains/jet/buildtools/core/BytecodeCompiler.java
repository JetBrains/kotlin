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

import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;
import org.jetbrains.jet.compiler.CompilerPlugin;

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
     * Creates new instance of {@link CompileEnvironment} instance using the arguments specified.
     *
     * @param stdlib    path to "kotlin-runtime.jar", only used if not null and not empty
     * @param classpath compilation classpath, only used if not null and not empty
     *
     * @return compile environment instance
     */
    private CompileEnvironment env( String stdlib, String[] classpath ) {
        CompileEnvironment env = new CompileEnvironment();

        if (( stdlib != null ) && ( stdlib.trim().length() > 0 )) {
            env.setStdlib( stdlib );
        }

        if (( classpath != null ) && ( classpath.length > 0 )) {
            env.addToClasspath( classpath );
        }

        // lets register any compiler plugins
        env.getMyEnvironment().getCompilerPlugins().addAll(getCompilerPlugins());

        return env;
    }


    /**
     * Retrieves compilation error message.
     * @param  source compilation source
     * @return compilation error message
     */
    private String compilationError ( String source ) {
        return String.format( "[%s] compilation failed, see \"ERROR:\" messages above for more details.", source );
    }


    /**
     * {@code CompileEnvironment#compileBunchOfSources} wrapper.
     *
     * @param src       compilation source (directory or file)
     * @param output    compilation destination directory
     * @param stdlib    "kotlin-runtime.jar" path
     * @param classpath compilation classpath, can be <code>null</code> or empty
     */
    public void sourcesToDir ( String src, String output, String stdlib, String[] classpath ) {
        boolean success = env( stdlib, classpath ).compileBunchOfSources( src, null, output, true /* Last arg is ignored anyway */ );
        if ( ! success ) {
            throw new CompileEnvironmentException( compilationError( src ));
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
    public void sourcesToJar ( String src, String jar, boolean includeRuntime, String stdlib, String[] classpath ) {
        boolean success = env( stdlib, classpath ).compileBunchOfSources( src, jar, null, includeRuntime );
        if ( ! success ) {
            throw new CompileEnvironmentException( compilationError( src ));
        }
    }


    /**
     * {@code CompileEnvironment#compileModuleScript} wrapper.
     *
     * @param module         compilation module file
     * @param jar            compilation destination jar
     * @param includeRuntime whether Kotlin runtime library is included in destination jar
     * @param stdlib         "kotlin-runtime.jar" path
     * @param classpath      compilation classpath, can be <code>null</code> or empty
     */
    public void moduleToJar ( String module, String jar, boolean includeRuntime, String stdlib, String[] classpath ) {
        env( stdlib, classpath ).compileModuleScript( module, jar, null, includeRuntime );
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        return compilerPlugins;
    }

    public void setCompilerPlugins(List<CompilerPlugin> compilerPlugins) {
        this.compilerPlugins = compilerPlugins;
    }
}
