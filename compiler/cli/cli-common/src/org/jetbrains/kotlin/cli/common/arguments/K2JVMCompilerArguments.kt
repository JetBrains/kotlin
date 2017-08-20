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

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.Jsr305State
import org.jetbrains.kotlin.config.JvmTarget

class K2JVMCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @Argument(value = "-d", valueDescription = "<directory|jar>", description = "Destination for generated class files")
    var destination: String? by FreezableVar(null)

    @Argument(value = "-classpath", shortName = "-cp", valueDescription = "<path>", description = "Paths where to find user class files")
    var classpath: String? by FreezableVar(null)

    @GradleOption(DefaultValues.BooleanFalseDefault::class)
    @Argument(value = "-include-runtime", description = "Include Kotlin runtime in to resulting .jar")
    var includeRuntime: Boolean by FreezableVar(false)

    @GradleOption(DefaultValues.StringNullDefault::class)
    @Argument(
            value = "-jdk-home",
            valueDescription = "<path>",
            description = "Path to JDK home directory to include into classpath, if differs from default JAVA_HOME"
    )
    var jdkHome: String? by FreezableVar(null)

    @GradleOption(DefaultValues.BooleanFalseDefault::class)
    @Argument(value = "-no-jdk", description = "Don't include Java runtime into classpath")
    var noJdk: Boolean by FreezableVar(false)

    @GradleOption(DefaultValues.BooleanTrueDefault::class)
    @Argument(value = "-no-stdlib", description = "Don't include Kotlin runtime into classpath")
    var noStdlib: Boolean by FreezableVar(false)

    @GradleOption(DefaultValues.BooleanTrueDefault::class)
    @Argument(value = "-no-reflect", description = "Don't include Kotlin reflection implementation into classpath")
    var noReflect: Boolean by FreezableVar(false)

    @Argument(value = "-script", description = "Evaluate the script file")
    var script: Boolean by FreezableVar(false)

    @Argument(
            value = "-script-templates",
            valueDescription = "<fully qualified class name[,]>",
            description = "Script definition template classes"
    )
    var scriptTemplates: Array<String>? by FreezableVar(null)

    @Argument(value = "-module-name", description = "Module name")
    var moduleName: String? by FreezableVar(null)

    @GradleOption(DefaultValues.JvmTargetVersions::class)
    @Argument(
            value = "-jvm-target",
            valueDescription = "<version>",
            description = "Target version of the generated JVM bytecode (1.6 or 1.8), default is 1.6"
    )
    var jvmTarget: String? by FreezableVar(JvmTarget.DEFAULT.description)

    @GradleOption(DefaultValues.BooleanFalseDefault::class)
    @Argument(value = "-java-parameters", description = "Generate metadata for Java 1.8 reflection on method parameters")
    var javaParameters: Boolean by FreezableVar(false)

    // Advanced options

    @Argument(value = "-Xmodule-path", valueDescription = "<path>", description = "Paths where to find Java 9+ modules")
    var javaModulePath: String? by FreezableVar(null)

    @Argument(
            value = "-Xadd-modules",
            valueDescription = "<module[,]>",
            description = "Root modules to resolve in addition to the initial modules,\n" +
                          "or all modules on the module path if <module> is ALL-MODULE-PATH"
    )
    var additionalJavaModules: Array<String>? by FreezableVar(null)

    @Argument(value = "-Xno-call-assertions", description = "Don't generate not-null assertion after each invocation of method returning not-null")
    var noCallAssertions: Boolean by FreezableVar(false)

    @Argument(value = "-Xno-param-assertions", description = "Don't generate not-null assertions on parameters of methods accessible from Java")
    var noParamAssertions: Boolean by FreezableVar(false)

    @Argument(value = "-Xno-optimize", description = "Disable optimizations")
    var noOptimize: Boolean by FreezableVar(false)

    @Argument(value = "-Xreport-perf", description = "Report detailed performance statistics")
    var reportPerf: Boolean by FreezableVar(false)

    @Argument(value = "-Xbuild-file", deprecatedName = "-module", valueDescription = "<path>", description = "Path to the .xml build file to compile")
    var buildFile: String? by FreezableVar(null)

    @Argument(value = "-Xmultifile-parts-inherit", description = "Compile multifile classes as a hierarchy of parts and facade")
    var inheritMultifileParts: Boolean by FreezableVar(false)

    @Argument(value = "-Xskip-runtime-version-check", description = "Allow Kotlin runtime libraries of incompatible versions in the classpath")
    var skipRuntimeVersionCheck: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xuse-old-class-files-reading",
            description = "Use old class files reading implementation " +
                          "(may slow down the build and should be used in case of problems with the new implementation)"
    )
    var useOldClassFilesReading: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xdump-declarations-to",
            valueDescription = "<path>",
            description = "Path to JSON file to dump Java to Kotlin declaration mappings"
    )
    var declarationsOutputPath: String? by FreezableVar(null)

    @Argument(value = "-Xsingle-module", description = "Combine modules for source files and binary dependencies into a single module")
    var singleModule: Boolean by FreezableVar(false)

    @Argument(value = "-Xadd-compiler-builtins", description = "Add definitions of built-in declarations to the compilation classpath (useful with -no-stdlib)")
    var addCompilerBuiltIns: Boolean by FreezableVar(false)

    @Argument(value = "-Xload-builtins-from-dependencies", description = "Load definitions of built-in declarations from module dependencies, instead of from the compiler")
    var loadBuiltInsFromDependencies: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xscript-resolver-environment",
            valueDescription = "<key=value[,]>",
            description = "Script resolver environment in key-value pairs (the value could be quoted and escaped)"
    )
    var scriptResolverEnvironment: Array<String>? by FreezableVar(null)

    // Javac options
    @Argument(value = "-Xuse-javac", description = "Use javac for Java source and class files analysis")
    var useJavac: Boolean by FreezableVar(false)

    @Argument(
            value = "-Xjavac-arguments",
            valueDescription = "<option[,]>",
            description = "Java compiler arguments")
    var javacArguments: Array<String>? by FreezableVar(null)

    @Argument(
            value = "-Xjsr305-annotations",
            valueDescription = "{ignore|enable}",
            description = "Specify global behavior for JSR-305 nullability annotations: ignore, or treat as other supported nullability annotations"
    )
    var jsr305GlobalReportLevel: String? by FreezableVar(Jsr305State.DEFAULT.description)

    // Paths to output directories for friend modules.
    var friendPaths: Array<String>? by FreezableVar(null)

    override fun configureAnalysisFlags(): MutableMap<AnalysisFlag<*>, Any> {
        val result = super.configureAnalysisFlags()
        Jsr305State.findByDescription(jsr305GlobalReportLevel)?.let {
            result.put(AnalysisFlag.loadJsr305Annotations, it)
        }
        return result
    }
}
