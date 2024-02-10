/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

class K2JVMCompilerArguments : CommonCompilerArguments() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }

    @Argument(value = "-d", valueDescription = "<directory|jar>", description = "Destination for generated class files.")
    var destination: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-classpath",
        shortName = "-cp",
        valueDescription = "<path>",
        description = "List of directories and JAR/ZIP archives to search for user class files."
    )
    var classpath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-include-runtime", description = "Include the Kotlin runtime in the resulting JAR.")
    var includeRuntime = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-jdk-home",
        valueDescription = "<path>",
        description = "Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'."
    )
    var jdkHome: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-no-jdk", description = "Don't automatically include the Java runtime in the classpath.")
    var noJdk = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-no-stdlib",
        description = "Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath."
    )
    var noStdlib = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-no-reflect", description = "Don't automatically include the Kotlin reflection dependency in the classpath.")
    var noReflect = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-expression",
        shortName = "-e",
        description = "Evaluate the given string as a Kotlin script."
    )
    var expression: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-script-templates",
        valueDescription = "<fully qualified class name[,]>",
        description = "Script definition template classes."
    )
    var scriptTemplates: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.STRING_NULL_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-module-name", valueDescription = "<name>", description = "Name of the generated '.kotlin_module' file.")
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.JVM_TARGET_VERSIONS,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-jvm-target",
        valueDescription = "<version>",
        description = "The target version of the generated JVM bytecode (${JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION}), with 1.8 as the default.",
    )
    var jvmTarget: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "-java-parameters", description = "Generate metadata for Java 1.8 reflection on method parameters.")
    var javaParameters = false
        set(value) {
            checkFrozen()
            field = value
        }

    // Advanced options

    @Argument(value = "-Xuse-old-backend", description = "Use the old JVM backend.")
    var useOldBackend = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-unstable-dependencies",
        description = "Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler."
    )
    var allowUnstableDependencies = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xabi-stability",
        valueDescription = "{stable|unstable}",
        description = """When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable
to prevent diagnostics from being reported when using stable compilers at the call site.
When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable
to force diagnostics to be reported."""
    )
    var abiStability: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-do-not-clear-binding-context",
        description = "When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings."
    )
    var doNotClearBindingContext = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xbackend-threads",
        valueDescription = "<N>",
        description = """When using the IR backend, run lowerings by file in N parallel threads.
0 means use one thread per processor core.
The default value is 1."""
    )
    var backendThreads: String = "1"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xmodule-path", valueDescription = "<path>", description = "Paths to Java 9+ modules.")
    var javaModulePath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xadd-modules",
        valueDescription = "<module[,]>",
        description = """Root modules to resolve in addition to the initial modules, or all modules on the module path if <module> is ALL-MODULE-PATH."""
    )
    var additionalJavaModules: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xno-call-assertions", description = "Don't generate not-null assertions for arguments of platform types.")
    var noCallAssertions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-receiver-assertions",
        description = "Don't generate not-null assertions for extension receiver arguments of platform types."
    )
    var noReceiverAssertions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-param-assertions",
        description = "Don't generate not-null assertions on parameters of methods accessible from Java."
    )
    var noParamAssertions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xno-optimize", description = "Disable optimizations.")
    var noOptimize = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xassertions", valueDescription = "{always-enable|always-disable|jvm|legacy}",
        description = """'kotlin.assert' call behavior:
-Xassertions=always-enable:  enable, ignore JVM assertion settings;
-Xassertions=always-disable: disable, ignore JVM assertion settings;
-Xassertions=jvm:            enable, depend on JVM assertion settings;
-Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package;
default: legacy"""
    )
    var assertionsMode: String? = JVMAssertionsMode.DEFAULT.description
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) JVMAssertionsMode.DEFAULT.description else value
        }

    @Argument(
        value = "-Xbuild-file",
        deprecatedName = "-module",
        valueDescription = "<path>",
        description = "Path to the .xml build file to compile."
    )
    var buildFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xmultifile-parts-inherit", description = "Compile multifile classes as a hierarchy of parts and a facade.")
    var inheritMultifileParts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xuse-type-table", description = "Use a type table in metadata serialization.")
    var useTypeTable = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-old-class-files-reading",
        description = """Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop.
This can be used in the event of problems with the new implementation."""
    )
    var useOldClassFilesReading = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fast-jar-file-system",
        description = "Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental."
    )
    var useFastJarFileSystem = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-missing-builtins-error",
        description = """Suppress the "cannot access built-in declaration" error (useful with '-no-stdlib')."""
    )
    var suppressMissingBuiltinsError = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xscript-resolver-environment",
        valueDescription = "<key=value[,]>",
        description = "Set the script resolver environment in key-value pairs (the value can be quoted and escaped)."
    )
    var scriptResolverEnvironment: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    // Javac options
    @Argument(value = "-Xuse-javac", description = "Use javac for Java source and class file analysis.")
    var useJavac = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xcompile-java", description = "Reuse 'javac' analysis and compile Java source files.")
    var compileJava = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjavac-arguments",
        valueDescription = "<option[,]>",
        description = "Java compiler arguments."
    )
    var javacArguments: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }


    @Argument(
        value = "-Xjava-source-roots",
        valueDescription = "<path>",
        description = "Paths to directories with Java source files."
    )
    var javaSourceRoots: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjava-package-prefix",
        description = "Package prefix for Java files."
    )
    var javaPackagePrefix: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjsr305",
        deprecatedName = "-Xjsr305-annotations",
        valueDescription = "{ignore/strict/warn}" +
                "|under-migration:{ignore/strict/warn}" +
                "|@<fq.name>:{ignore/strict/warn}",
        description = """Specify the behavior of 'JSR-305' nullability annotations:
-Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations)
-Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations
-Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name
Modes:
* ignore
* strict (experimental; treat like other supported nullability annotations)
* warn (report a warning)"""
    )
    var jsr305: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xnullability-annotations",
        valueDescription = "@<fq.name>:{ignore/strict/warn}",
        description = """Specify the behavior for specific Java nullability annotations (provided with fully qualified package name).
Modes:
* ignore
* strict
* warn (report a warning)"""
    )
    var nullabilityAnnotations: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsupport-compatqual-checker-framework-annotations",
        valueDescription = "enable|disable",
        description = """Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl').
The default value is 'enable'."""
    )
    var supportCompatqualCheckerFrameworkAnnotations: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjspecify-annotations",
        valueDescription = "ignore|strict|warn",
        description = """Specify the behavior of 'jspecify' annotations.
The default value is 'warn'."""
    )
    var jspecifyAnnotations: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjvm-default",
        valueDescription = "{all|all-compatibility|disable}",
        description = """Emit JVM default methods for interface declarations with bodies. The default is 'disable'.
-Xjvm-default=all                Generate JVM default methods for all interface declarations with bodies in the module.
                                 Do not generate 'DefaultImpls' stubs for interface declarations with bodies. If an interface inherits a method with a
                                 body from an interface compiled in 'disable' mode and doesn't override it, then a 'DefaultImpls' stub will be
                                 generated for it.
                                 This BREAKS BINARY COMPATIBILITY if some client code relies on the presence of 'DefaultImpls' classes.
                                 Note that if interface delegation is used, all interface methods are delegated.
-Xjvm-default=all-compatibility  Like 'all', but additionally generate compatibility stubs in the 'DefaultImpls' classes.
                                 Compatibility stubs can help library and runtime authors maintain backward binary compatibility
                                 for existing clients compiled against previous library versions.
                                 'all' and 'all-compatibility' modes change the library ABI surface that will be used by clients after
                                 the recompilation of the library. Because of this, clients might be incompatible with previous library
                                 versions. This usually means that proper library versioning is required, for example with major version increases in SemVer.
                                 In subtypes of Kotlin interfaces compiled in 'all' or 'all-compatibility' mode, 'DefaultImpls'
                                 compatibility stubs will invoke the default method of the interface with standard JVM runtime resolution semantics.
                                 Perform additional compatibility checks for classes inheriting generic interfaces where in some cases an
                                 additional implicit method with specialized signatures was generated in 'disable' mode.
                                 Unlike in 'disable' mode, the compiler will report an error if such a method is not overridden explicitly
                                 and the class is not annotated with '@JvmDefaultWithoutCompatibility' (see KT-39603 for more details).
-Xjvm-default=disable            Default behavior. Do not generate JVM default methods."""
    )
    var jvmDefault: String = JvmDefaultMode.DISABLE.description
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdefault-script-extension",
        valueDescription = "<script filename extension>",
        description = "Compile expressions and unrecognized scripts passed with the -script argument as scripts with the given filename extension."
    )
    var defaultScriptExtension: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xdisable-standard-script", description = "Disable standard Kotlin scripting support.")
    var disableStandardScript = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xgenerate-strict-metadata-version",
        description = "Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt')."
    )
    var strictMetadataVersionSemantics = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsanitize-parentheses",
        description = """Transform '(' and ')' in method names to some other character sequence.
This mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for
problems with parentheses in identifiers on certain platforms."""
    )
    var sanitizeParentheses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfriend-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for friend modules (modules whose internals should be visible)."
    )
    var friendPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-no-source-files",
        description = "Allow the set of source files to be empty."
    )
    var allowNoSourceFiles = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xemit-jvm-type-annotations",
        description = "Emit JVM type annotations in bytecode."
    )
    var emitJvmTypeAnnotations = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xstring-concat",
        valueDescription = "{indy-with-constants|indy|inline}",
        description = """Select the code generation scheme for string concatenation:
-Xstring-concat=indy-with-constants  Concatenate strings using 'invokedynamic' and 'makeConcatWithConstants'. This requires '-jvm-target 9' or greater.
-Xstring-concat=indy                 Concatenate strings using 'invokedynamic' and 'makeConcat'. This requires '-jvm-target 9' or greater.
-Xstring-concat=inline               Concatenate strings using 'StringBuilder'
default: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise."""

    )
    var stringConcat: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjdk-release",
        valueDescription = "<version>",
        description = """Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–21.
This also sets the value of '-jvm-target' to be equal to the selected JDK version."""
    )
    var jdkRelease: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }


    @Argument(
        value = "-Xsam-conversions",
        valueDescription = "{class|indy}",
        description = """Select the code generation scheme for SAM conversions.
-Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'. Requires '-jvm-target 1.8' or greater.
-Xsam-conversions=class         Generate SAM conversions as explicit classes"""
    )
    var samConversions: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xlambdas",
        valueDescription = "{class|indy}",
        description = """Select the code generation scheme for lambdas.
-Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'. This requires '-jvm-target 1.8' or greater.
                                A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'.
-Xlambdas=class                 Generate lambdas as explicit classes."""
    )
    var lambdas: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xklib",
        valueDescription = "<path>",
        description = "Paths to cross-platform libraries in the .klib format."
    )
    var klibLibraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xno-optimized-callable-references",
        description = "Don't use optimized callable reference superclasses, which have been available since 1.4."
    )
    var noOptimizedCallableReferences = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-kotlin-nothing-value-exception",
        description = "Don't use KotlinNothingValueException, which has been available since 1.4."
    )
    var noKotlinNothingValueException = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-reset-jar-timestamps",
        description = "Don't reset jar entry timestamps to a fixed date."
    )
    var noResetJarTimestamps = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-unified-null-checks",
        description = "Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details."
    )
    var noUnifiedNullChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-source-debug-extension",
        description = "Don't generate the '@kotlin.jvm.internal.SourceDebugExtension' annotation with an SMAP copy on classes."
    )
    var noSourceDebugExtension = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xprofile",
        valueDescription = "<profilerPath:command:outputDir>",
        description = """Debug option: Run the compiler with the async profiler and save snapshots to `outputDir`; `command` is passed to the async profiler on start.
`profilerPath` is the path to libasyncProfiler.so; async-profiler.jar should be on the compiler classpath.
If it's not on the classpath, the compiler will attempt to load async-profiler.jar from the containing directory of profilerPath.
Example: -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>"""
    )
    var profileCompilerCommand: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xrepeat",
        valueDescription = "<number>",
        description = "Debug option: Repeat module compilation <number> times."
    )
    var repeatCompileModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xuse-14-inline-classes-mangling-scheme",
        description = "Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30."
    )
    var useOldInlineClassesManglingScheme = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjvm-enable-preview",
        description = """Allow using Java features that are in the preview phase.
This works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments."""
    )
    var enableJvmPreview = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-deprecated-jvm-target-warning",
        description = """Suppress warnings about deprecated JVM target versions.
This option has no effect and will be deleted in a future version."""
    )
    var suppressDeprecatedJvmTargetWarning = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xtype-enhancement-improvements-strict-mode",
        description = """Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
including the ability to read type-use annotations from class files.
See KT-45671 for more details."""
    )
    var typeEnhancementImprovementsInStrictMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xserialize-ir",
        valueDescription = "{none|inline|all}",
        description = "Save the IR to metadata (Experimental)."
    )
    var serializeIr: String = "none"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalidate-ir",
        description = "Validate IR before and after lowering."
    )
    var validateIr = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalidate-bytecode",
        description = "Validate generated JVM bytecode before and after optimizations."
    )
    var validateBytecode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenhance-type-parameter-types-to-def-not-null",
        description = "Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any')."
    )
    var enhanceTypeParameterTypesToDefNotNull = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xlink-via-signatures",
        description = """Link JVM IR symbols via signatures instead of descriptors.
This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
This option is deprecated and will be deleted in future versions.
It has no effect when -language-version is 2.0 or higher."""
    )
    var linkViaSignatures = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdebug",
        description = """Enable debug mode for compilation.
Currently this includes spilling all variables in a suspending context regardless of whether they are alive."""
    )
    var enableDebugMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-new-java-annotation-targets",
        description = "Don't generate Java 1.8+ targets for Kotlin annotation classes."
    )
    var noNewJavaAnnotationTargets = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-old-innerclasses-logic",
        description = """Use the old logic for the generation of 'InnerClasses' attributes.
This option is deprecated and will be deleted in future versions."""
    )
    var oldInnerClassesLogic = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalue-classes",
        description = "Enable experimental value classes."
    )
    var valueClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-inliner",
        description = "Inline functions using the IR inliner instead of the bytecode inliner."
    )
    var enableIrInliner: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-kapt4",
        description = "Enable the experimental KAPT 4."
    )
    var useKapt4 = false
        set(value) {
            checkFrozen()
            field = value
        }

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        val result = super.configureAnalysisFlags(collector, languageVersion)
        result[JvmAnalysisFlags.strictMetadataVersionSemantics] = strictMetadataVersionSemantics
        result[JvmAnalysisFlags.javaTypeEnhancementState] = JavaTypeEnhancementStateParser(collector, languageVersion.toKotlinVersion())
            .parse(jsr305, supportCompatqualCheckerFrameworkAnnotations, jspecifyAnnotations, nullabilityAnnotations)
        result[AnalysisFlags.ignoreDataFlowInAssert] = JVMAssertionsMode.fromString(assertionsMode) != JVMAssertionsMode.LEGACY
        JvmDefaultMode.fromStringOrNull(jvmDefault)?.let {
            result[JvmAnalysisFlags.jvmDefaultMode] = it
        } ?: collector.report(
            CompilerMessageSeverity.ERROR,
            "Unknown -Xjvm-default mode: $jvmDefault, supported modes: ${JvmDefaultMode.values().map(JvmDefaultMode::description)}"
        )
        result[JvmAnalysisFlags.inheritMultifileParts] = inheritMultifileParts
        result[JvmAnalysisFlags.sanitizeParentheses] = sanitizeParentheses
        result[JvmAnalysisFlags.suppressMissingBuiltinsError] = suppressMissingBuiltinsError
        result[JvmAnalysisFlags.enableJvmPreview] = enableJvmPreview
        result[AnalysisFlags.allowUnstableDependencies] = allowUnstableDependencies
        result[JvmAnalysisFlags.useIR] = !useOldBackend
        return result
    }

    override fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
        val result = super.configureLanguageFeatures(collector)
        if (typeEnhancementImprovementsInStrictMode) {
            result[LanguageFeature.TypeEnhancementImprovementsInStrictMode] = LanguageFeature.State.ENABLED
        }
        if (enhanceTypeParameterTypesToDefNotNull) {
            result[LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated] = LanguageFeature.State.ENABLED
        }
        if (JvmDefaultMode.fromStringOrNull(jvmDefault)?.isEnabled == true) {
            result[LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride] = LanguageFeature.State.ENABLED
            result[LanguageFeature.AbstractClassMemberNotImplementedWithIntermediateAbstractClass] = LanguageFeature.State.ENABLED
        }
        if (valueClasses) {
            result[LanguageFeature.ValueClasses] = LanguageFeature.State.ENABLED
        }
        return result
    }

    override fun defaultLanguageVersion(collector: MessageCollector): LanguageVersion =
        if (useOldBackend) {
            if (!suppressVersionWarnings) {
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Language version is automatically inferred to ${LanguageVersion.KOTLIN_1_5.versionString} when using " +
                            "the old JVM backend. Consider specifying -language-version explicitly, or remove -Xuse-old-backend"
                )
            }
            LanguageVersion.KOTLIN_1_5
        } else super.defaultLanguageVersion(collector)

    override fun checkPlatformSpecificSettings(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        if (useOldBackend && languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_1_6) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Old JVM backend does not support language version 1.6 or above. " +
                        "Please use language version 1.5 or below, or remove -Xuse-old-backend"
            )
        }
        if (oldInnerClassesLogic) {
            collector.report(
                CompilerMessageSeverity.WARNING,
                "The -Xuse-old-innerclasses-logic option is deprecated and will be deleted in future versions."
            )
        }
    }

    override fun copyOf(): Freezable = copyK2JVMCompilerArguments(this, K2JVMCompilerArguments())
}
