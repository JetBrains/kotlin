package org.jetbrains.kotlin.buildtools.api.v2

import java.nio.`file`.Path
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.jvm.JvmField
import org.jetbrains.kotlin.buildtools.api.v2.enums.JvmTarget

public class JvmCompilerArguments : CommonCompilerArguments() {
  private val optionsMap: MutableMap<JvmCompilerArgument<*>, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V? = optionsMap[key] as V?

  public operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  public class JvmCompilerArgument<V>(
    public val id: String,
  )

  public companion object {
    /**
     * Destination for generated class files.
     */
    @JvmField
    public val D: JvmCompilerArgument<Path?> = JvmCompilerArgument("D")

    /**
     * List of directories and JAR/ZIP archives to search for user class files.
     */
    @JvmField
    public val CLASSPATH: JvmCompilerArgument<String?> = JvmCompilerArgument("CLASSPATH")

    /**
     * Include the Kotlin runtime in the resulting JAR.
     */
    @JvmField
    public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("INCLUDE_RUNTIME")

    /**
     * Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'.
     */
    @JvmField
    public val JDK_HOME: JvmCompilerArgument<Path?> = JvmCompilerArgument("JDK_HOME")

    /**
     * Don't automatically include the Java runtime in the classpath.
     */
    @JvmField
    public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

    /**
     * Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath.
     */
    @JvmField
    public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")

    /**
     * Don't automatically include the Kotlin reflection dependency in the classpath.
     */
    @JvmField
    public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

    /**
     * Evaluate the given string as a Kotlin script.
     */
    @JvmField
    public val EXPRESSION: JvmCompilerArgument<String?> = JvmCompilerArgument("EXPRESSION")

    /**
     * Script definition template classes.
     */
    @JvmField
    public val SCRIPT_TEMPLATES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("SCRIPT_TEMPLATES")

    /**
     * Name of the generated '.kotlin_module' file.
     */
    @JvmField
    public val MODULE_NAME: JvmCompilerArgument<String?> = JvmCompilerArgument("MODULE_NAME")

    /**
     * The target version of the generated JVM bytecode (1.8 and 9–24), with 1.8 as the default.
     */
    @JvmField
    public val JVM_TARGET: JvmCompilerArgument<JvmTarget?> = JvmCompilerArgument("JVM_TARGET")

    /**
     * Generate metadata for Java 1.8 reflection on method parameters.
     */
    @JvmField
    public val JAVA_PARAMETERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("JAVA_PARAMETERS")

    /**
     * Emit JVM default methods for interface declarations with bodies. The default is 'enable'.
     * -jvm-default=enable              Generate default methods for non-abstract interface declarations, as well as 'DefaultImpls' classes with
     *                                  static methods for compatibility with code compiled in the 'disable' mode.
     *                                  This is the default behavior since language version 2.2.
     * -jvm-default=no-compatibility    Generate default methods for non-abstract interface declarations. Do not generate 'DefaultImpls' classes.
     * -jvm-default=disable             Do not generate JVM default methods. This is the default behavior up to language version 2.1.
     */
    @JvmField
    public val JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("JVM_DEFAULT")

    /**
     * Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XALLOW_UNSTABLE_DEPENDENCIES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XALLOW_UNSTABLE_DEPENDENCIES")

    /**
     * When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable
     * to prevent diagnostics from being reported when using stable compilers at the call site.
     * When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable
     * to force diagnostics to be reported.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XABI_STABILITY: JvmCompilerArgument<String?> = JvmCompilerArgument("XABI_STABILITY")

    /**
     * When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_DO_NOT_CLEAR_BINDING_CONTEXT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XIR_DO_NOT_CLEAR_BINDING_CONTEXT")

    /**
     * Run codegen phase in N parallel threads.
     * 0 means use one thread per processor core.
     * The default value is 1.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XBACKEND_THREADS: JvmCompilerArgument<Int> = JvmCompilerArgument("XBACKEND_THREADS")

    /**
     * Paths to Java 9+ modules.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMODULE_PATH: JvmCompilerArgument<String?> = JvmCompilerArgument("XMODULE_PATH")

    /**
     * Root modules to resolve in addition to the initial modules, or all modules on the module path if <module> is ALL-MODULE-PATH.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XADD_MODULES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("XADD_MODULES")

    /**
     * Don't generate not-null assertions for arguments of platform types.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_CALL_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_CALL_ASSERTIONS")

    /**
     * Don't generate not-null assertions for extension receiver arguments of platform types.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_RECEIVER_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_RECEIVER_ASSERTIONS")

    /**
     * Don't generate not-null assertions on parameters of methods accessible from Java.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_PARAM_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_PARAM_ASSERTIONS")

    /**
     * Disable optimizations.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_OPTIMIZE: JvmCompilerArgument<Boolean> = JvmCompilerArgument("XNO_OPTIMIZE")

    /**
     * 'kotlin.assert' call behavior:
     * -Xassertions=always-enable:  enable, ignore JVM assertion settings;
     * -Xassertions=always-disable: disable, ignore JVM assertion settings;
     * -Xassertions=jvm:            enable, depend on JVM assertion settings;
     * -Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package;
     * default: legacy
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XASSERTIONS: JvmCompilerArgument<String?> = JvmCompilerArgument("XASSERTIONS")

    /**
     * Path to the .xml build file to compile.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XBUILD_FILE: JvmCompilerArgument<Path?> = JvmCompilerArgument("XBUILD_FILE")

    /**
     * Compile multifile classes as a hierarchy of parts and a facade.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XMULTIFILE_PARTS_INHERIT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XMULTIFILE_PARTS_INHERIT")

    /**
     * Use a type table in metadata serialization.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_TYPE_TABLE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XUSE_TYPE_TABLE")

    /**
     * Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop.
     * This can be used in the event of problems with the new implementation.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_OLD_CLASS_FILES_READING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XUSE_OLD_CLASS_FILES_READING")

    /**
     * Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_FAST_JAR_FILE_SYSTEM: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("XUSE_FAST_JAR_FILE_SYSTEM")

    /**
     * Suppress the "cannot access built-in declaration" error (useful with '-no-stdlib').
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSUPPRESS_MISSING_BUILTINS_ERROR: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XSUPPRESS_MISSING_BUILTINS_ERROR")

    /**
     * Set the script resolver environment in key-value pairs (the value can be quoted and escaped).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSCRIPT_RESOLVER_ENVIRONMENT: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("XSCRIPT_RESOLVER_ENVIRONMENT")

    /**
     * Use javac for Java source and class file analysis.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_JAVAC: JvmCompilerArgument<Boolean> = JvmCompilerArgument("XUSE_JAVAC")

    /**
     * Reuse 'javac' analysis and compile Java source files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCOMPILE_JAVA: JvmCompilerArgument<Boolean> = JvmCompilerArgument("XCOMPILE_JAVA")

    /**
     * Java compiler arguments.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJAVAC_ARGUMENTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("XJAVAC_ARGUMENTS")

    /**
     * Paths to directories with Java source files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJAVA_SOURCE_ROOTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("XJAVA_SOURCE_ROOTS")

    /**
     * Package prefix for Java files.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJAVA_PACKAGE_PREFIX: JvmCompilerArgument<String?> =
        JvmCompilerArgument("XJAVA_PACKAGE_PREFIX")

    /**
     * Specify the behavior of 'JSR-305' nullability annotations:
     * -Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations)
     * -Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations
     * -Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name
     * Modes:
     * * ignore
     * * strict (experimental; treat like other supported nullability annotations)
     * * warn (report a warning)
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJSR305: JvmCompilerArgument<Array<String>?> = JvmCompilerArgument("XJSR305")

    /**
     * Specify the behavior for specific Java nullability annotations (provided with fully qualified package name).
     * Modes:
     * * ignore
     * * strict
     * * warn (report a warning)
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNULLABILITY_ANNOTATIONS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("XNULLABILITY_ANNOTATIONS")

    /**
     * Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl').
     * The default value is 'enable'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("XSUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")

    /**
     * Specify the behavior of 'jspecify' annotations.
     * The default value is 'warn'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJSPECIFY_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("XJSPECIFY_ANNOTATIONS")

    /**
     * This option is deprecated. Migrate to -jvm-default as follows:
     * -Xjvm-default=disable            -> -jvm-default=disable
     * -Xjvm-default=all-compatibility  -> -jvm-default=enable
     * -Xjvm-default=all                -> -jvm-default=no-compatibility
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("XJVM_DEFAULT")

    /**
     * Compile expressions and unrecognized scripts passed with the -script argument as scripts with the given filename extension.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDEFAULT_SCRIPT_EXTENSION: JvmCompilerArgument<String?> =
        JvmCompilerArgument("XDEFAULT_SCRIPT_EXTENSION")

    /**
     * Disable standard Kotlin scripting support.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDISABLE_STANDARD_SCRIPT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XDISABLE_STANDARD_SCRIPT")

    /**
     * Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt').
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XGENERATE_STRICT_METADATA_VERSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XGENERATE_STRICT_METADATA_VERSION")

    /**
     * Transform '(' and ')' in method names to some other character sequence.
     * This mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for
     * problems with parentheses in identifiers on certain platforms.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSANITIZE_PARENTHESES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XSANITIZE_PARENTHESES")

    /**
     * Paths to output directories for friend modules (modules whose internals should be visible).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XFRIEND_PATHS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("XFRIEND_PATHS")

    /**
     * Allow the set of source files to be empty.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XALLOW_NO_SOURCE_FILES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XALLOW_NO_SOURCE_FILES")

    /**
     * Emit JVM type annotations in bytecode.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XEMIT_JVM_TYPE_ANNOTATIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XEMIT_JVM_TYPE_ANNOTATIONS")

    /**
     * Expose inline classes and functions, accepting and returning them, to Java.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJVM_EXPOSE_BOXED: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XJVM_EXPOSE_BOXED")

    /**
     * Select the code generation scheme for string concatenation:
     * -Xstring-concat=indy-with-constants  Concatenate strings using 'invokedynamic' and 'makeConcatWithConstants'. This requires '-jvm-target 9' or greater.
     * -Xstring-concat=indy                 Concatenate strings using 'invokedynamic' and 'makeConcat'. This requires '-jvm-target 9' or greater.
     * -Xstring-concat=inline               Concatenate strings using 'StringBuilder'
     * default: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSTRING_CONCAT: JvmCompilerArgument<String?> = JvmCompilerArgument("XSTRING_CONCAT")

    /**
     * Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.
     * The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–24.
     * This also sets the value of '-jvm-target' to be equal to the selected JDK version.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJDK_RELEASE: JvmCompilerArgument<String?> = JvmCompilerArgument("XJDK_RELEASE")

    /**
     * Select the code generation scheme for SAM conversions.
     * -Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
     * -Xsam-conversions=class         Generate SAM conversions as explicit classes.
     * The default value is 'indy'.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSAM_CONVERSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("XSAM_CONVERSIONS")

    /**
     * Select the code generation scheme for lambdas.
     * -Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'.
     *                                 A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'.
     * -Xlambdas=class                 Generate lambdas as explicit classes.
     * The default value is 'indy' if language version is 2.0+, and 'class' otherwise.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLAMBDAS: JvmCompilerArgument<String?> = JvmCompilerArgument("XLAMBDAS")

    /**
     * Allow using 'invokedynamic' for lambda expressions with annotations
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XINDY_ALLOW_ANNOTATED_LAMBDAS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XINDY_ALLOW_ANNOTATED_LAMBDAS")

    /**
     * Paths to cross-platform libraries in the .klib format.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XKLIB: JvmCompilerArgument<String?> = JvmCompilerArgument("XKLIB")

    /**
     * Don't reset jar entry timestamps to a fixed date.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_RESET_JAR_TIMESTAMPS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_RESET_JAR_TIMESTAMPS")

    /**
     * Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_UNIFIED_NULL_CHECKS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_UNIFIED_NULL_CHECKS")

    /**
     * Don't generate the '@kotlin.jvm.internal.SourceDebugExtension' annotation with an SMAP copy on classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_SOURCE_DEBUG_EXTENSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_SOURCE_DEBUG_EXTENSION")

    /**
     * Debug option: Run the compiler with the async profiler and save snapshots to `outputDir`; `command` is passed to the async profiler on start.
     * `profilerPath` is the path to libasyncProfiler.so; async-profiler.jar should be on the compiler classpath.
     * If it's not on the classpath, the compiler will attempt to load async-profiler.jar from the containing directory of profilerPath.
     * Example: -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XPROFILE: JvmCompilerArgument<String?> = JvmCompilerArgument("XPROFILE")

    /**
     * Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_14_INLINE_CLASSES_MANGLING_SCHEME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XUSE_14_INLINE_CLASSES_MANGLING_SCHEME")

    /**
     * Allow using Java features that are in the preview phase.
     * This works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XJVM_ENABLE_PREVIEW: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XJVM_ENABLE_PREVIEW")

    /**
     * Suppress warnings about deprecated JVM target versions.
     * This option has no effect and will be deleted in a future version.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSUPPRESS_DEPRECATED_JVM_TARGET_WARNING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XSUPPRESS_DEPRECATED_JVM_TARGET_WARNING")

    /**
     * Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
     * including the ability to read type-use annotations from class files.
     * See KT-45671 for more details.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XTYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XTYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE")

    /**
     * Save the IR to metadata (Experimental).
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XSERIALIZE_IR: JvmCompilerArgument<String> = JvmCompilerArgument("XSERIALIZE_IR")

    /**
     * Validate generated JVM bytecode before and after optimizations.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVALIDATE_BYTECODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XVALIDATE_BYTECODE")

    /**
     * Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL")

    /**
     * Link JVM IR symbols via signatures instead of descriptors.
     * This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
     * This option is deprecated and will be deleted in future versions.
     * It has no effect when -language-version is 2.0 or higher.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XLINK_VIA_SIGNATURES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XLINK_VIA_SIGNATURES")

    /**
     * Enable debug mode for compilation.
     * Currently this includes spilling all variables in a suspending context regardless of whether they are alive.
     * If API Level >= 2.2 -- no-op.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XDEBUG: JvmCompilerArgument<Boolean> = JvmCompilerArgument("XDEBUG")

    /**
     * Generate additional linenumber instruction for compiler-generated code
     * inside suspend functions and lambdas to distinguish them from user code by debugger.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XENHANCED_COROUTINES_DEBUGGING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XENHANCED_COROUTINES_DEBUGGING")

    /**
     * Don't generate Java 1.8+ targets for Kotlin annotation classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XNO_NEW_JAVA_ANNOTATION_TARGETS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XNO_NEW_JAVA_ANNOTATION_TARGETS")

    /**
     * Enable experimental value classes.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XVALUE_CLASSES: JvmCompilerArgument<Boolean> = JvmCompilerArgument("XVALUE_CLASSES")

    /**
     * Inline functions using the IR inliner instead of the bytecode inliner.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XIR_INLINER: JvmCompilerArgument<Boolean> = JvmCompilerArgument("XIR_INLINER")

    /**
     * Use inline scopes numbers for inline marker variables.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_INLINE_SCOPES_NUMBERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XUSE_INLINE_SCOPES_NUMBERS")

    /**
     * Enable the experimental support for K2 KAPT.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XUSE_K2_KAPT: JvmCompilerArgument<Boolean?> = JvmCompilerArgument("XUSE_K2_KAPT")

    /**
     * Enable behaviour needed to compile builtins as part of JVM stdlib
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XCOMPILE_BUILTINS_AS_PART_OF_STDLIB: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XCOMPILE_BUILTINS_AS_PART_OF_STDLIB")

    /**
     * Output builtins metadata as .kotlin_builtins files
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XOUTPUT_BUILTINS_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XOUTPUT_BUILTINS_METADATA")

    /**
     * Write annotations on declarations into the metadata (in addition to the JVM bytecode), and read annotations from the metadata if they are present.
     */
    @JvmField
    @Deprecated(message = "This option is experimental and it may be changed in the future")
    public val XANNOTATIONS_IN_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("XANNOTATIONS_IN_METADATA")
  }
}
