// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalCompilerArgument::class)
@file:Suppress("EnumValuesSoftDeprecate")

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import java.io.File
import java.lang.IllegalStateException
import java.nio.`file`.Path
import kotlin.Any
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.toTypedArray
import kotlin.text.split
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.`internal`.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AbiStabilityMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.AssertionsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.CompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JdkRelease
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.LambdasMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.SamConversionsMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.StringConcatMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.WhenExpressionsMode
import org.jetbrains.kotlin.buildtools.`internal`.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

@Serializable
internal class JvmCompilerArgumentsImpl() : CommonCompilerArgumentsImpl(), JvmCompilerArguments,
    JvmCompilerArguments.Builder, DeepCopyable<JvmCompilerArgumentsImpl> {
  @Transient
  private val defaultArguments: K2JVMCompilerArguments = createDefaultArguments()

  @Transient
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_ABI_STABILITY")
  internal var abiStability: AbiStabilityMode? =
      defaultArguments.abiStability?.let { AbiStabilityMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::abiStability, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") }

  @SerialName("X_ADD_MODULES")
  internal var additionalJavaModules: List<String> =
      defaultArguments.additionalJavaModules.toListOrEmpty()

  @SerialName("X_ALLOW_NO_SOURCE_FILES")
  internal var allowNoSourceFiles: Boolean = defaultArguments.allowNoSourceFiles

  @SerialName("X_ALLOW_UNSTABLE_DEPENDENCIES")
  internal var allowUnstableDependencies: Boolean = defaultArguments.allowUnstableDependencies

  @SerialName("X_ANNOTATIONS_IN_METADATA")
  internal var annotationsInMetadata: Boolean = defaultArguments.annotationsInMetadata

  @SerialName("X_ASSERTIONS")
  internal var assertionsMode: AssertionsMode? =
      defaultArguments.assertionsMode?.let { AssertionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::assertionsMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") }

  @SerialName("X_BACKEND_THREADS")
  internal var backendThreads: Int = defaultArguments.backendThreads.let { it.toInt() }

  @SerialName("X_BUILD_FILE")
  internal var buildFile: String? = defaultArguments.buildFile

  @SerialName("X_DEBUG")
  internal var enableDebugMode: Boolean = defaultArguments.enableDebugMode

  @SerialName("X_DEFAULT_SCRIPT_EXTENSION")
  internal var defaultScriptExtension: String? = defaultArguments.defaultScriptExtension

  @SerialName("X_DIRECT_JAVA_ACTUALIZATION")
  internal var directJavaActualization: Boolean = defaultArguments.directJavaActualization

  @SerialName("X_DISABLE_STANDARD_SCRIPT")
  internal var disableStandardScript: Boolean = defaultArguments.disableStandardScript

  @SerialName("X_EMIT_JVM_TYPE_ANNOTATIONS")
  internal var emitJvmTypeAnnotations: Boolean = defaultArguments.emitJvmTypeAnnotations

  @SerialName("X_ENHANCED_COROUTINES_DEBUGGING")
  internal var enhancedCoroutinesDebugging: Boolean = defaultArguments.enhancedCoroutinesDebugging

  @SerialName("X_FRIEND_PATHS")
  internal var friendPaths: List<Path> =
      defaultArguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_GENERATE_STRICT_METADATA_VERSION")
  internal var strictMetadataVersionSemantics: Boolean =
      defaultArguments.strictMetadataVersionSemantics

  @SerialName("X_IGNORED_ANNOTATIONS_FOR_BRIDGES")
  internal var ignoredAnnotationsForBridges: List<String> =
      defaultArguments.ignoredAnnotationsForBridges.toListOrEmpty()

  @SerialName("X_INDY_ALLOW_ANNOTATED_LAMBDAS")
  internal var indyAllowAnnotatedLambdas: Boolean? = defaultArguments.indyAllowAnnotatedLambdas

  @SerialName("X_JAVA_DIRECT")
  internal var javaDirect: Boolean = defaultArguments.javaDirect

  @SerialName("X_JAVA_PACKAGE_PREFIX")
  internal var javaPackagePrefix: String? = defaultArguments.javaPackagePrefix

  @SerialName("X_JAVA_SOURCE_ROOTS")
  internal var javaSourceRoots: List<Path> =
      defaultArguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_JDK_RELEASE")
  internal var jdkRelease: JdkRelease? =
      defaultArguments.jdkRelease?.let { JdkRelease.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jdkRelease, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") }

  @SerialName("X_JSPECIFY_ANNOTATIONS")
  internal var jspecifyAnnotations: JspecifyAnnotationsMode? =
      defaultArguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jspecifyAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") }

  @SerialName("X_JVM_DEFAULT")
  @Suppress("DEPRECATION")
  internal var jvmDefault: String? = defaultArguments.jvmDefault

  @SerialName("X_JVM_ENABLE_PREVIEW")
  internal var enableJvmPreview: Boolean = defaultArguments.enableJvmPreview

  @SerialName("X_JVM_EXPOSE_BOXED")
  internal var jvmExposeBoxed: Boolean = defaultArguments.jvmExposeBoxed

  @SerialName("X_LAMBDAS")
  internal var lambdas: LambdasMode? =
      defaultArguments.lambdas?.let { LambdasMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::lambdas, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") }

  @SerialName("X_MODULE_PATH")
  internal var javaModulePath: List<Path>? =
      defaultArguments.javaModulePath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("X_MULTIFILE_PARTS_INHERIT")
  internal var inheritMultifileParts: Boolean = defaultArguments.inheritMultifileParts

  @SerialName("X_NO_CALL_ASSERTIONS")
  internal var noCallAssertions: Boolean = defaultArguments.noCallAssertions

  @SerialName("X_NO_NEW_JAVA_ANNOTATION_TARGETS")
  internal var noNewJavaAnnotationTargets: Boolean = defaultArguments.noNewJavaAnnotationTargets

  @SerialName("X_NO_OPTIMIZE")
  internal var noOptimize: Boolean = defaultArguments.noOptimize

  @SerialName("X_NO_PARAM_ASSERTIONS")
  internal var noParamAssertions: Boolean = defaultArguments.noParamAssertions

  @SerialName("X_NO_RECEIVER_ASSERTIONS")
  internal var noReceiverAssertions: Boolean = defaultArguments.noReceiverAssertions

  @SerialName("X_NO_RESET_JAR_TIMESTAMPS")
  internal var noResetJarTimestamps: Boolean = defaultArguments.noResetJarTimestamps

  @SerialName("X_NO_SOURCE_DEBUG_EXTENSION")
  internal var noSourceDebugExtension: Boolean = defaultArguments.noSourceDebugExtension

  @SerialName("X_NO_UNIFIED_NULL_CHECKS")
  internal var noUnifiedNullChecks: Boolean = defaultArguments.noUnifiedNullChecks

  @SerialName("X_OUTPUT_BUILTINS_METADATA")
  internal var outputBuiltinsMetadata: Boolean = defaultArguments.outputBuiltinsMetadata

  @SerialName("X_SAM_CONVERSIONS")
  internal var samConversions: SamConversionsMode? =
      defaultArguments.samConversions?.let { SamConversionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::samConversions, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") }

  @SerialName("X_SANITIZE_PARENTHESES")
  internal var sanitizeParentheses: Boolean = defaultArguments.sanitizeParentheses

  @SerialName("X_SCRIPT_RESOLVER_ENVIRONMENT")
  internal var scriptResolverEnvironment: List<String> =
      defaultArguments.scriptResolverEnvironment.toListOrEmpty()

  @SerialName("X_STRING_CONCAT")
  internal var stringConcat: StringConcatMode? =
      defaultArguments.stringConcat?.let { StringConcatMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::stringConcat, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") }

  @SerialName("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")
  internal var supportCompatqualCheckerFrameworkAnnotations: CompatqualAnnotationsMode? =
      defaultArguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::supportCompatqualCheckerFrameworkAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") }

  @SerialName("X_SUPPRESS_MISSING_BUILTINS_ERROR")
  internal var suppressMissingBuiltinsError: Boolean = defaultArguments.suppressMissingBuiltinsError

  @SerialName("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")
  internal var useOldInlineClassesManglingScheme: Boolean =
      defaultArguments.useOldInlineClassesManglingScheme

  @SerialName("X_USE_FAST_JAR_FILE_SYSTEM")
  internal var useFastJarFileSystem: Boolean? = defaultArguments.useFastJarFileSystem

  @SerialName("X_USE_INLINE_SCOPES_NUMBERS")
  internal var useInlineScopesNumbers: Boolean = defaultArguments.useInlineScopesNumbers

  @SerialName("X_USE_METADATA_ON_INCREMENTAL_CLASSPATH")
  internal var useMetadataOnIncrementalClasspath: Boolean =
      defaultArguments.useMetadataOnIncrementalClasspath

  @SerialName("X_USE_OLD_CLASS_FILES_READING")
  internal var useOldClassFilesReading: Boolean = defaultArguments.useOldClassFilesReading

  @SerialName("X_USE_TYPE_TABLE")
  internal var useTypeTable: Boolean = defaultArguments.useTypeTable

  @SerialName("X_VALHALLA_VALUE_CLASSES")
  internal var valhallaValueClasses: Boolean = defaultArguments.valhallaValueClasses

  @SerialName("X_VALIDATE_BYTECODE")
  internal var validateBytecode: Boolean = defaultArguments.validateBytecode

  @SerialName("X_WHEN_EXPRESSIONS")
  internal var whenExpressionsGeneration: WhenExpressionsMode? =
      defaultArguments.whenExpressionsGeneration?.let { WhenExpressionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::whenExpressionsGeneration, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") }

  @SerialName("CLASSPATH")
  internal var classpath: List<Path>? =
      defaultArguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("D")
  internal var destination: String? = defaultArguments.destination

  @SerialName("EXPRESSION")
  internal var expression: String? = defaultArguments.expression

  @SerialName("INCLUDE_RUNTIME")
  internal var includeRuntime: Boolean = defaultArguments.includeRuntime

  @SerialName("JAVA_PARAMETERS")
  internal var javaParameters: Boolean = defaultArguments.javaParameters

  @SerialName("JDK_HOME")
  internal var jdkHome: Path? = defaultArguments.jdkHome?.let { kotlin.io.path.Path(it) }

  @SerialName("JVM_DEFAULT")
  internal var jvmDefaultStable: JvmDefaultMode? =
      defaultArguments.jvmDefaultStable?.let { JvmDefaultMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jvmDefaultStable, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") }

  @SerialName("JVM_TARGET")
  internal var jvmTarget: JvmTarget? =
      defaultArguments.jvmTarget?.let { JvmTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jvmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") }

  @SerialName("MODULE_NAME")
  internal var moduleName: String? = defaultArguments.moduleName

  @SerialName("NO_JDK")
  internal var noJdk: Boolean = defaultArguments.noJdk

  @SerialName("NO_REFLECT")
  internal var noReflect: Boolean = defaultArguments.noReflect

  @SerialName("NO_STDLIB")
  internal var noStdlib: Boolean = defaultArguments.noStdlib

  @SerialName("SCRIPT_TEMPLATES")
  internal var scriptTemplates: List<String> = defaultArguments.scriptTemplates.toListOrEmpty()

  @SerialName("X_PROFILE")
  internal var profile: ProfileCompilerCommandImpl? =
      @Suppress("UNCHECKED_CAST") applyProfileCompilerCommand(JvmCompilerArgumentValueAdapter.toImpl(null) as ProfileCompilerCommandImpl?, defaultArguments)

  @SerialName("X_NULLABILITY_ANNOTATIONS")
  internal var nullabilityAnnotations: List<NullabilityAnnotationImpl> =
      @Suppress("UNCHECKED_CAST") applyNullabilityAnnotations(JvmCompilerArgumentValueAdapter.toImpl(emptyList<NullabilityAnnotationImpl>()) as List<NullabilityAnnotationImpl>, defaultArguments)

  @SerialName("X_JSR305")
  internal var jsr305: List<Jsr305Impl> =
      @Suppress("UNCHECKED_CAST") applyJsr305(JvmCompilerArgumentValueAdapter.toImpl(emptyList<Jsr305Impl>()) as List<Jsr305Impl>, defaultArguments)

  public constructor(
    argumentValidationErrors: Set<String> = emptySet(),
    restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
    argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
  ) : this() {
    _argumentValidationErrors += argumentValidationErrors
    _restrictedArgViolations += restrictedArgViolations
    this.argumentParseDiagnostics += argumentParseDiagnostics
  }

  override fun createDefaultArguments(): K2JVMCompilerArguments = K2JVMCompilerArguments()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = JvmCompilerArgumentValueAdapter.toApi(when (key.id) {
    "X_ABI_STABILITY" -> abiStability
    "X_ADD_MODULES" -> additionalJavaModules
    "X_ALLOW_NO_SOURCE_FILES" -> allowNoSourceFiles
    "X_ALLOW_UNSTABLE_DEPENDENCIES" -> allowUnstableDependencies
    "X_ANNOTATIONS_IN_METADATA" -> annotationsInMetadata
    "X_ASSERTIONS" -> assertionsMode
    "X_BACKEND_THREADS" -> backendThreads
    "X_BUILD_FILE" -> buildFile
    "X_DEBUG" -> enableDebugMode
    "X_DEFAULT_SCRIPT_EXTENSION" -> defaultScriptExtension
    "X_DIRECT_JAVA_ACTUALIZATION" -> directJavaActualization
    "X_DISABLE_STANDARD_SCRIPT" -> disableStandardScript
    "X_EMIT_JVM_TYPE_ANNOTATIONS" -> emitJvmTypeAnnotations
    "X_ENHANCED_COROUTINES_DEBUGGING" -> enhancedCoroutinesDebugging
    "X_FRIEND_PATHS" -> friendPaths
    "X_GENERATE_STRICT_METADATA_VERSION" -> strictMetadataVersionSemantics
    "X_IGNORED_ANNOTATIONS_FOR_BRIDGES" -> ignoredAnnotationsForBridges
    "X_INDY_ALLOW_ANNOTATED_LAMBDAS" -> indyAllowAnnotatedLambdas
    "X_JAVA_DIRECT" -> javaDirect
    "X_JAVA_PACKAGE_PREFIX" -> javaPackagePrefix
    "X_JAVA_SOURCE_ROOTS" -> javaSourceRoots
    "X_JDK_RELEASE" -> jdkRelease
    "X_JSPECIFY_ANNOTATIONS" -> jspecifyAnnotations
    "X_JVM_DEFAULT" -> jvmDefault
    "X_JVM_ENABLE_PREVIEW" -> enableJvmPreview
    "X_JVM_EXPOSE_BOXED" -> jvmExposeBoxed
    "X_LAMBDAS" -> lambdas
    "X_MODULE_PATH" -> javaModulePath
    "X_MULTIFILE_PARTS_INHERIT" -> inheritMultifileParts
    "X_NO_CALL_ASSERTIONS" -> noCallAssertions
    "X_NO_NEW_JAVA_ANNOTATION_TARGETS" -> noNewJavaAnnotationTargets
    "X_NO_OPTIMIZE" -> noOptimize
    "X_NO_PARAM_ASSERTIONS" -> noParamAssertions
    "X_NO_RECEIVER_ASSERTIONS" -> noReceiverAssertions
    "X_NO_RESET_JAR_TIMESTAMPS" -> noResetJarTimestamps
    "X_NO_SOURCE_DEBUG_EXTENSION" -> noSourceDebugExtension
    "X_NO_UNIFIED_NULL_CHECKS" -> noUnifiedNullChecks
    "X_OUTPUT_BUILTINS_METADATA" -> outputBuiltinsMetadata
    "X_SAM_CONVERSIONS" -> samConversions
    "X_SANITIZE_PARENTHESES" -> sanitizeParentheses
    "X_SCRIPT_RESOLVER_ENVIRONMENT" -> scriptResolverEnvironment
    "X_STRING_CONCAT" -> stringConcat
    "X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS" -> supportCompatqualCheckerFrameworkAnnotations
    "X_SUPPRESS_MISSING_BUILTINS_ERROR" -> suppressMissingBuiltinsError
    "X_USE_14_INLINE_CLASSES_MANGLING_SCHEME" -> useOldInlineClassesManglingScheme
    "X_USE_FAST_JAR_FILE_SYSTEM" -> useFastJarFileSystem
    "X_USE_INLINE_SCOPES_NUMBERS" -> useInlineScopesNumbers
    "X_USE_METADATA_ON_INCREMENTAL_CLASSPATH" -> useMetadataOnIncrementalClasspath
    "X_USE_OLD_CLASS_FILES_READING" -> useOldClassFilesReading
    "X_USE_TYPE_TABLE" -> useTypeTable
    "X_VALHALLA_VALUE_CLASSES" -> valhallaValueClasses
    "X_VALIDATE_BYTECODE" -> validateBytecode
    "X_WHEN_EXPRESSIONS" -> whenExpressionsGeneration
    "CLASSPATH" -> classpath
    "D" -> destination
    "EXPRESSION" -> expression
    "INCLUDE_RUNTIME" -> includeRuntime
    "JAVA_PARAMETERS" -> javaParameters
    "JDK_HOME" -> jdkHome
    "JVM_DEFAULT" -> jvmDefaultStable
    "JVM_TARGET" -> jvmTarget
    "MODULE_NAME" -> moduleName
    "NO_JDK" -> noJdk
    "NO_REFLECT" -> noReflect
    "NO_STDLIB" -> noStdlib
    "SCRIPT_TEMPLATES" -> scriptTemplates
    "X_PROFILE" -> profile
    "X_NULLABILITY_ANNOTATIONS" -> nullabilityAnnotations
    "X_JSR305" -> jsr305
    else -> {
            check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
            optionsMap[key.id]
        }
  }
  )

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    when (key.id) {
      "X_ABI_STABILITY" -> abiStability = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_ADD_MODULES" -> additionalJavaModules = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_NO_SOURCE_FILES" -> allowNoSourceFiles = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_ALLOW_UNSTABLE_DEPENDENCIES" -> allowUnstableDependencies = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_ANNOTATIONS_IN_METADATA" -> annotationsInMetadata = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_ASSERTIONS" -> assertionsMode = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_BACKEND_THREADS" -> backendThreads = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_BUILD_FILE" -> buildFile = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_DEBUG" -> enableDebugMode = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_DEFAULT_SCRIPT_EXTENSION" -> defaultScriptExtension = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_DIRECT_JAVA_ACTUALIZATION" -> directJavaActualization = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_DISABLE_STANDARD_SCRIPT" -> disableStandardScript = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_EMIT_JVM_TYPE_ANNOTATIONS" -> emitJvmTypeAnnotations = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_ENHANCED_COROUTINES_DEBUGGING" -> enhancedCoroutinesDebugging = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_FRIEND_PATHS" -> friendPaths = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_GENERATE_STRICT_METADATA_VERSION" -> strictMetadataVersionSemantics = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_IGNORED_ANNOTATIONS_FOR_BRIDGES" -> ignoredAnnotationsForBridges = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_INDY_ALLOW_ANNOTATED_LAMBDAS" -> indyAllowAnnotatedLambdas = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JAVA_DIRECT" -> javaDirect = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JAVA_PACKAGE_PREFIX" -> javaPackagePrefix = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JAVA_SOURCE_ROOTS" -> javaSourceRoots = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JDK_RELEASE" -> jdkRelease = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JSPECIFY_ANNOTATIONS" -> jspecifyAnnotations = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JVM_DEFAULT" -> jvmDefault = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JVM_ENABLE_PREVIEW" -> enableJvmPreview = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JVM_EXPOSE_BOXED" -> jvmExposeBoxed = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_LAMBDAS" -> lambdas = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_MODULE_PATH" -> javaModulePath = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_MULTIFILE_PARTS_INHERIT" -> inheritMultifileParts = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_CALL_ASSERTIONS" -> noCallAssertions = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_NEW_JAVA_ANNOTATION_TARGETS" -> noNewJavaAnnotationTargets = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_OPTIMIZE" -> noOptimize = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_PARAM_ASSERTIONS" -> noParamAssertions = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_RECEIVER_ASSERTIONS" -> noReceiverAssertions = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_RESET_JAR_TIMESTAMPS" -> noResetJarTimestamps = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_SOURCE_DEBUG_EXTENSION" -> noSourceDebugExtension = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NO_UNIFIED_NULL_CHECKS" -> noUnifiedNullChecks = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_OUTPUT_BUILTINS_METADATA" -> outputBuiltinsMetadata = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_SAM_CONVERSIONS" -> samConversions = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_SANITIZE_PARENTHESES" -> sanitizeParentheses = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_SCRIPT_RESOLVER_ENVIRONMENT" -> scriptResolverEnvironment = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_STRING_CONCAT" -> stringConcat = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS" -> supportCompatqualCheckerFrameworkAnnotations = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_SUPPRESS_MISSING_BUILTINS_ERROR" -> suppressMissingBuiltinsError = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_14_INLINE_CLASSES_MANGLING_SCHEME" -> useOldInlineClassesManglingScheme = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_FAST_JAR_FILE_SYSTEM" -> useFastJarFileSystem = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_INLINE_SCOPES_NUMBERS" -> useInlineScopesNumbers = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_METADATA_ON_INCREMENTAL_CLASSPATH" -> useMetadataOnIncrementalClasspath = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_OLD_CLASS_FILES_READING" -> useOldClassFilesReading = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_USE_TYPE_TABLE" -> useTypeTable = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_VALHALLA_VALUE_CLASSES" -> valhallaValueClasses = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_VALIDATE_BYTECODE" -> validateBytecode = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_WHEN_EXPRESSIONS" -> whenExpressionsGeneration = JvmCompilerArgumentValueAdapter.toImpl(value)
      "CLASSPATH" -> classpath = JvmCompilerArgumentValueAdapter.toImpl(value)
      "D" -> destination = JvmCompilerArgumentValueAdapter.toImpl(value)
      "EXPRESSION" -> expression = JvmCompilerArgumentValueAdapter.toImpl(value)
      "INCLUDE_RUNTIME" -> includeRuntime = JvmCompilerArgumentValueAdapter.toImpl(value)
      "JAVA_PARAMETERS" -> javaParameters = JvmCompilerArgumentValueAdapter.toImpl(value)
      "JDK_HOME" -> jdkHome = JvmCompilerArgumentValueAdapter.toImpl(value)
      "JVM_DEFAULT" -> jvmDefaultStable = JvmCompilerArgumentValueAdapter.toImpl(value)
      "JVM_TARGET" -> jvmTarget = JvmCompilerArgumentValueAdapter.toImpl(value)
      "MODULE_NAME" -> moduleName = JvmCompilerArgumentValueAdapter.toImpl(value)
      "NO_JDK" -> noJdk = JvmCompilerArgumentValueAdapter.toImpl(value)
      "NO_REFLECT" -> noReflect = JvmCompilerArgumentValueAdapter.toImpl(value)
      "NO_STDLIB" -> noStdlib = JvmCompilerArgumentValueAdapter.toImpl(value)
      "SCRIPT_TEMPLATES" -> scriptTemplates = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_PROFILE" -> profile = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_NULLABILITY_ANNOTATIONS" -> nullabilityAnnotations = JvmCompilerArgumentValueAdapter.toImpl(value)
      "X_JSR305" -> jsr305 = JvmCompilerArgumentValueAdapter.toImpl(value)
      else -> {
              optionsMap[key.id] = JvmCompilerArgumentValueAdapter.toImpl(value)
          }
    }
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: JvmCompilerArguments.JvmCompilerArgument<*>): Boolean = key.id in knownOptionIds

  override fun deepCopy(): JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

  override fun build(): JvmCompilerArgumentsImpl = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(): K2JVMCompilerArguments {
    val arguments = K2JVMCompilerArguments()
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter {
      when (it) {
        "X_COMPILE_BUILTINS_AS_PART_OF_STDLIB" -> throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.20""")
        "X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL" -> throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_IR_DO_NOT_CLEAR_BINDING_CONTEXT" -> throw IllegalStateException("""Compiler parameter not recognized: X_IR_DO_NOT_CLEAR_BINDING_CONTEXT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_IR_INLINER" -> throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""")
        "X_KLIB" -> throw IllegalStateException("""Compiler parameter not recognized: X_KLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_LINK_VIA_SIGNATURES" -> throw IllegalStateException("""Compiler parameter not recognized: X_LINK_VIA_SIGNATURES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_SERIALIZE_IR" -> throw IllegalStateException("""Compiler parameter not recognized: X_SERIALIZE_IR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""")
        "X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING" -> throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE" -> throw IllegalStateException("""Compiler parameter not recognized: X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""")
        "X_USE_K2_KAPT" -> throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""")
        "X_VALUE_CLASSES" -> throw IllegalStateException("""Compiler parameter not recognized: X_VALUE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""")
        else -> true
      }
    }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.abiStability = abiStability?.stringValue
    arguments.additionalJavaModules = additionalJavaModules.toTypedArray()
    arguments.allowNoSourceFiles = allowNoSourceFiles
    arguments.allowUnstableDependencies = allowUnstableDependencies
    arguments.annotationsInMetadata = annotationsInMetadata
    arguments.assertionsMode = assertionsMode?.stringValue
    arguments.backendThreads = backendThreads.toString()
    arguments.buildFile = buildFile
    arguments.enableDebugMode = enableDebugMode
    arguments.defaultScriptExtension = defaultScriptExtension
    arguments.directJavaActualization = directJavaActualization
    arguments.disableStandardScript = disableStandardScript
    arguments.emitJvmTypeAnnotations = emitJvmTypeAnnotations
    arguments.enhancedCoroutinesDebugging = enhancedCoroutinesDebugging
    arguments.friendPaths = friendPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = strictMetadataVersionSemantics
    arguments.ignoredAnnotationsForBridges = ignoredAnnotationsForBridges.toTypedArray()
    arguments.indyAllowAnnotatedLambdas = indyAllowAnnotatedLambdas
    arguments.javaDirect = javaDirect
    arguments.javaPackagePrefix = javaPackagePrefix
    arguments.javaSourceRoots = javaSourceRoots.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.jdkRelease = jdkRelease?.stringValue
    arguments.jspecifyAnnotations = jspecifyAnnotations?.stringValue
    arguments.jvmDefault = jvmDefault
    arguments.enableJvmPreview = enableJvmPreview
    arguments.jvmExposeBoxed = jvmExposeBoxed
    arguments.lambdas = lambdas?.stringValue
    arguments.javaModulePath = javaModulePath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.inheritMultifileParts = inheritMultifileParts
    arguments.noCallAssertions = noCallAssertions
    arguments.noNewJavaAnnotationTargets = noNewJavaAnnotationTargets
    arguments.noOptimize = noOptimize
    arguments.noParamAssertions = noParamAssertions
    arguments.noReceiverAssertions = noReceiverAssertions
    arguments.noResetJarTimestamps = noResetJarTimestamps
    arguments.noSourceDebugExtension = noSourceDebugExtension
    arguments.noUnifiedNullChecks = noUnifiedNullChecks
    arguments.outputBuiltinsMetadata = outputBuiltinsMetadata
    arguments.samConversions = samConversions?.stringValue
    arguments.sanitizeParentheses = sanitizeParentheses
    arguments.scriptResolverEnvironment = scriptResolverEnvironment.toTypedArray()
    arguments.stringConcat = stringConcat?.stringValue
    arguments.supportCompatqualCheckerFrameworkAnnotations = supportCompatqualCheckerFrameworkAnnotations?.stringValue
    arguments.suppressMissingBuiltinsError = suppressMissingBuiltinsError
    arguments.useOldInlineClassesManglingScheme = useOldInlineClassesManglingScheme
    arguments.useFastJarFileSystem = useFastJarFileSystem
    arguments.useInlineScopesNumbers = useInlineScopesNumbers
    arguments.useMetadataOnIncrementalClasspath = useMetadataOnIncrementalClasspath
    arguments.useOldClassFilesReading = useOldClassFilesReading
    arguments.useTypeTable = useTypeTable
    arguments.valhallaValueClasses = valhallaValueClasses
    arguments.validateBytecode = validateBytecode
    arguments.whenExpressionsGeneration = whenExpressionsGeneration?.stringValue
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = destination
    arguments.expression = expression
    arguments.includeRuntime = includeRuntime
    arguments.javaParameters = javaParameters
    arguments.jdkHome = jdkHome?.absolutePathStringOrThrow()
    arguments.jvmDefaultStable = jvmDefaultStable?.stringValue
    arguments.jvmTarget = jvmTarget?.stringValue
    arguments.moduleName = moduleName
    arguments.noJdk = noJdk
    arguments.noReflect = noReflect
    arguments.noStdlib = noStdlib
    arguments.scriptTemplates = scriptTemplates.toTypedArray()
    arguments.applyProfileCompilerCommand(profile)
    arguments.applyNullabilityAnnotations(nullabilityAnnotations)
    arguments.applyJsr305(jsr305)
    arguments.internalArguments = parseCommandLineArguments<K2JVMCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2JVMCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { abiStability = arguments.abiStability?.let { AbiStabilityMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::abiStability, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    additionalJavaModules = arguments.additionalJavaModules.toListOrEmpty()
    allowNoSourceFiles = arguments.allowNoSourceFiles
    allowUnstableDependencies = arguments.allowUnstableDependencies
    annotationsInMetadata = arguments.annotationsInMetadata
    try { assertionsMode = arguments.assertionsMode?.let { AssertionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::assertionsMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    backendThreads = arguments.backendThreads.let { it.toInt() }
    buildFile = arguments.buildFile
    enableDebugMode = arguments.enableDebugMode
    defaultScriptExtension = arguments.defaultScriptExtension
    directJavaActualization = arguments.directJavaActualization
    disableStandardScript = arguments.disableStandardScript
    emitJvmTypeAnnotations = arguments.emitJvmTypeAnnotations
    enhancedCoroutinesDebugging = arguments.enhancedCoroutinesDebugging
    friendPaths = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }
    strictMetadataVersionSemantics = arguments.strictMetadataVersionSemantics
    ignoredAnnotationsForBridges = arguments.ignoredAnnotationsForBridges.toListOrEmpty()
    indyAllowAnnotatedLambdas = arguments.indyAllowAnnotatedLambdas
    javaDirect = arguments.javaDirect
    javaPackagePrefix = arguments.javaPackagePrefix
    javaSourceRoots = arguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) }
    try { jdkRelease = arguments.jdkRelease?.let { JdkRelease.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jdkRelease, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { jspecifyAnnotations = arguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jspecifyAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    jvmDefault = arguments.jvmDefault
    enableJvmPreview = arguments.enableJvmPreview
    jvmExposeBoxed = arguments.jvmExposeBoxed
    try { lambdas = arguments.lambdas?.let { LambdasMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::lambdas, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    javaModulePath = arguments.javaModulePath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    inheritMultifileParts = arguments.inheritMultifileParts
    noCallAssertions = arguments.noCallAssertions
    noNewJavaAnnotationTargets = arguments.noNewJavaAnnotationTargets
    noOptimize = arguments.noOptimize
    noParamAssertions = arguments.noParamAssertions
    noReceiverAssertions = arguments.noReceiverAssertions
    noResetJarTimestamps = arguments.noResetJarTimestamps
    noSourceDebugExtension = arguments.noSourceDebugExtension
    noUnifiedNullChecks = arguments.noUnifiedNullChecks
    outputBuiltinsMetadata = arguments.outputBuiltinsMetadata
    try { samConversions = arguments.samConversions?.let { SamConversionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::samConversions, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    sanitizeParentheses = arguments.sanitizeParentheses
    scriptResolverEnvironment = arguments.scriptResolverEnvironment.toListOrEmpty()
    try { stringConcat = arguments.stringConcat?.let { StringConcatMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::stringConcat, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { supportCompatqualCheckerFrameworkAnnotations = arguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::supportCompatqualCheckerFrameworkAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    suppressMissingBuiltinsError = arguments.suppressMissingBuiltinsError
    useOldInlineClassesManglingScheme = arguments.useOldInlineClassesManglingScheme
    useFastJarFileSystem = arguments.useFastJarFileSystem
    useInlineScopesNumbers = arguments.useInlineScopesNumbers
    useMetadataOnIncrementalClasspath = arguments.useMetadataOnIncrementalClasspath
    useOldClassFilesReading = arguments.useOldClassFilesReading
    useTypeTable = arguments.useTypeTable
    valhallaValueClasses = arguments.valhallaValueClasses
    validateBytecode = arguments.validateBytecode
    try { whenExpressionsGeneration = arguments.whenExpressionsGeneration?.let { WhenExpressionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::whenExpressionsGeneration, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    classpath = arguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }
    destination = arguments.destination
    expression = arguments.expression
    includeRuntime = arguments.includeRuntime
    javaParameters = arguments.javaParameters
    jdkHome = arguments.jdkHome?.let { kotlin.io.path.Path(it) }
    try { jvmDefaultStable = arguments.jvmDefaultStable?.let { JvmDefaultMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jvmDefaultStable, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { jvmTarget = arguments.jvmTarget?.let { JvmTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jvmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    moduleName = arguments.moduleName
    noJdk = arguments.noJdk
    noReflect = arguments.noReflect
    noStdlib = arguments.noStdlib
    scriptTemplates = arguments.scriptTemplates.toListOrEmpty()
    try { profile = applyProfileCompilerCommand(profile, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { nullabilityAnnotations = applyNullabilityAnnotations(nullabilityAnnotations, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    try { jsr305 = applyJsr305(jsr305, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2JVMCompilerArguments = K2JVMCompilerArguments()): K2JVMCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.abiStability = abiStability?.stringValue
    arguments.additionalJavaModules = additionalJavaModules.toTypedArray()
    arguments.allowNoSourceFiles = allowNoSourceFiles
    arguments.allowUnstableDependencies = allowUnstableDependencies
    arguments.annotationsInMetadata = annotationsInMetadata
    arguments.assertionsMode = assertionsMode?.stringValue
    arguments.buildFile = buildFile
    arguments.enableDebugMode = enableDebugMode
    arguments.defaultScriptExtension = defaultScriptExtension
    arguments.directJavaActualization = directJavaActualization
    arguments.disableStandardScript = disableStandardScript
    arguments.emitJvmTypeAnnotations = emitJvmTypeAnnotations
    arguments.enhancedCoroutinesDebugging = enhancedCoroutinesDebugging
    arguments.friendPaths = friendPaths.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = strictMetadataVersionSemantics
    arguments.ignoredAnnotationsForBridges = ignoredAnnotationsForBridges.toTypedArray()
    arguments.indyAllowAnnotatedLambdas = indyAllowAnnotatedLambdas
    arguments.javaDirect = javaDirect
    arguments.javaPackagePrefix = javaPackagePrefix
    arguments.javaSourceRoots = javaSourceRoots.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.jdkRelease = jdkRelease?.stringValue
    arguments.jspecifyAnnotations = jspecifyAnnotations?.stringValue
    arguments.jvmDefault = jvmDefault
    arguments.enableJvmPreview = enableJvmPreview
    arguments.jvmExposeBoxed = jvmExposeBoxed
    arguments.lambdas = lambdas?.stringValue
    arguments.javaModulePath = javaModulePath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.inheritMultifileParts = inheritMultifileParts
    arguments.noCallAssertions = noCallAssertions
    arguments.noNewJavaAnnotationTargets = noNewJavaAnnotationTargets
    arguments.noOptimize = noOptimize
    arguments.noParamAssertions = noParamAssertions
    arguments.noReceiverAssertions = noReceiverAssertions
    arguments.noResetJarTimestamps = noResetJarTimestamps
    arguments.noSourceDebugExtension = noSourceDebugExtension
    arguments.noUnifiedNullChecks = noUnifiedNullChecks
    arguments.outputBuiltinsMetadata = outputBuiltinsMetadata
    arguments.samConversions = samConversions?.stringValue
    arguments.sanitizeParentheses = sanitizeParentheses
    arguments.scriptResolverEnvironment = scriptResolverEnvironment.toTypedArray()
    arguments.stringConcat = stringConcat?.stringValue
    arguments.supportCompatqualCheckerFrameworkAnnotations = supportCompatqualCheckerFrameworkAnnotations?.stringValue
    arguments.suppressMissingBuiltinsError = suppressMissingBuiltinsError
    arguments.useOldInlineClassesManglingScheme = useOldInlineClassesManglingScheme
    arguments.useFastJarFileSystem = useFastJarFileSystem
    arguments.useInlineScopesNumbers = useInlineScopesNumbers
    arguments.useMetadataOnIncrementalClasspath = useMetadataOnIncrementalClasspath
    arguments.useOldClassFilesReading = useOldClassFilesReading
    arguments.useTypeTable = useTypeTable
    arguments.valhallaValueClasses = valhallaValueClasses
    arguments.validateBytecode = validateBytecode
    arguments.whenExpressionsGeneration = whenExpressionsGeneration?.stringValue
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = destination
    arguments.expression = expression
    arguments.includeRuntime = includeRuntime
    arguments.javaParameters = javaParameters
    arguments.jdkHome = jdkHome?.absolutePathStringOrThrow()
    arguments.jvmDefaultStable = jvmDefaultStable?.stringValue
    arguments.jvmTarget = jvmTarget?.stringValue
    arguments.moduleName = moduleName
    arguments.noJdk = noJdk
    arguments.noReflect = noReflect
    arguments.noStdlib = noStdlib
    arguments.scriptTemplates = scriptTemplates.toTypedArray()
    arguments.applyNullabilityAnnotations(nullabilityAnnotations)
    arguments.applyJsr305(jsr305)
    return arguments
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JVMCompilerArguments = parseCommandLineArguments(arguments)
    collectRestrictedArgViolations(compilerArgs, K2JVMCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    val compilerArgs = toCompilerArguments()
    parseCommandLineArguments(arguments, compilerArgs, false)
    handleCustomPluginArguments(this, compilerArgs)
    collectRestrictedArgViolations(compilerArgs, K2JVMCompilerArguments())
    validateArgumentsAllErrors(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }
    argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }
    applyCompilerArguments(compilerArgs)
  }

  override fun toArgumentStrings(): List<String> {
    val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)
    return arguments
  }

  @Suppress("DEPRECATION")
  internal override fun collectRestrictedArgViolations(compilerArgs: CommonToolArguments, defaultArgs: CommonToolArguments) {
    super.collectRestrictedArgViolations(compilerArgs, defaultArgs)
    val args = compilerArgs as K2JVMCompilerArguments
    val castedDefaults = defaultArgs as K2JVMCompilerArguments
    if (args.destination != castedDefaults.destination) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-d' is not supported in the Build Tools API. The destination is configured via the destinationDirectory parameter of jvmCompilationOperationBuilder."))
    if (args.expression != castedDefaults.expression) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-expression'/'-e' is not supported in the Build Tools API."))
    if (args.includeRuntime != castedDefaults.includeRuntime) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-include-runtime' is not supported in the Build Tools API."))
    if (args.buildFile != castedDefaults.buildFile) _restrictedArgViolations.add(RestrictedArgViolation.Error("Argument '-Xbuild-file'/'-module' is not supported in the Build Tools API."))
  }

  /**
   * Returns a sorted list of compiler argument strings representing only the arguments
   * that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
   * Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
   * only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
   * skips properties whose value matches the default.
   */
  public fun toCompilationInputs(): List<String> = toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()

  public companion object {
    private val knownOptionIds: List<String> =
        listOf("X_ABI_STABILITY","X_ADD_MODULES","X_ALLOW_NO_SOURCE_FILES","X_ALLOW_UNSTABLE_DEPENDENCIES","X_ANNOTATIONS_IN_METADATA","X_ASSERTIONS","X_BACKEND_THREADS","X_BUILD_FILE","X_DEBUG","X_DEFAULT_SCRIPT_EXTENSION","X_DIRECT_JAVA_ACTUALIZATION","X_DISABLE_STANDARD_SCRIPT","X_EMIT_JVM_TYPE_ANNOTATIONS","X_ENHANCED_COROUTINES_DEBUGGING","X_FRIEND_PATHS","X_GENERATE_STRICT_METADATA_VERSION","X_IGNORED_ANNOTATIONS_FOR_BRIDGES","X_INDY_ALLOW_ANNOTATED_LAMBDAS","X_JAVA_DIRECT","X_JAVA_PACKAGE_PREFIX","X_JAVA_SOURCE_ROOTS","X_JDK_RELEASE","X_JSPECIFY_ANNOTATIONS","X_JVM_DEFAULT","X_JVM_ENABLE_PREVIEW","X_JVM_EXPOSE_BOXED","X_LAMBDAS","X_MODULE_PATH","X_MULTIFILE_PARTS_INHERIT","X_NO_CALL_ASSERTIONS","X_NO_NEW_JAVA_ANNOTATION_TARGETS","X_NO_OPTIMIZE","X_NO_PARAM_ASSERTIONS","X_NO_RECEIVER_ASSERTIONS","X_NO_RESET_JAR_TIMESTAMPS","X_NO_SOURCE_DEBUG_EXTENSION","X_NO_UNIFIED_NULL_CHECKS","X_OUTPUT_BUILTINS_METADATA","X_SAM_CONVERSIONS","X_SANITIZE_PARENTHESES","X_SCRIPT_RESOLVER_ENVIRONMENT","X_STRING_CONCAT","X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS","X_SUPPRESS_MISSING_BUILTINS_ERROR","X_USE_14_INLINE_CLASSES_MANGLING_SCHEME","X_USE_FAST_JAR_FILE_SYSTEM","X_USE_INLINE_SCOPES_NUMBERS","X_USE_METADATA_ON_INCREMENTAL_CLASSPATH","X_USE_OLD_CLASS_FILES_READING","X_USE_TYPE_TABLE","X_VALHALLA_VALUE_CLASSES","X_VALIDATE_BYTECODE","X_WHEN_EXPRESSIONS","CLASSPATH","D","EXPRESSION","INCLUDE_RUNTIME","JAVA_PARAMETERS","JDK_HOME","JVM_DEFAULT","JVM_TARGET","MODULE_NAME","NO_JDK","NO_REFLECT","NO_STDLIB","SCRIPT_TEMPLATES","X_PROFILE","X_NULLABILITY_ANNOTATIONS","X_JSR305",)
  }
}
