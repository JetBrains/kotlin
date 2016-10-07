/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.common.arguments;

import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;

public class K2JVMCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "d", description = "Destination for generated class files")
    @ValueDescription("<directory|jar>")
    public String destination;

    @Argument(value = "classpath", alias = "cp", description = "Paths where to find user class files")
    @ValueDescription("<path>")
    public String classpath;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "include-runtime", description = "Include Kotlin runtime in to resulting .jar")
    public boolean includeRuntime;

    @GradleOption(DefaultValues.StringNullDefault.class)
    @Argument(value = "jdk-home", description = "Path to JDK home directory to include into classpath, if differs from default JAVA_HOME")
    @ValueDescription("<path>")
    public String jdkHome;

    @GradleOption(DefaultValues.BooleanFalseDefault.class)
    @Argument(value = "no-jdk", description = "Don't include Java runtime into classpath")
    public boolean noJdk;

    @GradleOption(DefaultValues.BooleanTrueDefault.class)
    @Argument(value = "no-stdlib", description = "Don't include Kotlin runtime into classpath")
    public boolean noStdlib;

    @GradleOption(DefaultValues.BooleanTrueDefault.class)
    @Argument(value = "no-reflect", description = "Don't include Kotlin reflection implementation into classpath")
    public boolean noReflect;

    @Argument(value = "module", description = "Path to the module file to compile")
    @ValueDescription("<path>")
    public String module;

    @Argument(value = "script", description = "Evaluate the script file")
    public boolean script;

    @Argument(value = "script-templates", description = "Script definition template classes")
    @ValueDescription("<fully qualified class name[,]>")
    public String[] scriptTemplates;

    @Argument(value = "kotlin-home", description = "Path to Kotlin compiler home directory, used for runtime libraries discovery")
    @ValueDescription("<path>")
    public String kotlinHome;

    @Argument(value = "module-name", description = "Module name")
    public String moduleName;

    @GradleOption(DefaultValues.JvmTargetVersions.class)
    @Argument(value = "jvm-target", description = "Target version of the generated JVM bytecode (1.6 or 1.8), default is 1.6")
    @ValueDescription("<version>")
    public String jvmTarget;

    // Advanced options
    @Argument(value = "Xno-call-assertions", description = "Don't generate not-null assertion after each invocation of method returning not-null")
    public boolean noCallAssertions;

    @Argument(value = "Xno-param-assertions", description = "Don't generate not-null assertions on parameters of methods accessible from Java")
    public boolean noParamAssertions;

    @Argument(value = "Xno-optimize", description = "Disable optimizations")
    public boolean noOptimize;

    @Argument(value = "Xreport-perf", description = "Report detailed performance statistics")
    public boolean reportPerf;

    @Argument(value = "Xmultifile-parts-inherit", description = "Compile multifile classes as a hierarchy of parts and facade")
    public boolean inheritMultifileParts;

    @Argument(value = "Xallow-kotlin-package", description = "Allow compiling code in package 'kotlin'")
    public boolean allowKotlinPackage;

    @Argument(value = "Xskip-metadata-version-check", description = "Try loading binary incompatible classes, may cause crashes")
    public boolean skipMetadataVersionCheck;

    @Argument(value = "Xdump-declarations-to", description = "Path to JSON file to dump Java to Kotlin declaration mappings")
    @ValueDescription("<path>")
    public String declarationsOutputPath;

    @Argument(value = "Xload-script-configs", description = "Load script configuration files from project directory tree")
    public boolean loadScriptConfigs;

    @Argument(value = "Xsingle-module", description = "Combine modules for source files and binary dependencies into a single module")
    public boolean singleModule;

    @Argument(value = "Xinterface-compatibility", description = "Generate DefaultImpls classes for interfaces in JVM target bytecode version 1.8 for binary compatibility with 1.6")
    public boolean interfaceCompatibility;

    // Paths to output directories for friend modules.
    public String[] friendPaths;

    @Override
    @NotNull
    public String executableScriptFileName() {
        return "kotlinc-jvm";
    }

}
