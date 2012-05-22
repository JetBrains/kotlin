/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jet.cli.jvm;

import com.sampullara.cli.Argument;
import org.jetbrains.jet.cli.common.CompilerArguments;

import java.util.List;

/**
 * Command line arguments for the {@link K2JVMCompiler}
 */
public class K2JVMCompilerArguments extends CompilerArguments {


    // TODO ideally we'd unify this with 'src' to just having a single field that supports multiple files/dirs
    private List<String> sourceDirs;

    public List<String> getSourceDirs() {
        return sourceDirs;
    }

    public void setSourceDirs(List<String> sourceDirs) {
        this.sourceDirs = sourceDirs;
    }

    @Argument(value = "jar", description = "jar file name")
    public String jar;

    @Argument(value = "src", description = "source file or directory")
    public String src;

    @Argument(value = "classpath", description = "classpath to use when compiling")
    public String classpath;

    @Argument(value = "includeRuntime", description = "include Kotlin runtime in to resulting jar")
    public boolean includeRuntime;

    @Argument(value = "stdlib", description = "Path to the stdlib.jar")
    public String stdlib;

    @Argument(value = "altHeaders", description = "Path to the alternative library headers paths")
    public String altHeaders;

    @Argument(value = "mode", description = "Special compiler modes: stubs or altHeaders")
    public String mode;

    @Argument(value = "output", description = "output directory")
    public String outputDir;

    @Argument(value = "module", description = "module to compile")
    public String module;

    @Argument(value = "script", description = "evaluate script")
    public boolean script;

    @Argument(value = "tags", description = "Demarcate each compilation message (error, warning, etc) with an open and close tag")
    public boolean tags;

    @Argument(value = "verbose", description = "Enable verbose logging output")
    public boolean verbose;

    @Argument(value = "version", description = "Display compiler version")
    public boolean version;

    @Argument(value = "help", alias = "h", description = "show help")
    public boolean help;

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    @Override
    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isIncludeRuntime() {
        return includeRuntime;
    }

    public void setIncludeRuntime(boolean includeRuntime) {
        this.includeRuntime = includeRuntime;
    }

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) {
        this.jar = jar;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getStdlib() {
        return stdlib;
    }

    public void setStdlib(String stdlib) {
        this.stdlib = stdlib;
    }

    @Override
    public boolean isTags() {
        return tags;
    }

    @Override
    public boolean isVersion() {
        return version;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    public void setTags(boolean tags) {
        this.tags = tags;
    }
}
