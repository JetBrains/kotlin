/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.String
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.BOOLEAN_FALSE_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JVM_DEFAULT_MODES
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.JVM_TARGET_VERSIONS
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.STRING_NULL_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes.INPUT
import org.jetbrains.kotlin.config.LanguageFeature.AnnotationsInMetadata
import org.jetbrains.kotlin.config.LanguageFeature.ImplicitJvmExposeBoxed
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
import org.jetbrains.kotlin.config.LanguageFeature.TypeEnhancementImprovementsInStrictMode
import org.jetbrains.kotlin.config.LanguageFeature.ValueClasses
import com.intellij.util.xmlb.annotations.Transient as AnnotationsTransient
import kotlin.jvm.Transient as JvmTransient

/**
 * This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
 * Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/JvmCompilerArguments.kt
 * DO NOT MODIFY IT MANUALLY.
 */
public class K2JVMCompilerArguments : CommonCompilerArguments() {
  @Argument(
    value = "-d",
    valueDescription = "<directory|jar>",
    description = "Destination for generated class files.",
  )
  public var destination: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-classpath",
    shortName = "-cp",
    valueDescription = "<path>",
    description = "List of directories and JAR/ZIP archives to search for user class files.",
  )
  public var classpath: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-include-runtime",
    description = "Include the Kotlin runtime in the resulting JAR.",
  )
  public var includeRuntime: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-jdk-home",
    valueDescription = "<path>",
    description = "Include a custom JDK from the specified location in the classpath instead of the default 'JAVA_HOME'.",
  )
  public var jdkHome: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-no-jdk",
    description = "Don't automatically include the Java runtime in the classpath.",
  )
  public var noJdk: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-no-stdlib",
    description = "Don't automatically include the Kotlin/JVM stdlib and Kotlin reflection dependencies in the classpath.",
  )
  public var noStdlib: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-no-reflect",
    description = "Don't automatically include the Kotlin reflection dependency in the classpath.",
  )
  public var noReflect: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-expression",
    shortName = "-e",
    description = "Evaluate the given string as a Kotlin script.",
  )
  public var expression: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-script-templates",
    valueDescription = "<fully qualified class name[,]>",
    description = "Script definition template classes.",
  )
  public var scriptTemplates: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = STRING_NULL_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-module-name",
    valueDescription = "<name>",
    description = "Name of the generated '.kotlin_module' file.",
  )
  public var moduleName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = JVM_TARGET_VERSIONS,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-jvm-target",
    valueDescription = "<version>",
    description = "The target version of the generated JVM bytecode (1.8 and 9–24), with 1.8 as the default.",
  )
  public var jvmTarget: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-java-parameters",
    description = "Generate metadata for Java 1.8 reflection on method parameters.",
  )
  public var javaParameters: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = JVM_DEFAULT_MODES,
    gradleInputType = INPUT,
    gradleName = "jvmDefault",
  )
  @Argument(
    value = "-jvm-default",
    valueDescription = "{enable|no-compatibility|disable}",
    description = "Emit JVM default methods for interface declarations with bodies. The default is 'enable'.\n-jvm-default=enable              Generate default methods for non-abstract interface declarations, as well as 'DefaultImpls' classes with\n                                 static methods for compatibility with code compiled in the 'disable' mode.\n                                 This is the default behavior since language version 2.2.\n-jvm-default=no-compatibility    Generate default methods for non-abstract interface declarations. Do not generate 'DefaultImpls' classes.\n-jvm-default=disable             Do not generate JVM default methods. This is the default behavior up to language version 2.1.",
  )
  public var jvmDefaultStable: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xallow-unstable-dependencies",
    description = "Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.",
  )
  public var allowUnstableDependencies: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xabi-stability",
    valueDescription = "{stable|unstable}",
    description = "When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable\nto prevent diagnostics from being reported when using stable compilers at the call site.\nWhen using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable\nto force diagnostics to be reported.",
  )
  public var abiStability: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xir-do-not-clear-binding-context",
    description = "When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.",
  )
  public var doNotClearBindingContext: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xbackend-threads",
    valueDescription = "<N>",
    description = "Run codegen phase in N parallel threads.\n0 means use one thread per processor core.\nThe default value is 1.",
  )
  public var backendThreads: String = "1"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xmodule-path",
    valueDescription = "<path>",
    description = "Paths to Java 9+ modules.",
  )
  public var javaModulePath: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xadd-modules",
    valueDescription = "<module[,]>",
    description = "Root modules to resolve in addition to the initial modules, or all modules on the module path if <module> is ALL-MODULE-PATH.",
  )
  public var additionalJavaModules: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-call-assertions",
    description = "Don't generate not-null assertions for arguments of platform types.",
  )
  public var noCallAssertions: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-receiver-assertions",
    description = "Don't generate not-null assertions for extension receiver arguments of platform types.",
  )
  public var noReceiverAssertions: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-param-assertions",
    description = "Don't generate not-null assertions on parameters of methods accessible from Java.",
  )
  public var noParamAssertions: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-optimize",
    description = "Disable optimizations.",
  )
  public var noOptimize: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xassertions",
    valueDescription = "{always-enable|always-disable|jvm|legacy}",
    description = "'kotlin.assert' call behavior:\n-Xassertions=always-enable:  enable, ignore JVM assertion settings;\n-Xassertions=always-disable: disable, ignore JVM assertion settings;\n-Xassertions=jvm:            enable, depend on JVM assertion settings;\n-Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package;\ndefault: legacy",
  )
  public var assertionsMode: String? = "legacy"
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) "legacy" else value
    }

  @Argument(
    value = "-Xbuild-file",
    deprecatedName = "-module",
    valueDescription = "<path>",
    description = "Path to the .xml build file to compile.",
  )
  public var buildFile: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xmultifile-parts-inherit",
    description = "Compile multifile classes as a hierarchy of parts and a facade.",
  )
  public var inheritMultifileParts: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-type-table",
    description = "Use a type table in metadata serialization.",
  )
  public var useTypeTable: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-old-class-files-reading",
    description = "Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop.\nThis can be used in the event of problems with the new implementation.",
  )
  public var useOldClassFilesReading: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-fast-jar-file-system",
    description = "Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental.",
  )
  public var useFastJarFileSystem: Boolean? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsuppress-missing-builtins-error",
    description = "Suppress the \"cannot access built-in declaration\" error (useful with '-no-stdlib').",
  )
  public var suppressMissingBuiltinsError: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xscript-resolver-environment",
    valueDescription = "<key=value[,]>",
    description = "Set the script resolver environment in key-value pairs (the value can be quoted and escaped).",
  )
  public var scriptResolverEnvironment: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-javac",
    description = "Use javac for Java source and class file analysis.",
  )
  public var useJavac: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcompile-java",
    description = "Reuse 'javac' analysis and compile Java source files.",
  )
  public var compileJava: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xjavac-arguments",
    valueDescription = "<option[,]>",
    description = "Java compiler arguments.",
  )
  public var javacArguments: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xjava-source-roots",
    valueDescription = "<path>",
    description = "Paths to directories with Java source files.",
  )
  public var javaSourceRoots: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xjava-package-prefix",
    description = "Package prefix for Java files.",
  )
  public var javaPackagePrefix: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xjsr305",
    deprecatedName = "-Xjsr305-annotations",
    valueDescription = "{ignore/strict/warn}|under-migration:{ignore/strict/warn}|@<fq.name>:{ignore/strict/warn}",
    description = "Specify the behavior of 'JSR-305' nullability annotations:\n-Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations)\n-Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations\n-Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name\nModes:\n* ignore\n* strict (experimental; treat like other supported nullability annotations)\n* warn (report a warning)",
  )
  public var jsr305: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xnullability-annotations",
    valueDescription = "@<fq.name>:{ignore/strict/warn}",
    description = "Specify the behavior for specific Java nullability annotations (provided with fully qualified package name).\nModes:\n* ignore\n* strict\n* warn (report a warning)",
  )
  public var nullabilityAnnotations: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsupport-compatqual-checker-framework-annotations",
    valueDescription = "enable|disable",
    description = "Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl').\nThe default value is 'enable'.",
  )
  public var supportCompatqualCheckerFrameworkAnnotations: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xjspecify-annotations",
    valueDescription = "ignore|strict|warn",
    description = "Specify the behavior of 'jspecify' annotations.\nThe default value is 'warn'.",
  )
  public var jspecifyAnnotations: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xjvm-default",
    valueDescription = "{all|all-compatibility|disable}",
    description = "This option is deprecated. Migrate to -jvm-default as follows:\n-Xjvm-default=disable            -> -jvm-default=disable\n-Xjvm-default=all-compatibility  -> -jvm-default=enable\n-Xjvm-default=all                -> -jvm-default=no-compatibility",
  )
  public var jvmDefault: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xdefault-script-extension",
    valueDescription = "<script filename extension>",
    description = "Compile expressions and unrecognized scripts passed with the -script argument as scripts with the given filename extension.",
  )
  public var defaultScriptExtension: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xdisable-standard-script",
    description = "Disable standard Kotlin scripting support.",
  )
  public var disableStandardScript: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xgenerate-strict-metadata-version",
    description = "Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt').",
  )
  public var strictMetadataVersionSemantics: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsanitize-parentheses",
    description = "Transform '(' and ')' in method names to some other character sequence.\nThis mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for\nproblems with parentheses in identifiers on certain platforms.",
  )
  public var sanitizeParentheses: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfriend-paths",
    valueDescription = "<path>",
    description = "Paths to output directories for friend modules (modules whose internals should be visible).",
  )
  public var friendPaths: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xallow-no-source-files",
    description = "Allow the set of source files to be empty.",
  )
  public var allowNoSourceFiles: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xemit-jvm-type-annotations",
    description = "Emit JVM type annotations in bytecode.",
  )
  public var emitJvmTypeAnnotations: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xjvm-expose-boxed",
    description = "Expose inline classes and functions, accepting and returning them, to Java.",
  )
  @Enables(ImplicitJvmExposeBoxed)
  public var jvmExposeBoxed: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xstring-concat",
    valueDescription = "{indy-with-constants|indy|inline}",
    description = "Select the code generation scheme for string concatenation:\n-Xstring-concat=indy-with-constants  Concatenate strings using 'invokedynamic' and 'makeConcatWithConstants'. This requires '-jvm-target 9' or greater.\n-Xstring-concat=indy                 Concatenate strings using 'invokedynamic' and 'makeConcat'. This requires '-jvm-target 9' or greater.\n-Xstring-concat=inline               Concatenate strings using 'StringBuilder'\ndefault: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise.",
  )
  public var stringConcat: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xjdk-release",
    valueDescription = "<version>",
    description = "Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer.\nThe supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–24.\nThis also sets the value of '-jvm-target' to be equal to the selected JDK version.",
  )
  public var jdkRelease: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xsam-conversions",
    valueDescription = "{class|indy}",
    description = "Select the code generation scheme for SAM conversions.\n-Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'.\n-Xsam-conversions=class         Generate SAM conversions as explicit classes.\nThe default value is 'indy'.",
  )
  public var samConversions: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xlambdas",
    valueDescription = "{class|indy}",
    description = "Select the code generation scheme for lambdas.\n-Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'.\n                                A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'.\n-Xlambdas=class                 Generate lambdas as explicit classes.\nThe default value is 'indy' if language version is 2.0+, and 'class' otherwise.",
  )
  public var lambdas: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xindy-allow-annotated-lambdas",
    description = "Allow using 'invokedynamic' for lambda expressions with annotations",
  )
  public var indyAllowAnnotatedLambdas: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xklib",
    valueDescription = "<path>",
    description = "Paths to cross-platform libraries in the .klib format.",
  )
  public var klibLibraries: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xno-reset-jar-timestamps",
    description = "Don't reset jar entry timestamps to a fixed date.",
  )
  public var noResetJarTimestamps: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-unified-null-checks",
    description = "Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details.",
  )
  public var noUnifiedNullChecks: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-source-debug-extension",
    description = "Don't generate the '@kotlin.jvm.internal.SourceDebugExtension' annotation with an SMAP copy on classes.",
  )
  public var noSourceDebugExtension: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xprofile",
    valueDescription = "<profilerPath:command:outputDir>",
    description = "Debug option: Run the compiler with the async profiler and save snapshots to `outputDir`; `command` is passed to the async profiler on start.\n`profilerPath` is the path to libasyncProfiler.so; async-profiler.jar should be on the compiler classpath.\nIf it's not on the classpath, the compiler will attempt to load async-profiler.jar from the containing directory of profilerPath.\nExample: -Xprofile=<PATH_TO_ASYNC_PROFILER>/async-profiler/build/libasyncProfiler.so:event=cpu,interval=1ms,threads,start:<SNAPSHOT_DIR_PATH>",
  )
  public var profileCompilerCommand: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xuse-14-inline-classes-mangling-scheme",
    description = "Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30.",
  )
  public var useOldInlineClassesManglingScheme: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xjvm-enable-preview",
    description = "Allow using Java features that are in the preview phase.\nThis works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments.",
  )
  public var enableJvmPreview: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsuppress-deprecated-jvm-target-warning",
    description = "Suppress warnings about deprecated JVM target versions.\nThis option has no effect and will be deleted in a future version.",
  )
  public var suppressDeprecatedJvmTargetWarning: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xtype-enhancement-improvements-strict-mode",
    description = "Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,\nincluding the ability to read type-use annotations from class files.\nSee KT-45671 for more details.",
  )
  @Enables(TypeEnhancementImprovementsInStrictMode)
  public var typeEnhancementImprovementsInStrictMode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xserialize-ir",
    valueDescription = "{none|inline|all}",
    description = "Save the IR to metadata (Experimental).",
  )
  public var serializeIr: String = "none"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xvalidate-bytecode",
    description = "Validate generated JVM bytecode before and after optimizations.",
  )
  public var validateBytecode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xenhance-type-parameter-types-to-def-not-null",
    description = "Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').",
  )
  @Enables(ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated)
  public var enhanceTypeParameterTypesToDefNotNull: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xlink-via-signatures",
    description = "Link JVM IR symbols via signatures instead of descriptors.\nThis mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.\nThis option is deprecated and will be deleted in future versions.\nIt has no effect when -language-version is 2.0 or higher.",
  )
  public var linkViaSignatures: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdebug",
    description = "Enable debug mode for compilation.\nCurrently this includes spilling all variables in a suspending context regardless of whether they are alive.\nIf API Level >= 2.2 -- no-op.",
  )
  public var enableDebugMode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xenhanced-coroutines-debugging",
    description = "Generate additional linenumber instruction for compiler-generated code\ninside suspend functions and lambdas to distinguish them from user code by debugger.",
  )
  public var enhancedCoroutinesDebugging: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-new-java-annotation-targets",
    description = "Don't generate Java 1.8+ targets for Kotlin annotation classes.",
  )
  public var noNewJavaAnnotationTargets: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xvalue-classes",
    description = "Enable experimental value classes.",
  )
  @Enables(ValueClasses)
  public var valueClasses: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xir-inliner",
    description = "Inline functions using the IR inliner instead of the bytecode inliner.",
  )
  public var enableIrInliner: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-inline-scopes-numbers",
    description = "Use inline scopes numbers for inline marker variables.",
  )
  public var useInlineScopesNumbers: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-k2-kapt",
    description = "Enable the experimental support for K2 KAPT.",
  )
  public var useK2Kapt: Boolean? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcompile-builtins-as-part-of-stdlib",
    description = "Enable behaviour needed to compile builtins as part of JVM stdlib",
  )
  public var expectBuiltinsAsPartOfStdlib: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xoutput-builtins-metadata",
    description = "Output builtins metadata as .kotlin_builtins files",
  )
  public var outputBuiltinsMetadata: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xannotations-in-metadata",
    description = "Write annotations on declarations into the metadata (in addition to the JVM bytecode), and read annotations from the metadata if they are present.",
  )
  @Enables(AnnotationsInMetadata)
  public var annotationsInMetadata: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @get:AnnotationsTransient
  @field:JvmTransient
  override val configurator: CommonCompilerArgumentsConfigurator =
      K2JVMCompilerArgumentsConfigurator()

  override fun copyOf(): Freezable = copyK2JVMCompilerArguments(this, K2JVMCompilerArguments())
}
