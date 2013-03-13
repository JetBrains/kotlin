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

package org.jetbrains.jet.buildtools.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.jetbrains.jet.buildtools.core.BytecodeCompiler;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentException;

import java.io.File;

import static org.jetbrains.jet.buildtools.core.Util.getPath;


/**
 * Kotlin bytecode compiler Ant task.
 * <p/>
 * See
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 * http://evgeny-goldin.org/javadoc/ant/develop.html
 * http://svn.apache.org/viewvc/ant/core/trunk/src/main/org/apache/tools/ant/taskdefs/Javac.java?view=markup.
 */
public class BytecodeCompilerTask extends Task {

    private File output;
    private File jar;
    private File stdlib;
    private File src;
    private File module;
    private Path compileClasspath;
    private boolean includeRuntime = true;

    public void setOutput(File output) {
        this.output = output;
    }

    public void setJar(File jar) {
        this.jar = jar;
    }

    public void setStdlib(File stdlib) {
        this.stdlib = stdlib;
    }

    public void setSrc(File src) {
        this.src = src;
    }

    public void setModule(File module) {
        this.module = module;
    }

    public void setIncludeRuntime(boolean includeRuntime) {
        this.includeRuntime = includeRuntime;
    }


    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(Path classpath) {
        if (this.compileClasspath == null) {
            this.compileClasspath = classpath;
        }
        else {
            this.compileClasspath.append(classpath);
        }
    }


    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param ref a reference to a classpath.
     */
    public void setClasspathRef(Reference ref) {
        if (this.compileClasspath == null) {
            this.compileClasspath = new Path(getProject());
        }
        this.compileClasspath.createPath().setRefid(ref);
    }


    /**
     * Set the nested {@code <classpath>} to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void addConfiguredClasspath(Path classpath) {
        setClasspath(classpath);
    }


    @Override
    public void execute() {

        BytecodeCompiler compiler = new BytecodeCompiler();
        String stdlibPath = (this.stdlib != null ? getPath(this.stdlib) : null);
        String[] classpath = (this.compileClasspath != null ? this.compileClasspath.list() : null);

        if (this.src != null) {

            if ((this.output == null) && (this.jar == null)) {
                throw new CompileEnvironmentException("\"output\" or \"jar\" should be specified");
            }

            String source = getPath(this.src);
            String destination = getPath(this.output != null ? this.output : this.jar);

            log(String.format("Compiling [%s] => [%s]", source, destination));

            if (this.output != null) {
                compiler.sourcesToDir(source, destination, stdlibPath, classpath);
            }
            else {
                compiler.sourcesToJar(source, destination, this.includeRuntime, stdlibPath, classpath);
            }
        }
        else if (this.module != null) {

            if (this.output != null) {
                throw new CompileEnvironmentException("Module compilation is only supported for jar destination");
            }

            String modulePath = getPath(this.module);
            String jarPath = (this.jar != null ? getPath(this.jar) : null);

            log(jarPath != null ? String.format("Compiling [%s] => [%s]", modulePath, jarPath) :
                String.format("Compiling [%s]", modulePath));

            compiler.moduleToJar(modulePath, jarPath, this.includeRuntime, stdlibPath, classpath);
        }
        else {
            throw new CompileEnvironmentException("\"src\" or \"module\" should be specified");
        }
    }
}
