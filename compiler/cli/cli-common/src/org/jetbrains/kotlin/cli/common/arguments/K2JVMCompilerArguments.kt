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

    @Argument(value = "-d", valueDescription = "<directory|jar>", description = "Destination for generated class files")
    var destination: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-classpath",
        shortName = "-cp",
        valueDescription = "<path>",
        description = "List of directories and JAR/ZIP archives to search for user class files",
        delimiter = Argument.Delimiters.pathSeparator
    )
    var classpath: Array<String>? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-include-runtime", description = "Include Kotlin runtime into the resulting JAR")
    var includeRuntime = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-jdk-home",
        valueDescription = "<path>",
        description = "Include a custom JDK from the specified location into the classpath instead of the default JAVA_HOME"
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
    @Argument(value = "-no-jdk", description = "Don't automatically include the Java runtime into the classpath")
    var noJdk = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-no-stdlib",
        description = "Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection into the classpath"
    )
    var noStdlib = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-no-reflect", description = "Don't automatically include Kotlin reflection into the classpath")
    var noReflect = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-expression",
        shortName = "-e",
        description = "Evaluate the given string as a Kotlin script"
    )
    var expression: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-script-templates",
        valueDescription = "<fully qualified class name[,]>",
        description = "Script definition template classes"
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
    @Argument(value = "-module-name", valueDescription = "<name>", description = "Name of the generated .kotlin_module file")
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
        description = "Target version of the generated JVM bytecode (${JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION}), default is 1.8",
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
    @Argument(value = "-java-parameters", description = "Generate metadata for Java 1.8 reflection on method parameters")
    var javaParameters = false
        set(value) {
            checkFrozen()
            field = value
        }

    // Advanced options

    @Argument(
        value = "-Xuse-ir",
        description = "Use the IR backend. This option has no effect unless the language version less than 1.5 is used"
    )
    var useIR = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xuse-old-backend", description = "Use the old JVM backend")
    var useOldBackend = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-unstable-dependencies",
        description = "Do not report errors on classes in dependencies, which were compiled by an unstable version of the Kotlin compiler"
    )
    var allowUnstableDependencies = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xabi-stability",
        valueDescription = "{stable|unstable}",
        description = "When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable\n" +
                "to prevent diagnostics from stable compilers at the call site.\n" +
                "When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable\n" +
                "to force diagnostics to be reported."
    )
    var abiStability: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-do-not-clear-binding-context",
        description = "When using the IR backend, do not clear BindingContext between psi2ir and lowerings"
    )
    var doNotClearBindingContext = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xbackend-threads",
        valueDescription = "<N>",
        description = "When using the IR backend, run lowerings by file in N parallel threads.\n" +
                "0 means use a thread per processor core.\n" +
                "Default value is 1"
    )
    var backendThreads: String = "1"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xmodule-path",
        valueDescription = "<path>",
        description = "Paths where to find Java 9+ modules",
        delimiter = Argument.Delimiters.pathSeparator
    )
    var javaModulePath: Array<String>? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xadd-modules",
        valueDescription = "<module[,]>",
        description = "Root modules to resolve in addition to the initial modules,\n" +
                "or all modules on the module path if <module> is ALL-MODULE-PATH"
    )
    var additionalJavaModules: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xno-call-assertions", description = "Don't generate not-null assertions for arguments of platform types")
    var noCallAssertions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-receiver-assertions",
        description = "Don't generate not-null assertion for extension receiver arguments of platform types"
    )
    var noReceiverAssertions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-param-assertions",
        description = "Don't generate not-null assertions on parameters of methods accessible from Java"
    )
    var noParamAssertions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xno-optimize", description = "Disable optimizations")
    var noOptimize = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xassertions", valueDescription = "{always-enable|always-disable|jvm|legacy}",
        description = "Assert calls behaviour\n" +
                "-Xassertions=always-enable:  enable, ignore jvm assertion settings;\n" +
                "-Xassertions=always-disable: disable, ignore jvm assertion settings;\n" +
                "-Xassertions=jvm:            enable, depend on jvm assertion settings;\n" +
                "-Xassertions=legacy:         calculate condition on each call, check depends on jvm assertion settings in the kotlin package;\n" +
                "default: legacy"
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
        description = "Path to the .xml build file to compile"
    )
    var buildFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-Xmultifile-parts-inherit", description = "Compile multifile classes as a hierarchy of parts and facade")
    var inheritMultifileParts = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xuse-type-table", description = "Use type table in metadata serialization")
    var useTypeTable = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-old-class-files-reading",
        description = "Use old class files reading implementation. This may slow down the build and cause problems with Groovy interop.\n" +
                "Should be used in case of problems with the new implementation"
    )
    var useOldClassFilesReading = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fast-jar-file-system",
        description = "Use fast implementation on Jar FS. This may speed up compilation time, but currently it's an experimental mode"
    )
    var useFastJarFileSystem = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-declarations-to",
        valueDescription = "<path>",
        description = "Path to JSON file to dump Java to Kotlin declaration mappings"
    )
    var declarationsOutputPath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xsuppress-missing-builtins-error",
        description = "Suppress the \"cannot access built-in declaration\" error (useful with -no-stdlib)"
    )
    var suppressMissingBuiltinsError = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xscript-resolver-environment",
        valueDescription = "<key=value[,]>",
        description = "Script resolver environment in key-value pairs (the value could be quoted and escaped)"
    )
    var scriptResolverEnvironment: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    // Javac options
    @Argument(value = "-Xuse-javac", description = "Use javac for Java source and class files analysis")
    var useJavac = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xcompile-java", description = "Reuse javac analysis and compile Java source files")
    var compileJava = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjavac-arguments",
        valueDescription = "<option[,]>",
        description = "Java compiler arguments"
    )
    var javacArguments: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }


    @Argument(
        value = "-Xjava-source-roots",
        valueDescription = "<path>",
        description = "Paths to directories with Java source files"
    )
    var javaSourceRoots: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjava-package-prefix",
        description = "Package prefix for Java files"
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
        description = "Specify behavior for JSR-305 nullability annotations:\n" +
                "-Xjsr305={ignore/strict/warn}                   globally (all non-@UnderMigration annotations)\n" +
                "-Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations\n" +
                "-Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name\n" +
                "Modes:\n" +
                "  * ignore\n" +
                "  * strict (experimental; treat as other supported nullability annotations)\n" +
                "  * warn (report a warning)"
    )
    var jsr305: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xnullability-annotations",
        valueDescription = "@<fq.name>:{ignore/strict/warn}",
        description = "Specify behavior for specific Java nullability annotations (provided with fully qualified package name)\n" +
                "Modes:\n" +
                "  * ignore\n" +
                "  * strict\n" +
                "  * warn (report a warning)"
    )
    var nullabilityAnnotations: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsupport-compatqual-checker-framework-annotations",
        valueDescription = "enable|disable",
        description = "Specify behavior for Checker Framework compatqual annotations (NullableDecl/NonNullDecl).\n" +
                "Default value is 'enable'"
    )
    var supportCompatqualCheckerFrameworkAnnotations: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjspecify-annotations",
        valueDescription = "ignore|strict|warn",
        description = "Specify behavior for jspecify annotations.\n" +
                "Default value is 'warn'"
    )
    var jspecifyAnnotations: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjvm-default",
        valueDescription = "{all|all-compatibility|disable|enable|compatibility}",
        description = """Emit JVM default methods for interface declarations with bodies. Default is 'disable'.
-Xjvm-default=all                Generate JVM default methods for all interface declarations with bodies in the module.
                                 Do not generate DefaultImpls stubs for interface declarations with bodies, which are generated by default
                                 in the 'disable' mode. If interface inherits a method with body from an interface compiled in the 'disable'
                                 mode and doesn't override it, then a DefaultImpls stub will be generated for it.
                                 BREAKS BINARY COMPATIBILITY if some client code relies on the presence of DefaultImpls classes.
                                 Note that if interface delegation is used, all interface methods are delegated.
                                 The only exception are methods annotated with the deprecated @JvmDefault annotation.
-Xjvm-default=all-compatibility  In addition to the 'all' mode, generate compatibility stubs in the DefaultImpls classes.
                                 Compatibility stubs could be useful for library and runtime authors to keep backward binary compatibility
                                 for existing clients compiled against previous library versions.
                                 'all' and 'all-compatibility' modes are changing the library ABI surface that will be used by clients after
                                 the recompilation of the library. In that sense, clients might be incompatible with previous library
                                 versions. This usually means that proper library versioning is required, e.g. major version increase in SemVer.
                                 In case of inheritance from a Kotlin interface compiled in 'all' or 'all-compatibility' modes, DefaultImpls
                                 compatibility stubs will invoke the default method of the interface with standard JVM runtime resolution semantics.
                                 Perform additional compatibility checks for classes inheriting generic interfaces where in some cases
                                 additional implicit method with specialized signatures was generated in the 'disable' mode:
                                 unlike in the 'disable' mode, the compiler will report an error if such method is not overridden explicitly
                                 and the class is not annotated with @JvmDefaultWithoutCompatibility (see KT-39603 for more details).
-Xjvm-default=disable            Default behavior. Do not generate JVM default methods and prohibit @JvmDefault annotation usage.
-Xjvm-default=enable             Deprecated. Allow usages of @JvmDefault; only generate the default method for annotated method
                                 in the interface (annotating an existing method can break binary compatibility).
-Xjvm-default=compatibility      Deprecated. Allow usages of @JvmDefault; generate a compatibility accessor
                                 in the DefaultImpls class in addition to the default interface method."""
    )
    var jvmDefault: String = JvmDefaultMode.DEFAULT.description
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdefault-script-extension",
        valueDescription = "<script filename extension>",
        description = "Compile expressions and unrecognized scripts passed with the -script argument as scripts with given filename extension"
    )
    var defaultScriptExtension: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xdisable-standard-script", description = "Disable standard kotlin script support")
    var disableStandardScript = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xgenerate-strict-metadata-version",
        description = "Generate metadata with strict version semantics (see kdoc on Metadata.extraInt)"
    )
    var strictMetadataVersionSemantics = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsanitize-parentheses",
        description = "Transform '(' and ')' in method names to some other character sequence.\n" +
                "This mode can BREAK BINARY COMPATIBILITY and is only supposed to be used to workaround\n" +
                "problems with parentheses in identifiers on certain platforms"
    )
    var sanitizeParentheses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfriend-paths",
        valueDescription = "<path>",
        description = "Paths to output directories for friend modules (whose internals should be visible)"
    )
    var friendPaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-no-source-files",
        description = "Allow no source files"
    )
    var allowNoSourceFiles = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xemit-jvm-type-annotations",
        description = "Emit JVM type annotations in bytecode"
    )
    var emitJvmTypeAnnotations = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xstring-concat",
        valueDescription = "{indy-with-constants|indy|inline}",
        description = """Select code generation scheme for string concatenation.
-Xstring-concat=indy-with-constants   Concatenate strings using `invokedynamic` `makeConcatWithConstants`. Requires `-jvm-target 9` or greater.
-Xstring-concat=indy                Concatenate strings using `invokedynamic` `makeConcat`. Requires `-jvm-target 9` or greater.
-Xstring-concat=inline              Concatenate strings using `StringBuilder`
default: `indy-with-constants` for JVM target 9 or greater, `inline` otherwise"""

    )
    var stringConcat: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xjdk-release",
        valueDescription = "<version>",
        description = """Compile against the specified JDK API version, similarly to javac's `-release`. Requires JDK 9 or newer.
Supported versions depend on the used JDK; for JDK 17+ supported versions are ${JvmTarget.SUPPORTED_VERSIONS_DESCRIPTION}.
Also sets `-jvm-target` value equal to the selected JDK version"""
    )
    var jdkRelease: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }


    @Argument(
        value = "-Xsam-conversions",
        valueDescription = "{class|indy}",
        description = """Select code generation scheme for SAM conversions.
-Xsam-conversions=indy              Generate SAM conversions using `invokedynamic` with `LambdaMetafactory.metafactory`. Requires `-jvm-target 1.8` or greater.
-Xsam-conversions=class             Generate SAM conversions as explicit classes"""
    )
    var samConversions: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xlambdas",
        valueDescription = "{class|indy}",
        description = """Select code generation scheme for lambdas.
-Xlambdas=indy                      Generate lambdas using `invokedynamic` with `LambdaMetafactory.metafactory`. Requires `-jvm-target 1.8` or greater.
                                    Lambda objects created using `LambdaMetafactory.metafactory` will have different `toString()`.
-Xlambdas=class                     Generate lambdas as explicit classes"""
    )
    var lambdas: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xklib",
        valueDescription = "<path>",
        description = "Paths to cross-platform libraries in .klib format"
    )
    var klibLibraries: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xno-optimized-callable-references",
        description = "Do not use optimized callable reference superclasses available from 1.4"
    )
    var noOptimizedCallableReferences = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-kotlin-nothing-value-exception",
        description = "Do not use KotlinNothingValueException available since 1.4"
    )
    var noKotlinNothingValueException = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-reset-jar-timestamps",
        description = "Do not reset jar entry timestamps to a fixed date"
    )
    var noResetJarTimestamps = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-unified-null-checks",
        description = "Use pre-1.4 exception types in null checks instead of java.lang.NPE. See KT-22275 for more details"
    )
    var noUnifiedNullChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-source-debug-extension",
        description = "Do not generate @kotlin.jvm.internal.SourceDebugExtension annotation on a class with the copy of SMAP"
    )
    var noSourceDebugExtension = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xprofile",
        valueDescription = "<profilerPath:command:outputDir>",
        description = "Debug option: Run compiler with async profiler and save snapshots to `outputDir`; `command` is passed to async-profiler on start.\n" +
                "`profilerPath` is a path to libasyncProfiler.so; async-profiler.jar should be on the compiler classpath.\n" +
                "If it's not on the classpath, the compiler will attempt to load async-profiler.jar from the containing directory of profilerPath.\n" +
                "Example: -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>"
    )
    var profileCompilerCommand: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xrepeat",
        valueDescription = "<number>",
        description = "Debug option: Repeats modules compilation <number> times"
    )
    var repeatCompileModules: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xuse-14-inline-classes-mangling-scheme",
        description = "Use 1.4 inline classes mangling scheme instead of 1.4.30 one"
    )
    var useOldInlineClassesManglingScheme = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xjvm-enable-preview",
        description = "Allow using features from Java language that are in preview phase.\n" +
                "Works as `--enable-preview` in Java. All class files are marked as preview-generated thus it won't be possible to use them in release environment"
    )
    var enableJvmPreview = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-deprecated-jvm-target-warning",
        description = "Suppress deprecation warning about deprecated JVM target versions.\n" +
                "This option has no effect and will be deleted in a future version."
    )
    var suppressDeprecatedJvmTargetWarning = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xtype-enhancement-improvements-strict-mode",
        description = "Enable strict mode for some improvements in the type enhancement for loaded Java types based on nullability annotations,\n" +
                "including freshly supported reading of the type use annotations from class files.\n" +
                "See KT-45671 for more details"
    )
    var typeEnhancementImprovementsInStrictMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xserialize-ir",
        valueDescription = "{none|inline|all}",
        description = "Save IR to metadata (EXPERIMENTAL)"
    )
    var serializeIr: String = "none"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalidate-ir",
        description = "Validate IR before and after lowering"
    )
    var validateIr = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalidate-bytecode",
        description = "Validate generated JVM bytecode before and after optimizations"
    )
    var validateBytecode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenhance-type-parameter-types-to-def-not-null",
        description = "Enhance not null annotated type parameter's types to definitely not null types (@NotNull T => T & Any)"
    )
    var enhanceTypeParameterTypesToDefNotNull = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xlink-via-signatures",
        description = "Link JVM IR symbols via signatures, instead of descriptors.\n" +
                "This mode is slower, but can be useful in troubleshooting problems with the JVM IR backend"
    )
    var linkViaSignatures = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdebug",
        description = "Enable debug mode for compilation.\n" +
                "Currently this includes spilling all variables in a suspending context regardless their liveness."
    )
    var enableDebugMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-new-java-annotation-targets",
        description = "Do not generate Java 1.8+ targets for Kotlin annotation classes"
    )
    var noNewJavaAnnotationTargets = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-old-innerclasses-logic",
        description = "Use old logic for generation of InnerClasses attributes"
    )
    var oldInnerClassesLogic = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xvalue-classes",
        description = "Enable experimental value classes"
    )
    var valueClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xir-inliner",
        description = "Inline functions using IR inliner instead of bytecode inliner"
    )
    var enableIrInliner: Boolean = false
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
            "Unknown @JvmDefault mode: $jvmDefault, " +
                    "supported modes: ${JvmDefaultMode.values().map { it.description }}"
        )
        result[JvmAnalysisFlags.inheritMultifileParts] = inheritMultifileParts
        result[JvmAnalysisFlags.sanitizeParentheses] = sanitizeParentheses
        result[JvmAnalysisFlags.suppressMissingBuiltinsError] = suppressMissingBuiltinsError
        result[JvmAnalysisFlags.enableJvmPreview] = enableJvmPreview
        result[AnalysisFlags.allowUnstableDependencies] = allowUnstableDependencies || useK2 || languageVersion.usesK2
        result[JvmAnalysisFlags.disableUltraLightClasses] = disableUltraLightClasses
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
        if (JvmDefaultMode.fromStringOrNull(jvmDefault)?.forAllMethodsWithBody == true) {
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
    }

    override fun copyOf(): Freezable = copyK2JVMCompilerArguments(this, K2JVMCompilerArguments())
}
