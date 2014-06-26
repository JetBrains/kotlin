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

package org.jetbrains.jet.cli.common.arguments;

import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;

/**
 * Command line arguments for K2JVMCompiler
 */
@SuppressWarnings("UnusedDeclaration")
public class K2JVMCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "src", description = "Source file or directory (allows many paths separated by the system path separator)")
    @ValueDescription("<path>")
    public String src;

    @Argument(value = "jar", description = "Resulting .jar file path")
    @ValueDescription("<path>")
    public String jar;

    @Argument(value = "output", description = "Output directory path for .class files")
    @ValueDescription("<path>")
    public String outputDir;

    @Argument(value = "classpath", description = "Paths where to find user class files")
    @ValueDescription("<path>")
    public String classpath;

    @Argument(value = "annotations", description = "Paths to external annotations")
    @ValueDescription("<path>")
    public String annotations;

    @Argument(value = "includeRuntime", description = "Include Kotlin runtime in to resulting .jar")
    public boolean includeRuntime;

    @Argument(value = "noJdk", description = "Don't include Java runtime into classpath")
    public boolean noJdk;

    @Argument(value = "noStdlib", description = "Don't include Kotlin runtime into classpath")
    public boolean noStdlib;

    @Argument(value = "noJdkAnnotations", description = "Don't include JDK external annotations into classpath")
    public boolean noJdkAnnotations;

    @Argument(value = "notNullAssertions", description = "Generate not-null assertion after each invocation of method returning not-null")
    public boolean notNullAssertions;

    @Argument(value = "notNullParamAssertions", description = "Generate not-null assertions on parameters of methods accessible from Java")
    public boolean notNullParamAssertions;

    @Argument(value = "module", description = "Path to the module file to compile")
    @ValueDescription("<path>")
    public String module;

    @Argument(value = "script", description = "Evaluate the script file")
    public boolean script;

    @Argument(value = "kotlinHome", description = "Path to Kotlin compiler home directory, used for annotations and runtime libraries discovery")
    @ValueDescription("<path>")
    public String kotlinHome;

    @Argument(value = "inline", description = "Inlining mode (default is on)")
    @ValueDescription("{on,off}")
    public String inline;

    @Override
    @NotNull
    public String executableScriptFileName() {
        return "kotlinc-jvm";
    }
}
