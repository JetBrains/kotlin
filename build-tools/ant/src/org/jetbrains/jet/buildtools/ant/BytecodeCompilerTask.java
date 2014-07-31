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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.jetbrains.jet.buildtools.core.BytecodeCompiler;
import org.jetbrains.jet.buildtools.core.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private File stdlib;
    private Path src;
    private Path externalAnnotations;
    private Path compileClasspath;
    private boolean includeRuntime = true;
    private final List<Commandline.Argument> additionalArguments = new ArrayList<Commandline.Argument>();

    public void setOutput(File output) {
        this.output = output;
    }

    public void setStdlib(File stdlib) {
        this.stdlib = stdlib;
    }

    public void setSrc(Path src) {
        this.src = src;
    }

    public Path createSrc() {
        if (src == null) {
            src = new Path(getProject());
        }
        return src.createPath();
    }

    public void setExternalAnnotations(Path externalAnnotations) {
        this.externalAnnotations = externalAnnotations;
    }

    public Path createExternalAnnotations() {
        if (externalAnnotations == null) {
            externalAnnotations = new Path(getProject());
        }
        return externalAnnotations.createPath();
    }

    public void setIncludeRuntime(boolean includeRuntime) {
        this.includeRuntime = includeRuntime;
    }

    public Commandline.Argument createCompilerArg() {
        Commandline.Argument argument = new Commandline.Argument();
        additionalArguments.add(argument);
        return argument;
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
        String stdlibPath = stdlib != null ? getPath(stdlib) : null;
        String[] classpath = compileClasspath != null ? compileClasspath.list() : null;
        String[] externalAnnotationsPath = externalAnnotations != null ? externalAnnotations.list() : null;

        List<String> args = new ArrayList<String>();
        for (Commandline.Argument argument : additionalArguments) {
            args.addAll(Arrays.asList(argument.getParts()));
        }

        if (src == null) {
            throw new BuildException("\"src\" should be specified");
        }
        if (output == null) {
            throw new BuildException("\"output\" should be specified");
        }

        String[] source = Util.getPaths(src.list());
        String destination = getPath(output);

        log(String.format("Compiling [%s] => [%s]", Arrays.toString(source), destination));
        BytecodeCompiler.compileSources(source, destination, includeRuntime, stdlibPath, classpath, externalAnnotationsPath, args);
    }
}
