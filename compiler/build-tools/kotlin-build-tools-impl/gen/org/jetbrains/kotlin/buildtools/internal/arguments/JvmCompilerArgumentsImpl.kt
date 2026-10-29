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
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
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
import org.jetbrains.kotlin.buildtools.`internal`.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.`internal`.serializability.setPropertyWithSerialNameValue
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
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V = JvmCompilerArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)

  public operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    JvmCompilerArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
  }

  public operator fun contains(key: JvmCompilerArgument<*>): Boolean = true

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = try { JvmCompilerArgumentValueAdapter.toApi(JvmCompilerArgumentsImpl::class.getPropertyWithSerialNameValue(this, key.id)) } catch (_: IllegalStateException) { optionsMap[key.id] } as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 20)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    try { JvmCompilerArgumentsImpl::class.setPropertyWithSerialNameValue(this, key.id, JvmCompilerArgumentValueAdapter.toImpl(value)) } catch (_: IllegalStateException) { optionsMap[key.id] = value }
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: JvmCompilerArguments.JvmCompilerArgument<*>): Boolean = key.id in optionsMap

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
    try { abiStability = arguments.abiStability?.let { AbiStabilityMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::abiStability, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { additionalJavaModules = arguments.additionalJavaModules.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { allowNoSourceFiles = arguments.allowNoSourceFiles } catch (_: NoSuchMethodError) {  }
    try { allowUnstableDependencies = arguments.allowUnstableDependencies } catch (_: NoSuchMethodError) {  }
    try { annotationsInMetadata = arguments.annotationsInMetadata } catch (_: NoSuchMethodError) {  }
    try { assertionsMode = arguments.assertionsMode?.let { AssertionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::assertionsMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { backendThreads = arguments.backendThreads.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { buildFile = arguments.buildFile } catch (_: NoSuchMethodError) {  }
    try { enableDebugMode = arguments.enableDebugMode } catch (_: NoSuchMethodError) {  }
    try { defaultScriptExtension = arguments.defaultScriptExtension } catch (_: NoSuchMethodError) {  }
    try { directJavaActualization = arguments.directJavaActualization } catch (_: NoSuchMethodError) {  }
    try { disableStandardScript = arguments.disableStandardScript } catch (_: NoSuchMethodError) {  }
    try { emitJvmTypeAnnotations = arguments.emitJvmTypeAnnotations } catch (_: NoSuchMethodError) {  }
    try { enhancedCoroutinesDebugging = arguments.enhancedCoroutinesDebugging } catch (_: NoSuchMethodError) {  }
    try { friendPaths = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { strictMetadataVersionSemantics = arguments.strictMetadataVersionSemantics } catch (_: NoSuchMethodError) {  }
    try { ignoredAnnotationsForBridges = arguments.ignoredAnnotationsForBridges.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { indyAllowAnnotatedLambdas = arguments.indyAllowAnnotatedLambdas } catch (_: NoSuchMethodError) {  }
    try { javaDirect = arguments.javaDirect } catch (_: NoSuchMethodError) {  }
    try { javaPackagePrefix = arguments.javaPackagePrefix } catch (_: NoSuchMethodError) {  }
    try { javaSourceRoots = arguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { jdkRelease = arguments.jdkRelease?.let { JdkRelease.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jdkRelease, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { jspecifyAnnotations = arguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jspecifyAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { jvmDefault = arguments.jvmDefault } catch (_: NoSuchMethodError) {  }
    try { enableJvmPreview = arguments.enableJvmPreview } catch (_: NoSuchMethodError) {  }
    try { jvmExposeBoxed = arguments.jvmExposeBoxed } catch (_: NoSuchMethodError) {  }
    try { lambdas = arguments.lambdas?.let { LambdasMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::lambdas, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { javaModulePath = arguments.javaModulePath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { inheritMultifileParts = arguments.inheritMultifileParts } catch (_: NoSuchMethodError) {  }
    try { noCallAssertions = arguments.noCallAssertions } catch (_: NoSuchMethodError) {  }
    try { noNewJavaAnnotationTargets = arguments.noNewJavaAnnotationTargets } catch (_: NoSuchMethodError) {  }
    try { noOptimize = arguments.noOptimize } catch (_: NoSuchMethodError) {  }
    try { noParamAssertions = arguments.noParamAssertions } catch (_: NoSuchMethodError) {  }
    try { noReceiverAssertions = arguments.noReceiverAssertions } catch (_: NoSuchMethodError) {  }
    try { noResetJarTimestamps = arguments.noResetJarTimestamps } catch (_: NoSuchMethodError) {  }
    try { noSourceDebugExtension = arguments.noSourceDebugExtension } catch (_: NoSuchMethodError) {  }
    try { noUnifiedNullChecks = arguments.noUnifiedNullChecks } catch (_: NoSuchMethodError) {  }
    try { outputBuiltinsMetadata = arguments.outputBuiltinsMetadata } catch (_: NoSuchMethodError) {  }
    try { samConversions = arguments.samConversions?.let { SamConversionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::samConversions, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { sanitizeParentheses = arguments.sanitizeParentheses } catch (_: NoSuchMethodError) {  }
    try { scriptResolverEnvironment = arguments.scriptResolverEnvironment.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { stringConcat = arguments.stringConcat?.let { StringConcatMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::stringConcat, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { supportCompatqualCheckerFrameworkAnnotations = arguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::supportCompatqualCheckerFrameworkAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { suppressMissingBuiltinsError = arguments.suppressMissingBuiltinsError } catch (_: NoSuchMethodError) {  }
    try { useOldInlineClassesManglingScheme = arguments.useOldInlineClassesManglingScheme } catch (_: NoSuchMethodError) {  }
    try { useFastJarFileSystem = arguments.useFastJarFileSystem } catch (_: NoSuchMethodError) {  }
    try { useInlineScopesNumbers = arguments.useInlineScopesNumbers } catch (_: NoSuchMethodError) {  }
    try { useMetadataOnIncrementalClasspath = arguments.useMetadataOnIncrementalClasspath } catch (_: NoSuchMethodError) {  }
    try { useOldClassFilesReading = arguments.useOldClassFilesReading } catch (_: NoSuchMethodError) {  }
    try { useTypeTable = arguments.useTypeTable } catch (_: NoSuchMethodError) {  }
    try { valhallaValueClasses = arguments.valhallaValueClasses } catch (_: NoSuchMethodError) {  }
    try { validateBytecode = arguments.validateBytecode } catch (_: NoSuchMethodError) {  }
    try { whenExpressionsGeneration = arguments.whenExpressionsGeneration?.let { WhenExpressionsMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::whenExpressionsGeneration, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { classpath = arguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { destination = arguments.destination } catch (_: NoSuchMethodError) {  }
    try { expression = arguments.expression } catch (_: NoSuchMethodError) {  }
    try { includeRuntime = arguments.includeRuntime } catch (_: NoSuchMethodError) {  }
    try { javaParameters = arguments.javaParameters } catch (_: NoSuchMethodError) {  }
    try { jdkHome = arguments.jdkHome?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { jvmDefaultStable = arguments.jvmDefaultStable?.let { JvmDefaultMode.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jvmDefaultStable, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { jvmTarget = arguments.jvmTarget?.let { JvmTarget.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jvmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { moduleName = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { noJdk = arguments.noJdk } catch (_: NoSuchMethodError) {  }
    try { noReflect = arguments.noReflect } catch (_: NoSuchMethodError) {  }
    try { noStdlib = arguments.noStdlib } catch (_: NoSuchMethodError) {  }
    try { scriptTemplates = arguments.scriptTemplates.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { profile = applyProfileCompilerCommand(profile, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { nullabilityAnnotations = applyNullabilityAnnotations(nullabilityAnnotations, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { jsr305 = applyJsr305(jsr305, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
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

    public val X_DEBUG: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_DEBUG")

    public val X_DEFAULT_SCRIPT_EXTENSION: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_DEFAULT_SCRIPT_EXTENSION")

    public val X_DIRECT_JAVA_ACTUALIZATION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_DIRECT_JAVA_ACTUALIZATION")

    public val X_DISABLE_STANDARD_SCRIPT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_DISABLE_STANDARD_SCRIPT")

    public val X_EMIT_JVM_TYPE_ANNOTATIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_EMIT_JVM_TYPE_ANNOTATIONS")

    public val X_ENHANCED_COROUTINES_DEBUGGING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCED_COROUTINES_DEBUGGING")

    public val X_FRIEND_PATHS: JvmCompilerArgument<List<Path>> =
        JvmCompilerArgument("X_FRIEND_PATHS")

    public val X_GENERATE_STRICT_METADATA_VERSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_GENERATE_STRICT_METADATA_VERSION")

    public val X_IGNORED_ANNOTATIONS_FOR_BRIDGES: JvmCompilerArgument<List<String>> =
        JvmCompilerArgument("X_IGNORED_ANNOTATIONS_FOR_BRIDGES")

    public val X_INDY_ALLOW_ANNOTATED_LAMBDAS: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_INDY_ALLOW_ANNOTATED_LAMBDAS")

    public val X_JAVA_DIRECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_JAVA_DIRECT")

    public val X_JAVA_PACKAGE_PREFIX: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JAVA_PACKAGE_PREFIX")

    public val X_JAVA_SOURCE_ROOTS: JvmCompilerArgument<List<Path>> =
        JvmCompilerArgument("X_JAVA_SOURCE_ROOTS")

    public val X_JDK_RELEASE: JvmCompilerArgument<JdkRelease?> =
        JvmCompilerArgument("X_JDK_RELEASE")

    public val X_JSPECIFY_ANNOTATIONS: JvmCompilerArgument<JspecifyAnnotationsMode?> =
        JvmCompilerArgument("X_JSPECIFY_ANNOTATIONS")

    public val X_JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JVM_DEFAULT")

    public val X_JVM_ENABLE_PREVIEW: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_ENABLE_PREVIEW")

    public val X_JVM_EXPOSE_BOXED: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_EXPOSE_BOXED")

    public val X_LAMBDAS: JvmCompilerArgument<LambdasMode?> = JvmCompilerArgument("X_LAMBDAS")

    public val X_MODULE_PATH: JvmCompilerArgument<List<Path>?> =
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

    public val X_STRING_CONCAT: JvmCompilerArgument<StringConcatMode?> =
        JvmCompilerArgument("X_STRING_CONCAT")

    public val X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS:
        JvmCompilerArgument<CompatqualAnnotationsMode?> =
        JvmCompilerArgument("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")

    public val X_SUPPRESS_MISSING_BUILTINS_ERROR: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_MISSING_BUILTINS_ERROR")

    public val X_USE_14_INLINE_CLASSES_MANGLING_SCHEME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")

    public val X_USE_FAST_JAR_FILE_SYSTEM: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_USE_FAST_JAR_FILE_SYSTEM")

    public val X_USE_INLINE_SCOPES_NUMBERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_INLINE_SCOPES_NUMBERS")

    public val X_USE_METADATA_ON_INCREMENTAL_CLASSPATH: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_METADATA_ON_INCREMENTAL_CLASSPATH")

    public val X_USE_OLD_CLASS_FILES_READING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_OLD_CLASS_FILES_READING")

    public val X_USE_TYPE_TABLE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_TYPE_TABLE")

    public val X_VALHALLA_VALUE_CLASSES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALHALLA_VALUE_CLASSES")

    public val X_VALIDATE_BYTECODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALIDATE_BYTECODE")

    public val X_WHEN_EXPRESSIONS: JvmCompilerArgument<WhenExpressionsMode?> =
        JvmCompilerArgument("X_WHEN_EXPRESSIONS")

    public val CLASSPATH: JvmCompilerArgument<List<Path>?> = JvmCompilerArgument("CLASSPATH")

    public val D: JvmCompilerArgument<String?> = JvmCompilerArgument("D")

    public val EXPRESSION: JvmCompilerArgument<String?> = JvmCompilerArgument("EXPRESSION")

    public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("INCLUDE_RUNTIME")

    public val JAVA_PARAMETERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("JAVA_PARAMETERS")

    public val JDK_HOME: JvmCompilerArgument<Path?> = JvmCompilerArgument("JDK_HOME")

    public val JVM_DEFAULT: JvmCompilerArgument<JvmDefaultMode?> =
        JvmCompilerArgument("JVM_DEFAULT")

    public val JVM_TARGET: JvmCompilerArgument<JvmTarget?> = JvmCompilerArgument("JVM_TARGET")

    public val MODULE_NAME: JvmCompilerArgument<String?> = JvmCompilerArgument("MODULE_NAME")

    public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

    public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

    public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")

    public val SCRIPT_TEMPLATES: JvmCompilerArgument<List<String>> =
        JvmCompilerArgument("SCRIPT_TEMPLATES")

    public val X_PROFILE: JvmCompilerArgument<ProfileCompilerCommandImpl?> =
        JvmCompilerArgument("X_PROFILE")

    public val X_NULLABILITY_ANNOTATIONS: JvmCompilerArgument<List<NullabilityAnnotationImpl>> =
        JvmCompilerArgument("X_NULLABILITY_ANNOTATIONS")

    public val X_JSR305: JvmCompilerArgument<List<Jsr305Impl>> = JvmCompilerArgument("X_JSR305")
  }
}
