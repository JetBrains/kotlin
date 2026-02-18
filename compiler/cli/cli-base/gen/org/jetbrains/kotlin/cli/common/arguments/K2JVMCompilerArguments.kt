/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.config.LanguageFeature

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/JvmCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

class K2JVMCompilerArguments : CommonCompilerArguments() {
    @Argument(
        value = "-Xabi-stability",
        valueDescription = "{stable|unstable}",
        description = """When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable
to prevent diagnostics from being reported when using stable compilers at the call site.
When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable
to force diagnostics to be reported.""",
    )
    var abiStability: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xadd-modules",
        valueDescription = "<module[,]>",
        description = "Root modules to resolve in addition to the initial modules, or all modules on the module path if <module> is ALL-MODULE-PATH.",
    )
    var additionalJavaModules: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-no-source-files",
        description = "Allow the set of source files to be empty.",
    )
    var allowNoSourceFiles: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-unstable-dependencies",
        description = "Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.",
    )
    var allowUnstableDependencies: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xannotations-in-metadata",
        description = "Write annotations on declarations into the metadata (in addition to the JVM bytecode), and read annotations from the metadata if they are present.",
    )
    @Enables(LanguageFeature.AnnotationsInMetadata)
    var annotationsInMetadata: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xassertions",
        valueDescription = "{always-enable|always-disable|jvm|legacy}",
        description = """'kotlin.assert' call behavior:
-Xassertions=always-enable:  enable, ignore JVM assertion settings;
-Xassertions=always-disable: disable, ignore JVM assertion settings;
-Xassertions=jvm:            enable, depend on JVM assertion settings;
-Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package;
default: legacy""",
    )
    var assertionsMode: String? = "legacy"
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) "legacy" else value
        }

    @Argument(
        value = "-Xbackend-threads",
        valueDescription = "<N>",
        description = """Run codegen phase in N parallel threads.
0 means use one thread per processor core.
The default value is 1.""",
    )
    var backendThreads: String = "1"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xbuild-file",
        deprecatedName = "-module",
        valueDescription = "<path>",
        description = "Path to the .xml build file to compile.",
    )
    var buildFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xdebug",
        description = """Enable debug mode for compilation.
Currently this includes spilling all variables in a suspending context regardless of whether they are alive.
If API Level >= 2.2 -- no-op.""",
    )
    var enableDebugMode: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdefault-script-extension",
        valueDescription = "<script filename extension>",
        description = "Compile expressions and unrecognized scripts passed with the -script argument as scripts with the given filename extension.",
    )
    var defaultScriptExtension: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xdisable-standard-script",
        description = "Disable standard Kotlin scripting support.",
    )
    var disableStandardScript: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xemit-jvm-type-annotations",
        description = "Emit JVM type annotations in bytecode.",
    )
    var emitJvmTypeAnnotations: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenhance-type-parameter-types-to-def-not-null",
        description = "Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').",
    )
    @Enables(LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated)
    var enhanceTypeParameterTypesToDefNotNull: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenhanced-coroutines-debugging",
        description = """Generate additional linenumber instruction for compiler-generated code
inside suspend functions and lambdas to distinguish them from user code by debugger.""",
    )
    var enhancedCoroutinesDebugging: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfriend-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for friend modules (modules whose internals should be visible).",
    )
    var friendPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xgenerate-strict-metadata-version",
        description = "Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt').",
    )
    var strictMetadataVersionSemantics: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xignored-annotations-for-bridges",
        valueDescription = "<fq.name>|*",
        description = "Do not copy these annotations to the bridge methods from their targets.",
    )
    var ignoredAnnotationsForBridges: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xindy-allow-annotated-lambdas",
        description = "Allow using 'invokedynamic' for lambda expressions with annotations",
    )
    var indyAllowAnnotatedLambdas: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-do-not-clear-binding-context",
        description = "When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.",
    )
    var doNotClearBindingContext: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjava-package-prefix",
        description = "Package prefix for Java files.",
    )
    var javaPackagePrefix: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjava-source-roots",
        valueDescription = "<path>",
        description = "Paths to directories with Java source files.",
    )
    var javaSourceRoots: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjdk-release",
        valueDescription = "<version>",
        description = """Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–25.
This also sets the value of '-jvm-target' to be equal to the selected JDK version.""",
    )
    var jdkRelease: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjspecify-annotations",
        valueDescription = "ignore|strict|warn",
        description = """Specify the behavior of 'jspecify' annotations.
The default value is 'strict'.""",
    )
    var jspecifyAnnotations: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjsr305",
        deprecatedName = "-Xjsr305-annotations",
        valueDescription = "{ignore/strict/warn}|under-migration:{ignore/strict/warn}|@<fq.name>:{ignore/strict/warn}",
        description = """Specify the behavior of 'JSR-305' nullability annotations:
-Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations)
-Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations
-Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name
Modes:
* ignore
* strict (experimental; treat like other supported nullability annotations)
* warn (report a warning)""",
    )
    var jsr305: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Deprecated("This flag is deprecated. Use `-jvm-default` instead")
    @Argument(
        value = "-Xjvm-default",
        valueDescription = "{all|all-compatibility|disable}",
        description = """This option is deprecated. Migrate to -jvm-default as follows:
-Xjvm-default=disable            -> -jvm-default=disable
-Xjvm-default=all-compatibility  -> -jvm-default=enable
-Xjvm-default=all                -> -jvm-default=no-compatibility""",
    )
    var jvmDefault: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjvm-enable-preview",
        description = """Allow using Java features that are in the preview phase.
This works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments.""",
    )
    var enableJvmPreview: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjvm-expose-boxed",
        description = "Expose inline classes and functions, accepting and returning them, to Java.",
    )
    @Enables(LanguageFeature.ImplicitJvmExposeBoxed)
    var jvmExposeBoxed: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib",
        valueDescription = "<path>",
        description = "Paths to cross-platform libraries in the .klib format.",
    )
    var klibLibraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xlambdas",
        valueDescription = "{class|indy}",
        description = """Select the code generation scheme for lambdas.
-Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
                                A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'.
-Xlambdas=class                 Generate lambdas as explicit classes.
The default value is 'indy' if language version is 2.0+, and 'class' otherwise.""",
    )
    var lambdas: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Deprecated("This flag is deprecated")
    @Argument(
        value = "-Xlink-via-signatures",
        description = """Link JVM IR symbols via signatures instead of descriptors.
This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
This option is deprecated and will be deleted in future versions.
It has no effect when -language-version is 2.0 or higher.""",
    )
    var linkViaSignatures: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xmodule-path",
        valueDescription = "<path>",
        description = "Paths to Java 9+ modules.",
    )
    var javaModulePath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xmultifile-parts-inherit",
        description = "Compile multifile classes as a hierarchy of parts and a facade.",
    )
    var inheritMultifileParts: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-call-assertions",
        description = "Don't generate not-null assertions for arguments of platform types.",
    )
    var noCallAssertions: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-new-java-annotation-targets",
        description = "Don't generate Java 1.8+ targets for Kotlin annotation classes.",
    )
    var noNewJavaAnnotationTargets: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-optimize",
        description = "Disable optimizations.",
    )
    var noOptimize: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-param-assertions",
        description = "Don't generate not-null assertions on parameters of methods accessible from Java.",
    )
    var noParamAssertions: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-receiver-assertions",
        description = "Don't generate not-null assertions for extension receiver arguments of platform types.",
    )
    var noReceiverAssertions: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-reset-jar-timestamps",
        description = "Don't reset jar entry timestamps to a fixed date.",
    )
    var noResetJarTimestamps: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-source-debug-extension",
        description = "Don't generate the '@kotlin.jvm.internal.SourceDebugExtension' annotation with an SMAP copy on classes.",
    )
    var noSourceDebugExtension: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-unified-null-checks",
        description = "Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details.",
    )
    var noUnifiedNullChecks: Boolean = false
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
* warn (report a warning)""",
    )
    var nullabilityAnnotations: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xoutput-builtins-metadata",
        description = "Output builtins metadata as .kotlin_builtins files",
    )
    var outputBuiltinsMetadata: Boolean = false
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
Individual parameter values are separated by the system path separator.
Example (Unix/Linux): -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>
Example (Windows): -Xprofile=<PATH_TO_ASYNC_PROFILER>\async-profiler\build\libasyncProfiler.so;event=cpu,interval=1ms,threads,start;<SNAPSHOT_DIR_PATH>""",
    )
    var profileCompilerCommand: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xsam-conversions",
        valueDescription = "{class|indy}",
        description = """Select the code generation scheme for SAM conversions.
-Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
-Xsam-conversions=class         Generate SAM conversions as explicit classes.
The default value is 'indy'.""",
    )
    var samConversions: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xsanitize-parentheses",
        description = """Transform '(' and ')' in method names to some other character sequence.
This mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for
problems with parentheses in identifiers on certain platforms.""",
    )
    var sanitizeParentheses: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xscript-resolver-environment",
        valueDescription = "<key=value[,]>",
        description = "Set the script resolver environment in key-value pairs (the value can be quoted and escaped).",
    )
    var scriptResolverEnvironment: Array<String>? = null
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
default: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise.""",
    )
    var stringConcat: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xsupport-compatqual-checker-framework-annotations",
        valueDescription = "enable|disable",
        description = """Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl').
The default value is 'enable'.""",
    )
    var supportCompatqualCheckerFrameworkAnnotations: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xsuppress-deprecated-jvm-target-warning",
        description = """Suppress warnings about deprecated JVM target versions.
This option has no effect and will be deleted in a future version.""",
    )
    var suppressDeprecatedJvmTargetWarning: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-missing-builtins-error",
        description = "Suppress the \"cannot access built-in declaration\" error (useful with '-no-stdlib').",
    )
    var suppressMissingBuiltinsError: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xtype-enhancement-improvements-strict-mode",
        description = """Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
including the ability to read type-use annotations from class files.
See KT-45671 for more details.""",
    )
    @Enables(LanguageFeature.TypeEnhancementImprovementsInStrictMode)
    var typeEnhancementImprovementsInStrictMode: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-14-inline-classes-mangling-scheme",
        description = "Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30.",
    )
    var useOldInlineClassesManglingScheme: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fast-jar-file-system",
        description = "Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental.",
    )
    var useFastJarFileSystem: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-inline-scopes-numbers",
        description = "Use inline scopes numbers for inline marker variables.",
    )
    var useInlineScopesNumbers: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-old-class-files-reading",
        description = """Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop.
This can be used in the event of problems with the new implementation.""",
    )
    var useOldClassFilesReading: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-type-table",
        description = "Use a type table in metadata serialization.",
    )
    var useTypeTable: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalidate-bytecode",
        description = "Validate generated JVM bytecode before and after optimizations.",
    )
    var validateBytecode: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalue-classes",
        description = "Enable experimental value classes.",
    )
    @Enables(LanguageFeature.JvmInlineMultiFieldValueClasses)
    var valueClasses: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwhen-expressions",
        valueDescription = "{indy|inline}",
        description = """Select the code generation scheme for type-checking 'when' expressions:
-Xwhen-expressions=indy         Generate type-checking 'when' expressions using 'invokedynamic' with 'SwitchBootstraps.typeSwitch(..)' and 
                                following 'tableswitch' or 'lookupswitch'. This requires '-jvm-target 21' or greater.
-Xwhen-expressions=inline       Generate type-checking 'when' expressions as a chain of type checks.
The default value is 'inline'.""",
    )
    var whenExpressionsGeneration: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-classpath",
        shortName = "-cp",
        valueDescription = "<path>",
        description = "List of directories and JAR/ZIP archives to search for user class files.",
    )
    var classpath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-d",
        valueDescription = "<directory|jar>",
        description = "Destination for generated class files.",
    )
    var destination: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-expression",
        shortName = "-e",
        description = "Evaluate the given string as a Kotlin script.",
    )
    var expression: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-include-runtime",
        description = "Include the Kotlin runtime in the resulting JAR.",
    )
    var includeRuntime: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-java-parameters",
        description = "Generate metadata for Java 1.8 reflection on method parameters.",
    )
    var javaParameters: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-jdk-home",
        valueDescription = "<path>",
        description = "Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'.",
    )
    var jdkHome: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-jvm-default",
        valueDescription = "{enable|no-compatibility|disable}",
        description = """Emit JVM default methods for interface declarations with bodies. The default is 'enable'.
-jvm-default=enable              Generate default methods for non-abstract interface declarations, as well as 'DefaultImpls' classes with
                                 static methods for compatibility with code compiled in the 'disable' mode.
                                 This is the default behavior since language version 2.2.
-jvm-default=no-compatibility    Generate default methods for non-abstract interface declarations. Do not generate 'DefaultImpls' classes.
-jvm-default=disable             Do not generate JVM default methods. This is the default behavior up to language version 2.1.""",
    )
    var jvmDefaultStable: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-jvm-target",
        valueDescription = "<version>",
        description = "The target version of the generated JVM bytecode (1.8 and 9–25), with 1.8 as the default.",
    )
    var jvmTarget: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-module-name",
        valueDescription = "<name>",
        description = "Name of the generated '.kotlin_module' file.",
    )
    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-no-jdk",
        description = "Don't automatically include the Java runtime in the classpath.",
    )
    var noJdk: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-no-reflect",
        description = "Don't automatically include the Kotlin reflection dependency in the classpath.",
    )
    var noReflect: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-no-stdlib",
        description = "Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath.",
    )
    var noStdlib: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-script-templates",
        valueDescription = "<fully qualified class name[,]>",
        description = "Script definition template classes.",
    )
    var scriptTemplates: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @get:Transient
    @field:kotlin.jvm.Transient
    override val configurator: CommonCompilerArgumentsConfigurator = K2JVMCompilerArgumentsConfigurator()

    override fun copyOf(): Freezable = copyK2JVMCompilerArguments(this, K2JVMCompilerArguments())
}
