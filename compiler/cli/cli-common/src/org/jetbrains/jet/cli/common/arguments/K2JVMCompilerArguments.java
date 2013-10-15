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

/**
 * Command line arguments for the {@link K2JVMCompiler}
 */
@SuppressWarnings("UnusedDeclaration")
public class K2JVMCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "jar", description = "jar file name")
    public String jar;

    @Argument(value = "src", description = "source file or directory (allows many paths separated by the system path separator)")
    public String src;

    @Argument(value = "classpath", description = "classpath to use when compiling")
    public String classpath;

    @Argument(value = "annotations", description = "paths to external annotations")
    public String annotations;

    @Argument(value = "includeRuntime", description = "include Kotlin runtime in to resulting jar")
    public boolean includeRuntime;

    @Argument(value = "noJdk", description = "don't include Java runtime into classpath")
    public boolean noJdk;

    @Argument(value = "noStdlib", description = "don't include Kotlin runtime into classpath")
    public boolean noStdlib;

    @Argument(value = "noJdkAnnotations", description = "don't include JDK external annotations into classpath")
    public boolean noJdkAnnotations;

    @Argument(value = "notNullAssertions", description = "generate not-null assertion after each invokation of method returning not-null")
    public boolean notNullAssertions;

    @Argument(value = "notNullParamAssertions", description = "generate not-null assertions on parameters of methods accessible from Java")
    public boolean notNullParamAssertions;

    @Argument(value = "output", description = "output directory")
    public String outputDir;

    @Argument(value = "module", description = "module to compile")
    public String module;

    @Argument(value = "script", description = "evaluate script")
    public boolean script;

    @Argument(value = "kotlinHome", description = "Path to Kotlin compiler home directory, used for annotations and runtime libraries discovery")
    public String kotlinHome;
}
