// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import java.io.File
import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.io.path.Path
import kotlin.text.split
import org.jetbrains.kotlin.buildtools.`internal`.compat.DeepCopyable
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NullabilityAnnotation
import org.jetbrains.kotlin.buildtools.api.arguments.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AbiStabilityMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AssertionsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.CompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JdkRelease
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.arguments.enums.LambdasMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SamConversionsMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.StringConcatMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WhenExpressionsMode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyK2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JvmCompilerArgumentsImpl(
  protected override val compilerArguments: K2JVMCompilerArguments = K2JVMCompilerArguments(),
  protected override val optionsMap: MutableMap<String, Any?> = mutableMapOf(),
) : CommonCompilerArgumentsImpl(compilerArguments, optionsMap),
    JvmCompilerArguments,
    JvmCompilerArguments.Builder,
    DeepCopyable<JvmCompilerArgumentsImpl> {
  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V = getOption(key.id) as V

  private operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    setOption(key.id, value)
  }

  public operator fun contains(key: JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = getOption(key.id) as V

  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    val currentKotlinVersion = KotlinToolingVersion(KC_VERSION)
    if (key.availableSinceVersion > KotlinReleaseVersion(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    setOption(key.id, value)
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: JvmCompilerArguments.JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun getOption(keyId: String): Any? = when (keyId) {
    "X_ABI_STABILITY" -> {
    this.compilerArguments.abiStability?.let { AbiStabilityMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") }
    }
    "X_ADD_MODULES" -> {
    this.compilerArguments.additionalJavaModules.toListOrEmpty()
    }
    "X_ALLOW_NO_SOURCE_FILES" -> {
    this.compilerArguments.allowNoSourceFiles
    }
    "X_ALLOW_UNSTABLE_DEPENDENCIES" -> {
    this.compilerArguments.allowUnstableDependencies
    }
    "X_ANNOTATIONS_IN_METADATA" -> {
    try {
    try { this.compilerArguments.annotationsInMetadata } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATIONS_IN_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_ASSERTIONS" -> {
    this.compilerArguments.assertionsMode?.let { AssertionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") }
    }
    "X_BACKEND_THREADS" -> {
    this.compilerArguments.backendThreads.let { it.toInt() }
    }
    "X_BUILD_FILE" -> {
    this.compilerArguments.buildFile
    }
    "X_COMPILE_BUILTINS_AS_PART_OF_STDLIB" -> {
    try {
    try { this.compilerArguments.getUsingReflection<Boolean>("expectBuiltinsAsPartOfStdlib") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20 and removed in 2.3.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_COMPILE_JAVA" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("compileJava") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_JAVA. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    }
    "X_DEBUG" -> {
    this.compilerArguments.enableDebugMode
    }
    "X_DEFAULT_SCRIPT_EXTENSION" -> {
    this.compilerArguments.defaultScriptExtension
    }
    "X_DISABLE_STANDARD_SCRIPT" -> {
    this.compilerArguments.disableStandardScript
    }
    "X_EMIT_JVM_TYPE_ANNOTATIONS" -> {
    this.compilerArguments.emitJvmTypeAnnotations
    }
    "X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL" -> {
    this.compilerArguments.enhanceTypeParameterTypesToDefNotNull
    }
    "X_ENHANCED_COROUTINES_DEBUGGING" -> {
    try {
    try { this.compilerArguments.enhancedCoroutinesDebugging } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCED_COROUTINES_DEBUGGING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_FRIEND_PATHS" -> {
    this.compilerArguments.friendPaths.mapOrEmpty { Path(it) }
    }
    "X_GENERATE_STRICT_METADATA_VERSION" -> {
    this.compilerArguments.strictMetadataVersionSemantics
    }
    "X_INDY_ALLOW_ANNOTATED_LAMBDAS" -> {
    try {
    try { this.compilerArguments.indyAllowAnnotatedLambdas } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INDY_ALLOW_ANNOTATED_LAMBDAS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_IR_DO_NOT_CLEAR_BINDING_CONTEXT" -> {
    this.compilerArguments.doNotClearBindingContext
    }
    "X_IR_INLINER" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("enableIrInliner") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    }
    "X_JAVA_PACKAGE_PREFIX" -> {
    this.compilerArguments.javaPackagePrefix
    }
    "X_JAVA_SOURCE_ROOTS" -> {
    this.compilerArguments.javaSourceRoots.mapOrEmpty { Path(it) }
    }
    "X_JAVAC_ARGUMENTS" -> {
    try { this.compilerArguments.getUsingReflection<Array<String>>("javacArguments") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JAVAC_ARGUMENTS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    }
    "X_JDK_RELEASE" -> {
    this.compilerArguments.jdkRelease?.let { JdkRelease.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") }
    }
    "X_JSPECIFY_ANNOTATIONS" -> {
    this.compilerArguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") }
    }
    "X_JVM_DEFAULT" -> {
    this.compilerArguments.jvmDefault
    }
    "X_JVM_ENABLE_PREVIEW" -> {
    this.compilerArguments.enableJvmPreview
    }
    "X_JVM_EXPOSE_BOXED" -> {
    try {
    try { this.compilerArguments.jvmExposeBoxed } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JVM_EXPOSE_BOXED. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_KLIB" -> {
    try { this.compilerArguments.getUsingReflection<String?>("klibLibraries")?.split(File.pathSeparator)?.map { Path(it) } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_KLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_LAMBDAS" -> {
    this.compilerArguments.lambdas?.let { LambdasMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") }
    }
    "X_LINK_VIA_SIGNATURES" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("linkViaSignatures") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_LINK_VIA_SIGNATURES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_MODULE_PATH" -> {
    this.compilerArguments.javaModulePath?.split(File.pathSeparator)?.map { Path(it) }
    }
    "X_MULTIFILE_PARTS_INHERIT" -> {
    this.compilerArguments.inheritMultifileParts
    }
    "X_NO_CALL_ASSERTIONS" -> {
    this.compilerArguments.noCallAssertions
    }
    "X_NO_NEW_JAVA_ANNOTATION_TARGETS" -> {
    this.compilerArguments.noNewJavaAnnotationTargets
    }
    "X_NO_OPTIMIZE" -> {
    this.compilerArguments.noOptimize
    }
    "X_NO_PARAM_ASSERTIONS" -> {
    this.compilerArguments.noParamAssertions
    }
    "X_NO_RECEIVER_ASSERTIONS" -> {
    this.compilerArguments.noReceiverAssertions
    }
    "X_NO_RESET_JAR_TIMESTAMPS" -> {
    this.compilerArguments.noResetJarTimestamps
    }
    "X_NO_SOURCE_DEBUG_EXTENSION" -> {
    this.compilerArguments.noSourceDebugExtension
    }
    "X_NO_UNIFIED_NULL_CHECKS" -> {
    this.compilerArguments.noUnifiedNullChecks
    }
    "X_OUTPUT_BUILTINS_METADATA" -> {
    try {
    try { this.compilerArguments.outputBuiltinsMetadata } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_OUTPUT_BUILTINS_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_SAM_CONVERSIONS" -> {
    this.compilerArguments.samConversions?.let { SamConversionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") }
    }
    "X_SANITIZE_PARENTHESES" -> {
    this.compilerArguments.sanitizeParentheses
    }
    "X_SCRIPT_RESOLVER_ENVIRONMENT" -> {
    this.compilerArguments.scriptResolverEnvironment.toListOrEmpty()
    }
    "X_SERIALIZE_IR" -> {
    try { this.compilerArguments.getUsingReflection<String>("serializeIr") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SERIALIZE_IR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    }
    "X_STRING_CONCAT" -> {
    this.compilerArguments.stringConcat?.let { StringConcatMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") }
    }
    "X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS" -> {
    this.compilerArguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") }
    }
    "X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("suppressDeprecatedJvmTargetWarning") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    }
    "X_SUPPRESS_MISSING_BUILTINS_ERROR" -> {
    this.compilerArguments.suppressMissingBuiltinsError
    }
    "X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE" -> {
    this.compilerArguments.typeEnhancementImprovementsInStrictMode
    }
    "X_USE_14_INLINE_CLASSES_MANGLING_SCHEME" -> {
    this.compilerArguments.useOldInlineClassesManglingScheme
    }
    "X_USE_FAST_JAR_FILE_SYSTEM" -> {
    this.compilerArguments.useFastJarFileSystem
    }
    "X_USE_INLINE_SCOPES_NUMBERS" -> {
    try {
    try { this.compilerArguments.useInlineScopesNumbers } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_INLINE_SCOPES_NUMBERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_USE_JAVAC" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("useJavac") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_JAVAC. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    }
    "X_USE_K2_KAPT" -> {
    try {
    try { this.compilerArguments.getUsingReflection<Boolean?>("useK2Kapt") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.3.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "X_USE_OLD_CLASS_FILES_READING" -> {
    this.compilerArguments.useOldClassFilesReading
    }
    "X_USE_TYPE_TABLE" -> {
    this.compilerArguments.useTypeTable
    }
    "X_VALIDATE_BYTECODE" -> {
    this.compilerArguments.validateBytecode
    }
    "X_VALUE_CLASSES" -> {
    try { this.compilerArguments.getUsingReflection<Boolean>("valueClasses") } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VALUE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    }
    "X_WHEN_EXPRESSIONS" -> {
    try {
    try { this.compilerArguments.whenExpressionsGeneration?.let { WhenExpressionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_EXPRESSIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "CLASSPATH" -> {
    this.compilerArguments.classpath?.split(File.pathSeparator)?.map { Path(it) }
    }
    "D" -> {
    this.compilerArguments.destination
    }
    "EXPRESSION" -> {
    this.compilerArguments.expression
    }
    "INCLUDE_RUNTIME" -> {
    this.compilerArguments.includeRuntime
    }
    "JAVA_PARAMETERS" -> {
    this.compilerArguments.javaParameters
    }
    "JDK_HOME" -> {
    this.compilerArguments.jdkHome?.let { Path(it) }
    }
    "JVM_DEFAULT" -> {
    try {
    try { this.compilerArguments.jvmDefaultStable?.let { JvmDefaultMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") } } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: JVM_DEFAULT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    } catch (_: NoSuchMethodError) { null }
    }
    "JVM_TARGET" -> {
    this.compilerArguments.jvmTarget?.let { JvmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") }
    }
    "MODULE_NAME" -> {
    this.compilerArguments.moduleName
    }
    "NO_JDK" -> {
    this.compilerArguments.noJdk
    }
    "NO_REFLECT" -> {
    this.compilerArguments.noReflect
    }
    "NO_STDLIB" -> {
    this.compilerArguments.noStdlib
    }
    "SCRIPT_TEMPLATES" -> {
    this.compilerArguments.scriptTemplates.toListOrEmpty()
    }
    "X_PROFILE" -> {
    applyProfileCompilerCommand(null, compilerArguments)
    }
    "X_NULLABILITY_ANNOTATIONS" -> {
    applyNullabilityAnnotations(null, compilerArguments)
    }
    "X_JSR305" -> {
    applyJsr305(null, compilerArguments)
    }
    else -> {
      check(keyId in optionsMap) { "Argument ${keyId} is not set and has no default value" }
      optionsMap[keyId]
    }
  }

  @Suppress(
    "UNCHECKED_CAST",
    "DEPRECATION",
  )
  private fun setOption(keyId: String, `value`: Any?) {
    when (keyId) {
      "X_ABI_STABILITY" -> {
      this.compilerArguments.abiStability = (value as AbiStabilityMode?)?.stringValue
      }
      "X_ADD_MODULES" -> {
      this.compilerArguments.additionalJavaModules = (value as List<String>).toTypedArray()
      }
      "X_ALLOW_NO_SOURCE_FILES" -> {
      this.compilerArguments.allowNoSourceFiles = (value as Boolean)
      }
      "X_ALLOW_UNSTABLE_DEPENDENCIES" -> {
      this.compilerArguments.allowUnstableDependencies = (value as Boolean)
      }
      "X_ANNOTATIONS_IN_METADATA" -> {
      try {
      try { this.compilerArguments.annotationsInMetadata = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATIONS_IN_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_ASSERTIONS" -> {
      this.compilerArguments.assertionsMode = (value as AssertionsMode?)?.stringValue
      }
      "X_BACKEND_THREADS" -> {
      this.compilerArguments.backendThreads = (value as Int).toString()
      }
      "X_BUILD_FILE" -> {
      this.compilerArguments.buildFile = (value as String?)
      }
      "X_COMPILE_BUILTINS_AS_PART_OF_STDLIB" -> {
      try {
      try { this.compilerArguments.setUsingReflection("expectBuiltinsAsPartOfStdlib", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20 and removed in 2.3.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_COMPILE_JAVA" -> {
      try { this.compilerArguments.setUsingReflection("compileJava", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_JAVA. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }}
      "X_DEBUG" -> {
      this.compilerArguments.enableDebugMode = (value as Boolean)
      }
      "X_DEFAULT_SCRIPT_EXTENSION" -> {
      this.compilerArguments.defaultScriptExtension = (value as String?)
      }
      "X_DISABLE_STANDARD_SCRIPT" -> {
      this.compilerArguments.disableStandardScript = (value as Boolean)
      }
      "X_EMIT_JVM_TYPE_ANNOTATIONS" -> {
      this.compilerArguments.emitJvmTypeAnnotations = (value as Boolean)
      }
      "X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL" -> {
      this.compilerArguments.enhanceTypeParameterTypesToDefNotNull = (value as Boolean)
      }
      "X_ENHANCED_COROUTINES_DEBUGGING" -> {
      try {
      try { this.compilerArguments.enhancedCoroutinesDebugging = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCED_COROUTINES_DEBUGGING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_FRIEND_PATHS" -> {
      this.compilerArguments.friendPaths = (value as List<java.nio.`file`.Path>).map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
      }
      "X_GENERATE_STRICT_METADATA_VERSION" -> {
      this.compilerArguments.strictMetadataVersionSemantics = (value as Boolean)
      }
      "X_INDY_ALLOW_ANNOTATED_LAMBDAS" -> {
      try {
      try { this.compilerArguments.indyAllowAnnotatedLambdas = (value as Boolean?)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INDY_ALLOW_ANNOTATED_LAMBDAS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_IR_DO_NOT_CLEAR_BINDING_CONTEXT" -> {
      this.compilerArguments.doNotClearBindingContext = (value as Boolean)
      }
      "X_IR_INLINER" -> {
      try { this.compilerArguments.setUsingReflection("enableIrInliner", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }}
      "X_JAVA_PACKAGE_PREFIX" -> {
      this.compilerArguments.javaPackagePrefix = (value as String?)
      }
      "X_JAVA_SOURCE_ROOTS" -> {
      this.compilerArguments.javaSourceRoots = (value as List<java.nio.`file`.Path>).map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
      }
      "X_JAVAC_ARGUMENTS" -> {
      try { this.compilerArguments.setUsingReflection("javacArguments", (value as Array<String>?) ?: emptyArray())
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JAVAC_ARGUMENTS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }}
      "X_JDK_RELEASE" -> {
      this.compilerArguments.jdkRelease = (value as JdkRelease?)?.stringValue
      }
      "X_JSPECIFY_ANNOTATIONS" -> {
      this.compilerArguments.jspecifyAnnotations = (value as JspecifyAnnotationsMode?)?.stringValue
      }
      "X_JVM_DEFAULT" -> {
      this.compilerArguments.jvmDefault = (value as String?)
      }
      "X_JVM_ENABLE_PREVIEW" -> {
      this.compilerArguments.enableJvmPreview = (value as Boolean)
      }
      "X_JVM_EXPOSE_BOXED" -> {
      try {
      try { this.compilerArguments.jvmExposeBoxed = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JVM_EXPOSE_BOXED. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_KLIB" -> {
      try { this.compilerArguments.setUsingReflection("klibLibraries", (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_KLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_LAMBDAS" -> {
      this.compilerArguments.lambdas = (value as LambdasMode?)?.stringValue
      }
      "X_LINK_VIA_SIGNATURES" -> {
      try { this.compilerArguments.setUsingReflection("linkViaSignatures", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_LINK_VIA_SIGNATURES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_MODULE_PATH" -> {
      this.compilerArguments.javaModulePath = (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
      }
      "X_MULTIFILE_PARTS_INHERIT" -> {
      this.compilerArguments.inheritMultifileParts = (value as Boolean)
      }
      "X_NO_CALL_ASSERTIONS" -> {
      this.compilerArguments.noCallAssertions = (value as Boolean)
      }
      "X_NO_NEW_JAVA_ANNOTATION_TARGETS" -> {
      this.compilerArguments.noNewJavaAnnotationTargets = (value as Boolean)
      }
      "X_NO_OPTIMIZE" -> {
      this.compilerArguments.noOptimize = (value as Boolean)
      }
      "X_NO_PARAM_ASSERTIONS" -> {
      this.compilerArguments.noParamAssertions = (value as Boolean)
      }
      "X_NO_RECEIVER_ASSERTIONS" -> {
      this.compilerArguments.noReceiverAssertions = (value as Boolean)
      }
      "X_NO_RESET_JAR_TIMESTAMPS" -> {
      this.compilerArguments.noResetJarTimestamps = (value as Boolean)
      }
      "X_NO_SOURCE_DEBUG_EXTENSION" -> {
      this.compilerArguments.noSourceDebugExtension = (value as Boolean)
      }
      "X_NO_UNIFIED_NULL_CHECKS" -> {
      this.compilerArguments.noUnifiedNullChecks = (value as Boolean)
      }
      "X_OUTPUT_BUILTINS_METADATA" -> {
      try {
      try { this.compilerArguments.outputBuiltinsMetadata = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_OUTPUT_BUILTINS_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_SAM_CONVERSIONS" -> {
      this.compilerArguments.samConversions = (value as SamConversionsMode?)?.stringValue
      }
      "X_SANITIZE_PARENTHESES" -> {
      this.compilerArguments.sanitizeParentheses = (value as Boolean)
      }
      "X_SCRIPT_RESOLVER_ENVIRONMENT" -> {
      this.compilerArguments.scriptResolverEnvironment = (value as List<String>).toTypedArray()
      }
      "X_SERIALIZE_IR" -> {
      try { this.compilerArguments.setUsingReflection("serializeIr", (value as String))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SERIALIZE_IR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }}
      "X_STRING_CONCAT" -> {
      this.compilerArguments.stringConcat = (value as StringConcatMode?)?.stringValue
      }
      "X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS" -> {
      this.compilerArguments.supportCompatqualCheckerFrameworkAnnotations = (value as CompatqualAnnotationsMode?)?.stringValue
      }
      "X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING" -> {
      try { this.compilerArguments.setUsingReflection("suppressDeprecatedJvmTargetWarning", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }}
      "X_SUPPRESS_MISSING_BUILTINS_ERROR" -> {
      this.compilerArguments.suppressMissingBuiltinsError = (value as Boolean)
      }
      "X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE" -> {
      this.compilerArguments.typeEnhancementImprovementsInStrictMode = (value as Boolean)
      }
      "X_USE_14_INLINE_CLASSES_MANGLING_SCHEME" -> {
      this.compilerArguments.useOldInlineClassesManglingScheme = (value as Boolean)
      }
      "X_USE_FAST_JAR_FILE_SYSTEM" -> {
      this.compilerArguments.useFastJarFileSystem = (value as Boolean?)
      }
      "X_USE_INLINE_SCOPES_NUMBERS" -> {
      try {
      try { this.compilerArguments.useInlineScopesNumbers = (value as Boolean)
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_INLINE_SCOPES_NUMBERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_USE_JAVAC" -> {
      try { this.compilerArguments.setUsingReflection("useJavac", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_JAVAC. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }}
      "X_USE_K2_KAPT" -> {
      try {
      try { this.compilerArguments.setUsingReflection("useK2Kapt", (value as Boolean?))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.3.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "X_USE_OLD_CLASS_FILES_READING" -> {
      this.compilerArguments.useOldClassFilesReading = (value as Boolean)
      }
      "X_USE_TYPE_TABLE" -> {
      this.compilerArguments.useTypeTable = (value as Boolean)
      }
      "X_VALIDATE_BYTECODE" -> {
      this.compilerArguments.validateBytecode = (value as Boolean)
      }
      "X_VALUE_CLASSES" -> {
      try { this.compilerArguments.setUsingReflection("valueClasses", (value as Boolean))
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VALUE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }}
      "X_WHEN_EXPRESSIONS" -> {
      try {
      try { this.compilerArguments.whenExpressionsGeneration = (value as WhenExpressionsMode?)?.stringValue
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_EXPRESSIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "CLASSPATH" -> {
      this.compilerArguments.classpath = (value as List<java.nio.`file`.Path>?)?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
      }
      "D" -> {
      this.compilerArguments.destination = (value as String?)
      }
      "EXPRESSION" -> {
      this.compilerArguments.expression = (value as String?)
      }
      "INCLUDE_RUNTIME" -> {
      this.compilerArguments.includeRuntime = (value as Boolean)
      }
      "JAVA_PARAMETERS" -> {
      this.compilerArguments.javaParameters = (value as Boolean)
      }
      "JDK_HOME" -> {
      this.compilerArguments.jdkHome = (value as java.nio.`file`.Path?)?.absolutePathStringOrThrow()
      }
      "JVM_DEFAULT" -> {
      try {
      try { this.compilerArguments.jvmDefaultStable = (value as JvmDefaultMode?)?.stringValue
       } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: JVM_DEFAULT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }} catch (_: NoSuchMethodError) { }
      }
      "JVM_TARGET" -> {
      this.compilerArguments.jvmTarget = (value as JvmTarget?)?.stringValue
      }
      "MODULE_NAME" -> {
      this.compilerArguments.moduleName = (value as String?)
      }
      "NO_JDK" -> {
      this.compilerArguments.noJdk = (value as Boolean)
      }
      "NO_REFLECT" -> {
      this.compilerArguments.noReflect = (value as Boolean)
      }
      "NO_STDLIB" -> {
      this.compilerArguments.noStdlib = (value as Boolean)
      }
      "SCRIPT_TEMPLATES" -> {
      this.compilerArguments.scriptTemplates = (value as List<String>).toTypedArray()
      }
      "X_PROFILE" -> {
      compilerArguments.applyProfileCompilerCommand(value as ProfileCompilerCommand?)}
      "X_NULLABILITY_ANNOTATIONS" -> {
      compilerArguments.applyNullabilityAnnotations(value as List<NullabilityAnnotation>)}
      "X_JSR305" -> {
      compilerArguments.applyJsr305(value as List<Jsr305>)}
      else -> optionsMap[keyId] = value
    }
  }

  override fun deepCopy(): JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(org.jetbrains.kotlin.cli.common.arguments.copyK2JVMCompilerArguments(this.compilerArguments, org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments()).also { newArgs -> newArgs.errors = this.compilerArguments.errors } , optionsMap)

  override fun build(): JvmCompilerArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2JVMCompilerArguments {
    val arguments = copyK2JVMCompilerArguments(compilerArguments, K2JVMCompilerArguments()).also { newArgs -> newArgs.errors = compilerArguments.errors } 
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filterNot { isArgumentKnown(it) }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    return arguments
  }

  protected fun applyCompilerArguments(arguments: K2JVMCompilerArguments) {
    copyK2JVMCompilerArguments(arguments, this.compilerArguments).also { newArgs -> newArgs.errors = arguments.errors } 
    super.applyCompilerArguments(arguments)
  }

  protected override fun isArgumentKnown(name: String): Boolean = name in knownArguments || super.isArgumentKnown(name)

  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JVMCompilerArguments = parseCommandLineArguments(arguments)
    validateArguments(compilerArgs.errors)?.let { throw CompilerArgumentsParseException(it) }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings()
    return arguments
  }

  public class JvmCompilerArgument<V>(
    public val id: String,
  ) {
    init {
      knownArguments.add(id)}
  }

  public companion object {
    private val knownArguments: MutableSet<String> = mutableSetOf()

    public val X_ABI_STABILITY: JvmCompilerArgument<AbiStabilityMode?> =
        JvmCompilerArgument("X_ABI_STABILITY")

    public val X_ADD_MODULES: JvmCompilerArgument<List<String>> =
        JvmCompilerArgument("X_ADD_MODULES")

    public val X_ALLOW_NO_SOURCE_FILES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_NO_SOURCE_FILES")

    public val X_ALLOW_UNSTABLE_DEPENDENCIES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_UNSTABLE_DEPENDENCIES")

    public val X_ANNOTATIONS_IN_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ANNOTATIONS_IN_METADATA")

    public val X_ASSERTIONS: JvmCompilerArgument<AssertionsMode?> =
        JvmCompilerArgument("X_ASSERTIONS")

    public val X_BACKEND_THREADS: JvmCompilerArgument<Int> =
        JvmCompilerArgument("X_BACKEND_THREADS")

    public val X_BUILD_FILE: JvmCompilerArgument<String?> = JvmCompilerArgument("X_BUILD_FILE")

    public val X_COMPILE_BUILTINS_AS_PART_OF_STDLIB: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB")

    public val X_COMPILE_JAVA: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_COMPILE_JAVA")

    public val X_DEBUG: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_DEBUG")

    public val X_DEFAULT_SCRIPT_EXTENSION: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_DEFAULT_SCRIPT_EXTENSION")

    public val X_DISABLE_STANDARD_SCRIPT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_DISABLE_STANDARD_SCRIPT")

    public val X_EMIT_JVM_TYPE_ANNOTATIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_EMIT_JVM_TYPE_ANNOTATIONS")

    public val X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL")

    public val X_ENHANCED_COROUTINES_DEBUGGING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCED_COROUTINES_DEBUGGING")

    public val X_FRIEND_PATHS: JvmCompilerArgument<List<java.nio.`file`.Path>> =
        JvmCompilerArgument("X_FRIEND_PATHS")

    public val X_GENERATE_STRICT_METADATA_VERSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_GENERATE_STRICT_METADATA_VERSION")

    public val X_INDY_ALLOW_ANNOTATED_LAMBDAS: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_INDY_ALLOW_ANNOTATED_LAMBDAS")

    public val X_IR_DO_NOT_CLEAR_BINDING_CONTEXT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT")

    public val X_IR_INLINER: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_IR_INLINER")

    public val X_JAVA_PACKAGE_PREFIX: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JAVA_PACKAGE_PREFIX")

    public val X_JAVA_SOURCE_ROOTS: JvmCompilerArgument<List<java.nio.`file`.Path>> =
        JvmCompilerArgument("X_JAVA_SOURCE_ROOTS")

    public val X_JAVAC_ARGUMENTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_JAVAC_ARGUMENTS")

    public val X_JDK_RELEASE: JvmCompilerArgument<JdkRelease?> =
        JvmCompilerArgument("X_JDK_RELEASE")

    public val X_JSPECIFY_ANNOTATIONS: JvmCompilerArgument<JspecifyAnnotationsMode?> =
        JvmCompilerArgument("X_JSPECIFY_ANNOTATIONS")

    public val X_JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JVM_DEFAULT")

    public val X_JVM_ENABLE_PREVIEW: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_ENABLE_PREVIEW")

    public val X_JVM_EXPOSE_BOXED: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_EXPOSE_BOXED")

    public val X_KLIB: JvmCompilerArgument<List<java.nio.`file`.Path>?> =
        JvmCompilerArgument("X_KLIB")

    public val X_LAMBDAS: JvmCompilerArgument<LambdasMode?> = JvmCompilerArgument("X_LAMBDAS")

    public val X_LINK_VIA_SIGNATURES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_LINK_VIA_SIGNATURES")

    public val X_MODULE_PATH: JvmCompilerArgument<List<java.nio.`file`.Path>?> =
        JvmCompilerArgument("X_MODULE_PATH")

    public val X_MULTIFILE_PARTS_INHERIT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_MULTIFILE_PARTS_INHERIT")

    public val X_NO_CALL_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_CALL_ASSERTIONS")

    public val X_NO_NEW_JAVA_ANNOTATION_TARGETS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_NEW_JAVA_ANNOTATION_TARGETS")

    public val X_NO_OPTIMIZE: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_NO_OPTIMIZE")

    public val X_NO_PARAM_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_PARAM_ASSERTIONS")

    public val X_NO_RECEIVER_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_RECEIVER_ASSERTIONS")

    public val X_NO_RESET_JAR_TIMESTAMPS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_RESET_JAR_TIMESTAMPS")

    public val X_NO_SOURCE_DEBUG_EXTENSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_SOURCE_DEBUG_EXTENSION")

    public val X_NO_UNIFIED_NULL_CHECKS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_UNIFIED_NULL_CHECKS")

    public val X_OUTPUT_BUILTINS_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_OUTPUT_BUILTINS_METADATA")

    public val X_SAM_CONVERSIONS: JvmCompilerArgument<SamConversionsMode?> =
        JvmCompilerArgument("X_SAM_CONVERSIONS")

    public val X_SANITIZE_PARENTHESES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SANITIZE_PARENTHESES")

    public val X_SCRIPT_RESOLVER_ENVIRONMENT: JvmCompilerArgument<List<String>> =
        JvmCompilerArgument("X_SCRIPT_RESOLVER_ENVIRONMENT")

    public val X_SERIALIZE_IR: JvmCompilerArgument<String> = JvmCompilerArgument("X_SERIALIZE_IR")

    public val X_STRING_CONCAT: JvmCompilerArgument<StringConcatMode?> =
        JvmCompilerArgument("X_STRING_CONCAT")

    public val X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS:
        JvmCompilerArgument<CompatqualAnnotationsMode?> =
        JvmCompilerArgument("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")

    public val X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING")

    public val X_SUPPRESS_MISSING_BUILTINS_ERROR: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_MISSING_BUILTINS_ERROR")

    public val X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE")

    public val X_USE_14_INLINE_CLASSES_MANGLING_SCHEME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")

    public val X_USE_FAST_JAR_FILE_SYSTEM: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_USE_FAST_JAR_FILE_SYSTEM")

    public val X_USE_INLINE_SCOPES_NUMBERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_INLINE_SCOPES_NUMBERS")

    public val X_USE_JAVAC: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_USE_JAVAC")

    public val X_USE_K2_KAPT: JvmCompilerArgument<Boolean?> = JvmCompilerArgument("X_USE_K2_KAPT")

    public val X_USE_OLD_CLASS_FILES_READING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_OLD_CLASS_FILES_READING")

    public val X_USE_TYPE_TABLE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_TYPE_TABLE")

    public val X_VALIDATE_BYTECODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALIDATE_BYTECODE")

    public val X_VALUE_CLASSES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALUE_CLASSES")

    public val X_WHEN_EXPRESSIONS: JvmCompilerArgument<WhenExpressionsMode?> =
        JvmCompilerArgument("X_WHEN_EXPRESSIONS")

    public val CLASSPATH: JvmCompilerArgument<List<java.nio.`file`.Path>?> =
        JvmCompilerArgument("CLASSPATH")

    public val D: JvmCompilerArgument<String?> = JvmCompilerArgument("D")

    public val EXPRESSION: JvmCompilerArgument<String?> = JvmCompilerArgument("EXPRESSION")

    public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("INCLUDE_RUNTIME")

    public val JAVA_PARAMETERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("JAVA_PARAMETERS")

    public val JDK_HOME: JvmCompilerArgument<java.nio.`file`.Path?> =
        JvmCompilerArgument("JDK_HOME")

    public val JVM_DEFAULT: JvmCompilerArgument<JvmDefaultMode?> =
        JvmCompilerArgument("JVM_DEFAULT")

    public val JVM_TARGET: JvmCompilerArgument<JvmTarget?> = JvmCompilerArgument("JVM_TARGET")

    public val MODULE_NAME: JvmCompilerArgument<String?> = JvmCompilerArgument("MODULE_NAME")

    public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

    public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

    public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")

    public val SCRIPT_TEMPLATES: JvmCompilerArgument<List<String>> =
        JvmCompilerArgument("SCRIPT_TEMPLATES")

    public val X_PROFILE: JvmCompilerArgument<ProfileCompilerCommand?> =
        JvmCompilerArgument("X_PROFILE")

    public val X_NULLABILITY_ANNOTATIONS: JvmCompilerArgument<List<NullabilityAnnotation>> =
        JvmCompilerArgument("X_NULLABILITY_ANNOTATIONS")

    public val X_JSR305: JvmCompilerArgument<List<Jsr305>> = JvmCompilerArgument("X_JSR305")
  }
}
