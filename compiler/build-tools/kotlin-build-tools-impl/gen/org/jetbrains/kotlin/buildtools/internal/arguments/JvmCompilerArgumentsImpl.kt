// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

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
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.ValhallaSupportMode
import org.jetbrains.kotlin.buildtools.`internal`.arguments.enums.WhenExpressionsMode
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NullabilityAnnotation
import org.jetbrains.kotlin.buildtools.api.arguments.ProfileCompilerCommand
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArgumentsAllErrors
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JvmCompilerArgumentsImpl(
  defaultArguments: K2JVMCompilerArguments = K2JVMCompilerArguments(),
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonCompilerArgumentsImpl(defaultArguments, argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    JvmCompilerArguments,
    JvmCompilerArguments.Builder,
    DeepCopyable<JvmCompilerArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_ABI_STABILITY")
  protected var `Xabi-stability`: AbiStabilityMode? =
      defaultArguments.abiStability?.let { AbiStabilityMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::abiStability, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") }

  @SerialName("X_ADD_MODULES")
  protected var `Xadd-modules`: List<String> =
      defaultArguments.additionalJavaModules.toListOrEmpty()

  @SerialName("X_ALLOW_NO_SOURCE_FILES")
  protected var `Xallow-no-source-files`: Boolean = defaultArguments.allowNoSourceFiles

  @SerialName("X_ALLOW_UNSTABLE_DEPENDENCIES")
  protected var `Xallow-unstable-dependencies`: Boolean = defaultArguments.allowUnstableDependencies

  @SerialName("X_ANNOTATIONS_IN_METADATA")
  protected var `Xannotations-in-metadata`: Boolean = defaultArguments.annotationsInMetadata

  @SerialName("X_ASSERTIONS")
  protected var Xassertions: AssertionsMode? =
      defaultArguments.assertionsMode?.let { AssertionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::assertionsMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") }

  @SerialName("X_BACKEND_THREADS")
  protected var `Xbackend-threads`: Int = defaultArguments.backendThreads.let { it.toInt() }

  @SerialName("X_BUILD_FILE")
  protected var `Xbuild-file`: String? = defaultArguments.buildFile

  @SerialName("X_DEBUG")
  protected var Xdebug: Boolean = defaultArguments.enableDebugMode

  @SerialName("X_DEFAULT_SCRIPT_EXTENSION")
  protected var `Xdefault-script-extension`: String? = defaultArguments.defaultScriptExtension

  @SerialName("X_DIRECT_JAVA_ACTUALIZATION")
  protected var `Xdirect-java-actualization`: Boolean = defaultArguments.directJavaActualization

  @SerialName("X_DISABLE_STANDARD_SCRIPT")
  protected var `Xdisable-standard-script`: Boolean = defaultArguments.disableStandardScript

  @SerialName("X_EMIT_JVM_TYPE_ANNOTATIONS")
  protected var `Xemit-jvm-type-annotations`: Boolean = defaultArguments.emitJvmTypeAnnotations

  @SerialName("X_ENHANCED_COROUTINES_DEBUGGING")
  protected var `Xenhanced-coroutines-debugging`: Boolean =
      defaultArguments.enhancedCoroutinesDebugging

  @SerialName("X_FRIEND_PATHS")
  protected var `Xfriend-paths`: List<Path> =
      defaultArguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_GENERATE_STRICT_METADATA_VERSION")
  protected var `Xgenerate-strict-metadata-version`: Boolean =
      defaultArguments.strictMetadataVersionSemantics

  @SerialName("X_IGNORED_ANNOTATIONS_FOR_BRIDGES")
  protected var `Xignored-annotations-for-bridges`: List<String> =
      defaultArguments.ignoredAnnotationsForBridges.toListOrEmpty()

  @SerialName("X_INDY_ALLOW_ANNOTATED_LAMBDAS")
  protected var `Xindy-allow-annotated-lambdas`: Boolean? =
      defaultArguments.indyAllowAnnotatedLambdas

  @SerialName("X_JAVA_DIRECT")
  protected var `Xjava-direct`: Boolean = defaultArguments.javaDirect

  @SerialName("X_JAVA_PACKAGE_PREFIX")
  protected var `Xjava-package-prefix`: String? = defaultArguments.javaPackagePrefix

  @SerialName("X_JAVA_SOURCE_ROOTS")
  protected var `Xjava-source-roots`: List<Path> =
      defaultArguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_JDK_RELEASE")
  protected var `Xjdk-release`: JdkRelease? =
      defaultArguments.jdkRelease?.let { JdkRelease.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jdkRelease, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") }

  @SerialName("X_JSPECIFY_ANNOTATIONS")
  protected var `Xjspecify-annotations`: JspecifyAnnotationsMode? =
      defaultArguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jspecifyAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") }

  @SerialName("X_JVM_DEFAULT")
  protected var `Xjvm-default`: String? = defaultArguments.jvmDefault

  @SerialName("X_JVM_ENABLE_PREVIEW")
  protected var `Xjvm-enable-preview`: Boolean = defaultArguments.enableJvmPreview

  @SerialName("X_JVM_EXPOSE_BOXED")
  protected var `Xjvm-expose-boxed`: Boolean = defaultArguments.jvmExposeBoxed

  @SerialName("X_LAMBDAS")
  protected var Xlambdas: LambdasMode? =
      defaultArguments.lambdas?.let { LambdasMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::lambdas, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") }

  @SerialName("X_MODULE_PATH")
  protected var `Xmodule-path`: List<Path>? =
      defaultArguments.javaModulePath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("X_MULTIFILE_PARTS_INHERIT")
  protected var `Xmultifile-parts-inherit`: Boolean = defaultArguments.inheritMultifileParts

  @SerialName("X_NO_CALL_ASSERTIONS")
  protected var `Xno-call-assertions`: Boolean = defaultArguments.noCallAssertions

  @SerialName("X_NO_NEW_JAVA_ANNOTATION_TARGETS")
  protected var `Xno-new-java-annotation-targets`: Boolean =
      defaultArguments.noNewJavaAnnotationTargets

  @SerialName("X_NO_OPTIMIZE")
  protected var `Xno-optimize`: Boolean = defaultArguments.noOptimize

  @SerialName("X_NO_PARAM_ASSERTIONS")
  protected var `Xno-param-assertions`: Boolean = defaultArguments.noParamAssertions

  @SerialName("X_NO_RECEIVER_ASSERTIONS")
  protected var `Xno-receiver-assertions`: Boolean = defaultArguments.noReceiverAssertions

  @SerialName("X_NO_RESET_JAR_TIMESTAMPS")
  protected var `Xno-reset-jar-timestamps`: Boolean = defaultArguments.noResetJarTimestamps

  @SerialName("X_NO_SOURCE_DEBUG_EXTENSION")
  protected var `Xno-source-debug-extension`: Boolean = defaultArguments.noSourceDebugExtension

  @SerialName("X_NO_UNIFIED_NULL_CHECKS")
  protected var `Xno-unified-null-checks`: Boolean = defaultArguments.noUnifiedNullChecks

  @SerialName("X_OUTPUT_BUILTINS_METADATA")
  protected var `Xoutput-builtins-metadata`: Boolean = defaultArguments.outputBuiltinsMetadata

  @SerialName("X_SAM_CONVERSIONS")
  protected var `Xsam-conversions`: SamConversionsMode? =
      defaultArguments.samConversions?.let { SamConversionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::samConversions, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") }

  @SerialName("X_SANITIZE_PARENTHESES")
  protected var `Xsanitize-parentheses`: Boolean = defaultArguments.sanitizeParentheses

  @SerialName("X_SCRIPT_RESOLVER_ENVIRONMENT")
  protected var `Xscript-resolver-environment`: List<String> =
      defaultArguments.scriptResolverEnvironment.toListOrEmpty()

  @SerialName("X_STRING_CONCAT")
  protected var `Xstring-concat`: StringConcatMode? =
      defaultArguments.stringConcat?.let { StringConcatMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::stringConcat, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") }

  @SerialName("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")
  protected var `Xsupport-compatqual-checker-framework-annotations`: CompatqualAnnotationsMode? =
      defaultArguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::supportCompatqualCheckerFrameworkAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") }

  @SerialName("X_SUPPRESS_MISSING_BUILTINS_ERROR")
  protected var `Xsuppress-missing-builtins-error`: Boolean =
      defaultArguments.suppressMissingBuiltinsError

  @SerialName("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")
  protected var `Xuse-14-inline-classes-mangling-scheme`: Boolean =
      defaultArguments.useOldInlineClassesManglingScheme

  @SerialName("X_USE_FAST_JAR_FILE_SYSTEM")
  protected var `Xuse-fast-jar-file-system`: Boolean? = defaultArguments.useFastJarFileSystem

  @SerialName("X_USE_INLINE_SCOPES_NUMBERS")
  protected var `Xuse-inline-scopes-numbers`: Boolean = defaultArguments.useInlineScopesNumbers

  @SerialName("X_USE_METADATA_ON_INCREMENTAL_CLASSPATH")
  protected var `Xuse-metadata-on-incremental-classpath`: Boolean =
      defaultArguments.useMetadataOnIncrementalClasspath

  @SerialName("X_USE_OLD_CLASS_FILES_READING")
  protected var `Xuse-old-class-files-reading`: Boolean = defaultArguments.useOldClassFilesReading

  @SerialName("X_USE_TYPE_TABLE")
  protected var `Xuse-type-table`: Boolean = defaultArguments.useTypeTable

  @SerialName("X_VALHALLA_SUPPORT")
  protected var `Xvalhalla-support`: ValhallaSupportMode? =
      defaultArguments.valhallaSupport?.let { ValhallaSupportMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::valhallaSupport, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xvalhalla-support value: $it") }

  @SerialName("X_VALIDATE_BYTECODE")
  protected var `Xvalidate-bytecode`: Boolean = defaultArguments.validateBytecode

  @SerialName("X_WHEN_EXPRESSIONS")
  protected var `Xwhen-expressions`: WhenExpressionsMode? =
      defaultArguments.whenExpressionsGeneration?.let { WhenExpressionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::whenExpressionsGeneration, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") }

  @SerialName("CLASSPATH")
  protected var classpath: List<Path>? =
      defaultArguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("D")
  protected var d: String? = defaultArguments.destination

  @SerialName("EXPRESSION")
  protected var expression: String? = defaultArguments.expression

  @SerialName("INCLUDE_RUNTIME")
  protected var `include-runtime`: Boolean = defaultArguments.includeRuntime

  @SerialName("JAVA_PARAMETERS")
  protected var `java-parameters`: Boolean = defaultArguments.javaParameters

  @SerialName("JDK_HOME")
  protected var `jdk-home`: Path? = defaultArguments.jdkHome?.let { kotlin.io.path.Path(it) }

  @SerialName("JVM_DEFAULT")
  protected var `jvm-default`: JvmDefaultMode? =
      defaultArguments.jvmDefaultStable?.let { JvmDefaultMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jvmDefaultStable, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") }

  @SerialName("JVM_TARGET")
  protected var `jvm-target`: JvmTarget? =
      defaultArguments.jvmTarget?.let { JvmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, defaultArguments::jvmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") }

  @SerialName("MODULE_NAME")
  protected var `module-name`: String? = defaultArguments.moduleName

  @SerialName("NO_JDK")
  protected var `no-jdk`: Boolean = defaultArguments.noJdk

  @SerialName("NO_REFLECT")
  protected var `no-reflect`: Boolean = defaultArguments.noReflect

  @SerialName("NO_STDLIB")
  protected var `no-stdlib`: Boolean = defaultArguments.noStdlib

  @SerialName("SCRIPT_TEMPLATES")
  protected var `script-templates`: List<String> = defaultArguments.scriptTemplates.toListOrEmpty()

  @SerialName("X_PROFILE")
  protected var Xprofile: ProfileCompilerCommand? =
      applyProfileCompilerCommand(null, defaultArguments)

  @SerialName("X_NULLABILITY_ANNOTATIONS")
  protected var `Xnullability-annotations`: List<NullabilityAnnotation> =
      applyNullabilityAnnotations(emptyList<NullabilityAnnotation>(), defaultArguments)

  @SerialName("X_JSR305")
  protected var Xjsr305: List<Jsr305> = applyJsr305(emptyList<Jsr305>(), defaultArguments)
  init {
    applyCompilerArguments(K2JVMCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  private operator fun `get`(key: String): Any? = JvmCompilerArgumentValueAdapter.toApi(optionsMap[key])

  private operator fun `set`(key: String, `value`: Any?) {
    optionsMap[key] = JvmCompilerArgumentValueAdapter.toImpl(`value`)
  }

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    if (key.availableSinceVersion > KotlinReleaseVersion(2, 5, 0)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
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
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    arguments.abiStability = `Xabi-stability`?.stringValue
    arguments.additionalJavaModules = `Xadd-modules`.toTypedArray()
    arguments.allowNoSourceFiles = `Xallow-no-source-files`
    arguments.allowUnstableDependencies = `Xallow-unstable-dependencies`
    arguments.annotationsInMetadata = `Xannotations-in-metadata`
    arguments.assertionsMode = Xassertions?.stringValue
    arguments.backendThreads = `Xbackend-threads`.toString()
    arguments.buildFile = `Xbuild-file`
    arguments.enableDebugMode = Xdebug
    arguments.defaultScriptExtension = `Xdefault-script-extension`
    arguments.directJavaActualization = `Xdirect-java-actualization`
    arguments.disableStandardScript = `Xdisable-standard-script`
    arguments.emitJvmTypeAnnotations = `Xemit-jvm-type-annotations`
    arguments.enhancedCoroutinesDebugging = `Xenhanced-coroutines-debugging`
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = `Xgenerate-strict-metadata-version`
    arguments.ignoredAnnotationsForBridges = `Xignored-annotations-for-bridges`.toTypedArray()
    arguments.indyAllowAnnotatedLambdas = `Xindy-allow-annotated-lambdas`
    arguments.javaDirect = `Xjava-direct`
    arguments.javaPackagePrefix = `Xjava-package-prefix`
    arguments.javaSourceRoots = `Xjava-source-roots`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.jdkRelease = `Xjdk-release`?.stringValue
    arguments.jspecifyAnnotations = `Xjspecify-annotations`?.stringValue
    arguments.jvmDefault = `Xjvm-default`
    arguments.enableJvmPreview = `Xjvm-enable-preview`
    arguments.jvmExposeBoxed = `Xjvm-expose-boxed`
    arguments.lambdas = Xlambdas?.stringValue
    arguments.javaModulePath = `Xmodule-path`?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.inheritMultifileParts = `Xmultifile-parts-inherit`
    arguments.noCallAssertions = `Xno-call-assertions`
    arguments.noNewJavaAnnotationTargets = `Xno-new-java-annotation-targets`
    arguments.noOptimize = `Xno-optimize`
    arguments.noParamAssertions = `Xno-param-assertions`
    arguments.noReceiverAssertions = `Xno-receiver-assertions`
    arguments.noResetJarTimestamps = `Xno-reset-jar-timestamps`
    arguments.noSourceDebugExtension = `Xno-source-debug-extension`
    arguments.noUnifiedNullChecks = `Xno-unified-null-checks`
    arguments.outputBuiltinsMetadata = `Xoutput-builtins-metadata`
    arguments.samConversions = `Xsam-conversions`?.stringValue
    arguments.sanitizeParentheses = `Xsanitize-parentheses`
    arguments.scriptResolverEnvironment = `Xscript-resolver-environment`.toTypedArray()
    arguments.stringConcat = `Xstring-concat`?.stringValue
    arguments.supportCompatqualCheckerFrameworkAnnotations = `Xsupport-compatqual-checker-framework-annotations`?.stringValue
    arguments.suppressMissingBuiltinsError = `Xsuppress-missing-builtins-error`
    arguments.useOldInlineClassesManglingScheme = `Xuse-14-inline-classes-mangling-scheme`
    arguments.useFastJarFileSystem = `Xuse-fast-jar-file-system`
    arguments.useInlineScopesNumbers = `Xuse-inline-scopes-numbers`
    arguments.useMetadataOnIncrementalClasspath = `Xuse-metadata-on-incremental-classpath`
    arguments.useOldClassFilesReading = `Xuse-old-class-files-reading`
    arguments.useTypeTable = `Xuse-type-table`
    arguments.valhallaSupport = `Xvalhalla-support`?.stringValue
    arguments.validateBytecode = `Xvalidate-bytecode`
    arguments.whenExpressionsGeneration = `Xwhen-expressions`?.stringValue
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = d
    arguments.expression = expression
    arguments.includeRuntime = `include-runtime`
    arguments.javaParameters = `java-parameters`
    arguments.jdkHome = `jdk-home`?.absolutePathStringOrThrow()
    arguments.jvmDefaultStable = `jvm-default`?.stringValue
    arguments.jvmTarget = `jvm-target`?.stringValue
    arguments.moduleName = `module-name`
    arguments.noJdk = `no-jdk`
    arguments.noReflect = `no-reflect`
    arguments.noStdlib = `no-stdlib`
    arguments.scriptTemplates = `script-templates`.toTypedArray()
    arguments.applyProfileCompilerCommand(Xprofile)
    arguments.applyNullabilityAnnotations(`Xnullability-annotations`)
    arguments.applyJsr305(Xjsr305)
    arguments.internalArguments = parseCommandLineArguments<K2JVMCompilerArguments>(internalArguments.toList()).internalArguments
    populateExplicitArguments(arguments)
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2JVMCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { `Xabi-stability` = arguments.abiStability?.let { AbiStabilityMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::abiStability, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xadd-modules` = arguments.additionalJavaModules.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xallow-no-source-files` = arguments.allowNoSourceFiles } catch (_: NoSuchMethodError) {  }
    try { `Xallow-unstable-dependencies` = arguments.allowUnstableDependencies } catch (_: NoSuchMethodError) {  }
    try { `Xannotations-in-metadata` = arguments.annotationsInMetadata } catch (_: NoSuchMethodError) {  }
    try { Xassertions = arguments.assertionsMode?.let { AssertionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::assertionsMode, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xbackend-threads` = arguments.backendThreads.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { `Xbuild-file` = arguments.buildFile } catch (_: NoSuchMethodError) {  }
    try { Xdebug = arguments.enableDebugMode } catch (_: NoSuchMethodError) {  }
    try { `Xdefault-script-extension` = arguments.defaultScriptExtension } catch (_: NoSuchMethodError) {  }
    try { `Xdirect-java-actualization` = arguments.directJavaActualization } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-standard-script` = arguments.disableStandardScript } catch (_: NoSuchMethodError) {  }
    try { `Xemit-jvm-type-annotations` = arguments.emitJvmTypeAnnotations } catch (_: NoSuchMethodError) {  }
    try { `Xenhanced-coroutines-debugging` = arguments.enhancedCoroutinesDebugging } catch (_: NoSuchMethodError) {  }
    try { `Xfriend-paths` = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xgenerate-strict-metadata-version` = arguments.strictMetadataVersionSemantics } catch (_: NoSuchMethodError) {  }
    try { `Xignored-annotations-for-bridges` = arguments.ignoredAnnotationsForBridges.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xindy-allow-annotated-lambdas` = arguments.indyAllowAnnotatedLambdas } catch (_: NoSuchMethodError) {  }
    try { `Xjava-direct` = arguments.javaDirect } catch (_: NoSuchMethodError) {  }
    try { `Xjava-package-prefix` = arguments.javaPackagePrefix } catch (_: NoSuchMethodError) {  }
    try { `Xjava-source-roots` = arguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xjdk-release` = arguments.jdkRelease?.let { JdkRelease.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jdkRelease, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xjspecify-annotations` = arguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jspecifyAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-default` = arguments.jvmDefault } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-enable-preview` = arguments.enableJvmPreview } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-expose-boxed` = arguments.jvmExposeBoxed } catch (_: NoSuchMethodError) {  }
    try { Xlambdas = arguments.lambdas?.let { LambdasMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::lambdas, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xmodule-path` = arguments.javaModulePath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xmultifile-parts-inherit` = arguments.inheritMultifileParts } catch (_: NoSuchMethodError) {  }
    try { `Xno-call-assertions` = arguments.noCallAssertions } catch (_: NoSuchMethodError) {  }
    try { `Xno-new-java-annotation-targets` = arguments.noNewJavaAnnotationTargets } catch (_: NoSuchMethodError) {  }
    try { `Xno-optimize` = arguments.noOptimize } catch (_: NoSuchMethodError) {  }
    try { `Xno-param-assertions` = arguments.noParamAssertions } catch (_: NoSuchMethodError) {  }
    try { `Xno-receiver-assertions` = arguments.noReceiverAssertions } catch (_: NoSuchMethodError) {  }
    try { `Xno-reset-jar-timestamps` = arguments.noResetJarTimestamps } catch (_: NoSuchMethodError) {  }
    try { `Xno-source-debug-extension` = arguments.noSourceDebugExtension } catch (_: NoSuchMethodError) {  }
    try { `Xno-unified-null-checks` = arguments.noUnifiedNullChecks } catch (_: NoSuchMethodError) {  }
    try { `Xoutput-builtins-metadata` = arguments.outputBuiltinsMetadata } catch (_: NoSuchMethodError) {  }
    try { `Xsam-conversions` = arguments.samConversions?.let { SamConversionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::samConversions, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xsanitize-parentheses` = arguments.sanitizeParentheses } catch (_: NoSuchMethodError) {  }
    try { `Xscript-resolver-environment` = arguments.scriptResolverEnvironment.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xstring-concat` = arguments.stringConcat?.let { StringConcatMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::stringConcat, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xsupport-compatqual-checker-framework-annotations` = arguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::supportCompatqualCheckerFrameworkAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-missing-builtins-error` = arguments.suppressMissingBuiltinsError } catch (_: NoSuchMethodError) {  }
    try { `Xuse-14-inline-classes-mangling-scheme` = arguments.useOldInlineClassesManglingScheme } catch (_: NoSuchMethodError) {  }
    try { `Xuse-fast-jar-file-system` = arguments.useFastJarFileSystem } catch (_: NoSuchMethodError) {  }
    try { `Xuse-inline-scopes-numbers` = arguments.useInlineScopesNumbers } catch (_: NoSuchMethodError) {  }
    try { `Xuse-metadata-on-incremental-classpath` = arguments.useMetadataOnIncrementalClasspath } catch (_: NoSuchMethodError) {  }
    try { `Xuse-old-class-files-reading` = arguments.useOldClassFilesReading } catch (_: NoSuchMethodError) {  }
    try { `Xuse-type-table` = arguments.useTypeTable } catch (_: NoSuchMethodError) {  }
    try { `Xvalhalla-support` = arguments.valhallaSupport?.let { ValhallaSupportMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::valhallaSupport, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xvalhalla-support value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xvalidate-bytecode` = arguments.validateBytecode } catch (_: NoSuchMethodError) {  }
    try { `Xwhen-expressions` = arguments.whenExpressionsGeneration?.let { WhenExpressionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::whenExpressionsGeneration, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { classpath = arguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { d = arguments.destination } catch (_: NoSuchMethodError) {  }
    try { expression = arguments.expression } catch (_: NoSuchMethodError) {  }
    try { `include-runtime` = arguments.includeRuntime } catch (_: NoSuchMethodError) {  }
    try { `java-parameters` = arguments.javaParameters } catch (_: NoSuchMethodError) {  }
    try { `jdk-home` = arguments.jdkHome?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `jvm-default` = arguments.jvmDefaultStable?.let { JvmDefaultMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jvmDefaultStable, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `jvm-target` = arguments.jvmTarget?.let { JvmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jvmTarget, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `module-name` = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { `no-jdk` = arguments.noJdk } catch (_: NoSuchMethodError) {  }
    try { `no-reflect` = arguments.noReflect } catch (_: NoSuchMethodError) {  }
    try { `no-stdlib` = arguments.noStdlib } catch (_: NoSuchMethodError) {  }
    try { `script-templates` = arguments.scriptTemplates.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { Xprofile = applyProfileCompilerCommand(Xprofile, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xnullability-annotations` = applyNullabilityAnnotations(`Xnullability-annotations`, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { Xjsr305 = applyJsr305(Xjsr305, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArgumentsAffectingOutcome(arguments: K2JVMCompilerArguments = K2JVMCompilerArguments()): K2JVMCompilerArguments {
    super.toCompilerArgumentsAffectingOutcome(arguments)
    arguments.abiStability = `Xabi-stability`?.stringValue
    arguments.additionalJavaModules = `Xadd-modules`.toTypedArray()
    arguments.allowNoSourceFiles = `Xallow-no-source-files`
    arguments.allowUnstableDependencies = `Xallow-unstable-dependencies`
    arguments.annotationsInMetadata = `Xannotations-in-metadata`
    arguments.assertionsMode = Xassertions?.stringValue
    arguments.buildFile = `Xbuild-file`
    arguments.enableDebugMode = Xdebug
    arguments.defaultScriptExtension = `Xdefault-script-extension`
    arguments.directJavaActualization = `Xdirect-java-actualization`
    arguments.disableStandardScript = `Xdisable-standard-script`
    arguments.emitJvmTypeAnnotations = `Xemit-jvm-type-annotations`
    arguments.enhancedCoroutinesDebugging = `Xenhanced-coroutines-debugging`
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = `Xgenerate-strict-metadata-version`
    arguments.ignoredAnnotationsForBridges = `Xignored-annotations-for-bridges`.toTypedArray()
    arguments.indyAllowAnnotatedLambdas = `Xindy-allow-annotated-lambdas`
    arguments.javaDirect = `Xjava-direct`
    arguments.javaPackagePrefix = `Xjava-package-prefix`
    arguments.javaSourceRoots = `Xjava-source-roots`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.jdkRelease = `Xjdk-release`?.stringValue
    arguments.jspecifyAnnotations = `Xjspecify-annotations`?.stringValue
    arguments.jvmDefault = `Xjvm-default`
    arguments.enableJvmPreview = `Xjvm-enable-preview`
    arguments.jvmExposeBoxed = `Xjvm-expose-boxed`
    arguments.lambdas = Xlambdas?.stringValue
    arguments.javaModulePath = `Xmodule-path`?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.inheritMultifileParts = `Xmultifile-parts-inherit`
    arguments.noCallAssertions = `Xno-call-assertions`
    arguments.noNewJavaAnnotationTargets = `Xno-new-java-annotation-targets`
    arguments.noOptimize = `Xno-optimize`
    arguments.noParamAssertions = `Xno-param-assertions`
    arguments.noReceiverAssertions = `Xno-receiver-assertions`
    arguments.noResetJarTimestamps = `Xno-reset-jar-timestamps`
    arguments.noSourceDebugExtension = `Xno-source-debug-extension`
    arguments.noUnifiedNullChecks = `Xno-unified-null-checks`
    arguments.outputBuiltinsMetadata = `Xoutput-builtins-metadata`
    arguments.samConversions = `Xsam-conversions`?.stringValue
    arguments.sanitizeParentheses = `Xsanitize-parentheses`
    arguments.scriptResolverEnvironment = `Xscript-resolver-environment`.toTypedArray()
    arguments.stringConcat = `Xstring-concat`?.stringValue
    arguments.supportCompatqualCheckerFrameworkAnnotations = `Xsupport-compatqual-checker-framework-annotations`?.stringValue
    arguments.suppressMissingBuiltinsError = `Xsuppress-missing-builtins-error`
    arguments.useOldInlineClassesManglingScheme = `Xuse-14-inline-classes-mangling-scheme`
    arguments.useFastJarFileSystem = `Xuse-fast-jar-file-system`
    arguments.useInlineScopesNumbers = `Xuse-inline-scopes-numbers`
    arguments.useMetadataOnIncrementalClasspath = `Xuse-metadata-on-incremental-classpath`
    arguments.useOldClassFilesReading = `Xuse-old-class-files-reading`
    arguments.useTypeTable = `Xuse-type-table`
    arguments.valhallaSupport = `Xvalhalla-support`?.stringValue
    arguments.validateBytecode = `Xvalidate-bytecode`
    arguments.whenExpressionsGeneration = `Xwhen-expressions`?.stringValue
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = d
    arguments.expression = expression
    arguments.includeRuntime = `include-runtime`
    arguments.javaParameters = `java-parameters`
    arguments.jdkHome = `jdk-home`?.absolutePathStringOrThrow()
    arguments.jvmDefaultStable = `jvm-default`?.stringValue
    arguments.jvmTarget = `jvm-target`?.stringValue
    arguments.moduleName = `module-name`
    arguments.noJdk = `no-jdk`
    arguments.noReflect = `no-reflect`
    arguments.noStdlib = `no-stdlib`
    arguments.scriptTemplates = `script-templates`.toTypedArray()
    arguments.applyNullabilityAnnotations(`Xnullability-annotations`)
    arguments.applyJsr305(Xjsr305)
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

    public val X_VALHALLA_SUPPORT: JvmCompilerArgument<ValhallaSupportMode?> =
        JvmCompilerArgument("X_VALHALLA_SUPPORT")

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

    public val X_PROFILE: JvmCompilerArgument<ProfileCompilerCommand?> =
        JvmCompilerArgument("X_PROFILE")

    public val X_NULLABILITY_ANNOTATIONS: JvmCompilerArgument<List<NullabilityAnnotation>> =
        JvmCompilerArgument("X_NULLABILITY_ANNOTATIONS")

    public val X_JSR305: JvmCompilerArgument<List<Jsr305>> = JvmCompilerArgument("X_JSR305")
  }
}
