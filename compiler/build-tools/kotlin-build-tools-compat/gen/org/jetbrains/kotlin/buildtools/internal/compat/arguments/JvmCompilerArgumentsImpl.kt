// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import java.io.File
import java.lang.IllegalStateException
import java.nio.`file`.Path
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
import kotlin.collections.emptyList
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.toTypedArray
import kotlin.text.split
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.buildtools.`internal`.compat.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.AbiStabilityMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.AssertionsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.CompatqualAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JdkRelease
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JspecifyAnnotationsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.LambdasMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.SamConversionsMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.StringConcatMode
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.enums.WhenExpressionsMode
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NullabilityAnnotation
import org.jetbrains.kotlin.buildtools.api.arguments.ProfileCompilerCommand
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JvmCompilerArgumentsImpl() : CommonCompilerArgumentsImpl(), JvmCompilerArguments,
    JvmCompilerArguments.Builder, DeepCopyable<JvmCompilerArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_ABI_STABILITY")
  protected var `Xabi-stability`: AbiStabilityMode? =
      defaultArguments.abiStability?.let { AbiStabilityMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") }

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
      defaultArguments.assertionsMode?.let { AssertionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") }

  @SerialName("X_BACKEND_THREADS")
  protected var `Xbackend-threads`: Int = defaultArguments.backendThreads.let { it.toInt() }

  @SerialName("X_BUILD_FILE")
  protected var `Xbuild-file`: String? = defaultArguments.buildFile

  @SerialName("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB")
  protected var `Xcompile-builtins-as-part-of-stdlib`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("expectBuiltinsAsPartOfStdlib")

  @SerialName("X_COMPILE_JAVA")
  protected var `Xcompile-java`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("compileJava")

  @SerialName("X_DEBUG")
  protected var Xdebug: Boolean = defaultArguments.enableDebugMode

  @SerialName("X_DEFAULT_SCRIPT_EXTENSION")
  protected var `Xdefault-script-extension`: String? = defaultArguments.defaultScriptExtension

  @SerialName("X_DISABLE_STANDARD_SCRIPT")
  protected var `Xdisable-standard-script`: Boolean = defaultArguments.disableStandardScript

  @SerialName("X_EMIT_JVM_TYPE_ANNOTATIONS")
  protected var `Xemit-jvm-type-annotations`: Boolean = defaultArguments.emitJvmTypeAnnotations

  @SerialName("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL")
  protected var `Xenhance-type-parameter-types-to-def-not-null`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("enhanceTypeParameterTypesToDefNotNull")

  @SerialName("X_ENHANCED_COROUTINES_DEBUGGING")
  protected var `Xenhanced-coroutines-debugging`: Boolean =
      defaultArguments.enhancedCoroutinesDebugging

  @SerialName("X_FRIEND_PATHS")
  protected var `Xfriend-paths`: List<Path> =
      defaultArguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_GENERATE_STRICT_METADATA_VERSION")
  protected var `Xgenerate-strict-metadata-version`: Boolean =
      defaultArguments.strictMetadataVersionSemantics

  @SerialName("X_INDY_ALLOW_ANNOTATED_LAMBDAS")
  protected var `Xindy-allow-annotated-lambdas`: Boolean? =
      defaultArguments.indyAllowAnnotatedLambdas

  @SerialName("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT")
  protected var `Xir-do-not-clear-binding-context`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("doNotClearBindingContext")

  @SerialName("X_IR_INLINER")
  protected var `Xir-inliner`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("enableIrInliner")

  @SerialName("X_JAVA_PACKAGE_PREFIX")
  protected var `Xjava-package-prefix`: String? = defaultArguments.javaPackagePrefix

  @SerialName("X_JAVA_SOURCE_ROOTS")
  protected var `Xjava-source-roots`: List<Path> =
      defaultArguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) }

  @SerialName("X_JAVAC_ARGUMENTS")
  protected var `Xjavac-arguments`: Array<String>? =
      defaultArguments.getUsingReflection<Array<String>>("javacArguments")

  @SerialName("X_JDK_RELEASE")
  protected var `Xjdk-release`: JdkRelease? =
      defaultArguments.jdkRelease?.let { JdkRelease.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") }

  @SerialName("X_JSPECIFY_ANNOTATIONS")
  protected var `Xjspecify-annotations`: JspecifyAnnotationsMode? =
      defaultArguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") }

  @SerialName("X_JVM_DEFAULT")
  protected var `Xjvm-default`: String? = defaultArguments.jvmDefault

  @SerialName("X_JVM_ENABLE_PREVIEW")
  protected var `Xjvm-enable-preview`: Boolean = defaultArguments.enableJvmPreview

  @SerialName("X_JVM_EXPOSE_BOXED")
  protected var `Xjvm-expose-boxed`: Boolean = defaultArguments.jvmExposeBoxed

  @SerialName("X_KLIB")
  protected var Xklib: List<Path>? =
      defaultArguments.getUsingReflection<String?>("klibLibraries")?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) }

  @SerialName("X_LAMBDAS")
  protected var Xlambdas: LambdasMode? =
      defaultArguments.lambdas?.let { LambdasMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") }

  @SerialName("X_LINK_VIA_SIGNATURES")
  protected var `Xlink-via-signatures`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("linkViaSignatures")

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
      defaultArguments.samConversions?.let { SamConversionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") }

  @SerialName("X_SANITIZE_PARENTHESES")
  protected var `Xsanitize-parentheses`: Boolean = defaultArguments.sanitizeParentheses

  @SerialName("X_SCRIPT_RESOLVER_ENVIRONMENT")
  protected var `Xscript-resolver-environment`: List<String> =
      defaultArguments.scriptResolverEnvironment.toListOrEmpty()

  @SerialName("X_SERIALIZE_IR")
  protected var `Xserialize-ir`: String = defaultArguments.getUsingReflection<String>("serializeIr")

  @SerialName("X_STRING_CONCAT")
  protected var `Xstring-concat`: StringConcatMode? =
      defaultArguments.stringConcat?.let { StringConcatMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") }

  @SerialName("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")
  protected var `Xsupport-compatqual-checker-framework-annotations`: CompatqualAnnotationsMode? =
      defaultArguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") }

  @SerialName("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING")
  protected var `Xsuppress-deprecated-jvm-target-warning`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("suppressDeprecatedJvmTargetWarning")

  @SerialName("X_SUPPRESS_MISSING_BUILTINS_ERROR")
  protected var `Xsuppress-missing-builtins-error`: Boolean =
      defaultArguments.suppressMissingBuiltinsError

  @SerialName("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE")
  protected var `Xtype-enhancement-improvements-strict-mode`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("typeEnhancementImprovementsInStrictMode")

  @SerialName("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")
  protected var `Xuse-14-inline-classes-mangling-scheme`: Boolean =
      defaultArguments.useOldInlineClassesManglingScheme

  @SerialName("X_USE_FAST_JAR_FILE_SYSTEM")
  protected var `Xuse-fast-jar-file-system`: Boolean? = defaultArguments.useFastJarFileSystem

  @SerialName("X_USE_INLINE_SCOPES_NUMBERS")
  protected var `Xuse-inline-scopes-numbers`: Boolean = defaultArguments.useInlineScopesNumbers

  @SerialName("X_USE_JAVAC")
  protected var `Xuse-javac`: Boolean = defaultArguments.getUsingReflection<Boolean>("useJavac")

  @SerialName("X_USE_K2_KAPT")
  protected var `Xuse-k2-kapt`: Boolean? =
      defaultArguments.getUsingReflection<Boolean?>("useK2Kapt")

  @SerialName("X_USE_OLD_CLASS_FILES_READING")
  protected var `Xuse-old-class-files-reading`: Boolean = defaultArguments.useOldClassFilesReading

  @SerialName("X_USE_TYPE_TABLE")
  protected var `Xuse-type-table`: Boolean = defaultArguments.useTypeTable

  @SerialName("X_VALIDATE_BYTECODE")
  protected var `Xvalidate-bytecode`: Boolean = defaultArguments.validateBytecode

  @SerialName("X_VALUE_CLASSES")
  protected var `Xvalue-classes`: Boolean =
      defaultArguments.getUsingReflection<Boolean>("valueClasses")

  @SerialName("X_WHEN_EXPRESSIONS")
  protected var `Xwhen-expressions`: WhenExpressionsMode? =
      defaultArguments.whenExpressionsGeneration?.let { WhenExpressionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") }

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
      defaultArguments.jvmDefaultStable?.let { JvmDefaultMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") }

  @SerialName("JVM_TARGET")
  protected var `jvm-target`: JvmTarget? =
      defaultArguments.jvmTarget?.let { JvmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") }

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
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return this[key.id] as V
  }

  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    val currentKotlinVersion = KotlinToolingVersion(KC_VERSION)
    if (key.availableSinceVersion > KotlinReleaseVersion(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    this[key.id] = `value`
  }

  @Deprecated(
    message = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments.",
    level = DeprecationLevel.ERROR,
  )
  override operator fun contains(key: JvmCompilerArguments.JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  override fun deepCopy(): JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl().also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

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
    try { arguments.annotationsInMetadata = `Xannotations-in-metadata` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATIONS_IN_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    arguments.assertionsMode = Xassertions?.stringValue
    arguments.backendThreads = `Xbackend-threads`.toString()
    arguments.buildFile = `Xbuild-file`
    try { arguments.setUsingReflection("expectBuiltinsAsPartOfStdlib", `Xcompile-builtins-as-part-of-stdlib`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20 and removed in 2.3.20""").initCause(e) }
    try { arguments.setUsingReflection("compileJava", `Xcompile-java`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_JAVA. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.enableDebugMode = Xdebug
    arguments.defaultScriptExtension = `Xdefault-script-extension`
    arguments.disableStandardScript = `Xdisable-standard-script`
    arguments.emitJvmTypeAnnotations = `Xemit-jvm-type-annotations`
    try { arguments.setUsingReflection("enhanceTypeParameterTypesToDefNotNull", `Xenhance-type-parameter-types-to-def-not-null`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.enhancedCoroutinesDebugging = `Xenhanced-coroutines-debugging` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCED_COROUTINES_DEBUGGING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = `Xgenerate-strict-metadata-version`
    try { arguments.indyAllowAnnotatedLambdas = `Xindy-allow-annotated-lambdas` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INDY_ALLOW_ANNOTATED_LAMBDAS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    try { arguments.setUsingReflection("doNotClearBindingContext", `Xir-do-not-clear-binding-context`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_DO_NOT_CLEAR_BINDING_CONTEXT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("enableIrInliner", `Xir-inliner`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.javaPackagePrefix = `Xjava-package-prefix`
    arguments.javaSourceRoots = `Xjava-source-roots`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    try { arguments.setUsingReflection("javacArguments", `Xjavac-arguments` ?: emptyArray()) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JAVAC_ARGUMENTS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.jdkRelease = `Xjdk-release`?.stringValue
    arguments.jspecifyAnnotations = `Xjspecify-annotations`?.stringValue
    arguments.jvmDefault = `Xjvm-default`
    arguments.enableJvmPreview = `Xjvm-enable-preview`
    try { arguments.jvmExposeBoxed = `Xjvm-expose-boxed` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JVM_EXPOSE_BOXED. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    try { arguments.setUsingReflection("klibLibraries", Xklib?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_KLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.lambdas = Xlambdas?.stringValue
    try { arguments.setUsingReflection("linkViaSignatures", `Xlink-via-signatures`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_LINK_VIA_SIGNATURES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
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
    try { arguments.outputBuiltinsMetadata = `Xoutput-builtins-metadata` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_OUTPUT_BUILTINS_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    arguments.samConversions = `Xsam-conversions`?.stringValue
    arguments.sanitizeParentheses = `Xsanitize-parentheses`
    arguments.scriptResolverEnvironment = `Xscript-resolver-environment`.toTypedArray()
    try { arguments.setUsingReflection("serializeIr", `Xserialize-ir`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SERIALIZE_IR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.stringConcat = `Xstring-concat`?.stringValue
    arguments.supportCompatqualCheckerFrameworkAnnotations = `Xsupport-compatqual-checker-framework-annotations`?.stringValue
    try { arguments.setUsingReflection("suppressDeprecatedJvmTargetWarning", `Xsuppress-deprecated-jvm-target-warning`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.suppressMissingBuiltinsError = `Xsuppress-missing-builtins-error`
    try { arguments.setUsingReflection("typeEnhancementImprovementsInStrictMode", `Xtype-enhancement-improvements-strict-mode`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.useOldInlineClassesManglingScheme = `Xuse-14-inline-classes-mangling-scheme`
    arguments.useFastJarFileSystem = `Xuse-fast-jar-file-system`
    try { arguments.useInlineScopesNumbers = `Xuse-inline-scopes-numbers` } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_INLINE_SCOPES_NUMBERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    try { arguments.setUsingReflection("useJavac", `Xuse-javac`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_JAVAC. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    try { arguments.setUsingReflection("useK2Kapt", `Xuse-k2-kapt`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.3.0""").initCause(e) }
    arguments.useOldClassFilesReading = `Xuse-old-class-files-reading`
    arguments.useTypeTable = `Xuse-type-table`
    arguments.validateBytecode = `Xvalidate-bytecode`
    try { arguments.setUsingReflection("valueClasses", `Xvalue-classes`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VALUE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
    try { arguments.whenExpressionsGeneration = `Xwhen-expressions`?.stringValue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_EXPRESSIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    arguments.classpath = classpath?.map { it.absolutePathStringOrThrow() }?.also { list -> list.checkNoneContains("${File.pathSeparator}") }?.joinToString(File.pathSeparator)
    arguments.destination = d
    arguments.expression = expression
    arguments.includeRuntime = `include-runtime`
    arguments.javaParameters = `java-parameters`
    arguments.jdkHome = `jdk-home`?.absolutePathStringOrThrow()
    try { arguments.jvmDefaultStable = `jvm-default`?.stringValue } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: JVM_DEFAULT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
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
    return arguments
  }

  @Suppress("DEPRECATION")
  protected fun applyCompilerArguments(arguments: K2JVMCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { `Xabi-stability` = arguments.abiStability?.let { AbiStabilityMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xadd-modules` = arguments.additionalJavaModules.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xallow-no-source-files` = arguments.allowNoSourceFiles } catch (_: NoSuchMethodError) {  }
    try { `Xallow-unstable-dependencies` = arguments.allowUnstableDependencies } catch (_: NoSuchMethodError) {  }
    try { `Xannotations-in-metadata` = arguments.annotationsInMetadata } catch (_: NoSuchMethodError) {  }
    try { Xassertions = arguments.assertionsMode?.let { AssertionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xbackend-threads` = arguments.backendThreads.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { `Xbuild-file` = arguments.buildFile } catch (_: NoSuchMethodError) {  }
    try { `Xcompile-builtins-as-part-of-stdlib` = arguments.getUsingReflection<Boolean>("expectBuiltinsAsPartOfStdlib") } catch (_: NoSuchMethodError) {  }
    try { `Xcompile-java` = arguments.getUsingReflection<Boolean>("compileJava") } catch (_: NoSuchMethodError) {  }
    try { Xdebug = arguments.enableDebugMode } catch (_: NoSuchMethodError) {  }
    try { `Xdefault-script-extension` = arguments.defaultScriptExtension } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-standard-script` = arguments.disableStandardScript } catch (_: NoSuchMethodError) {  }
    try { `Xemit-jvm-type-annotations` = arguments.emitJvmTypeAnnotations } catch (_: NoSuchMethodError) {  }
    try { `Xenhance-type-parameter-types-to-def-not-null` = arguments.getUsingReflection<Boolean>("enhanceTypeParameterTypesToDefNotNull") } catch (_: NoSuchMethodError) {  }
    try { `Xenhanced-coroutines-debugging` = arguments.enhancedCoroutinesDebugging } catch (_: NoSuchMethodError) {  }
    try { `Xfriend-paths` = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xgenerate-strict-metadata-version` = arguments.strictMetadataVersionSemantics } catch (_: NoSuchMethodError) {  }
    try { `Xindy-allow-annotated-lambdas` = arguments.indyAllowAnnotatedLambdas } catch (_: NoSuchMethodError) {  }
    try { `Xir-do-not-clear-binding-context` = arguments.getUsingReflection<Boolean>("doNotClearBindingContext") } catch (_: NoSuchMethodError) {  }
    try { `Xir-inliner` = arguments.getUsingReflection<Boolean>("enableIrInliner") } catch (_: NoSuchMethodError) {  }
    try { `Xjava-package-prefix` = arguments.javaPackagePrefix } catch (_: NoSuchMethodError) {  }
    try { `Xjava-source-roots` = arguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xjavac-arguments` = arguments.getUsingReflection<Array<String>>("javacArguments") } catch (_: NoSuchMethodError) {  }
    try { `Xjdk-release` = arguments.jdkRelease?.let { JdkRelease.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xjspecify-annotations` = arguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-default` = arguments.jvmDefault } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-enable-preview` = arguments.enableJvmPreview } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-expose-boxed` = arguments.jvmExposeBoxed } catch (_: NoSuchMethodError) {  }
    try { Xklib = arguments.getUsingReflection<String?>("klibLibraries")?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { Xlambdas = arguments.lambdas?.let { LambdasMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xlink-via-signatures` = arguments.getUsingReflection<Boolean>("linkViaSignatures") } catch (_: NoSuchMethodError) {  }
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
    try { `Xsam-conversions` = arguments.samConversions?.let { SamConversionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xsanitize-parentheses` = arguments.sanitizeParentheses } catch (_: NoSuchMethodError) {  }
    try { `Xscript-resolver-environment` = arguments.scriptResolverEnvironment.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xserialize-ir` = arguments.getUsingReflection<String>("serializeIr") } catch (_: NoSuchMethodError) {  }
    try { `Xstring-concat` = arguments.stringConcat?.let { StringConcatMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xsupport-compatqual-checker-framework-annotations` = arguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-deprecated-jvm-target-warning` = arguments.getUsingReflection<Boolean>("suppressDeprecatedJvmTargetWarning") } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-missing-builtins-error` = arguments.suppressMissingBuiltinsError } catch (_: NoSuchMethodError) {  }
    try { `Xtype-enhancement-improvements-strict-mode` = arguments.getUsingReflection<Boolean>("typeEnhancementImprovementsInStrictMode") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-14-inline-classes-mangling-scheme` = arguments.useOldInlineClassesManglingScheme } catch (_: NoSuchMethodError) {  }
    try { `Xuse-fast-jar-file-system` = arguments.useFastJarFileSystem } catch (_: NoSuchMethodError) {  }
    try { `Xuse-inline-scopes-numbers` = arguments.useInlineScopesNumbers } catch (_: NoSuchMethodError) {  }
    try { `Xuse-javac` = arguments.getUsingReflection<Boolean>("useJavac") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-k2-kapt` = arguments.getUsingReflection<Boolean?>("useK2Kapt") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-old-class-files-reading` = arguments.useOldClassFilesReading } catch (_: NoSuchMethodError) {  }
    try { `Xuse-type-table` = arguments.useTypeTable } catch (_: NoSuchMethodError) {  }
    try { `Xvalidate-bytecode` = arguments.validateBytecode } catch (_: NoSuchMethodError) {  }
    try { `Xvalue-classes` = arguments.getUsingReflection<Boolean>("valueClasses") } catch (_: NoSuchMethodError) {  }
    try { `Xwhen-expressions` = arguments.whenExpressionsGeneration?.let { WhenExpressionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $it") } } catch (_: NoSuchMethodError) {  }
    try { classpath = arguments.classpath?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { d = arguments.destination } catch (_: NoSuchMethodError) {  }
    try { expression = arguments.expression } catch (_: NoSuchMethodError) {  }
    try { `include-runtime` = arguments.includeRuntime } catch (_: NoSuchMethodError) {  }
    try { `java-parameters` = arguments.javaParameters } catch (_: NoSuchMethodError) {  }
    try { `jdk-home` = arguments.jdkHome?.let { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `jvm-default` = arguments.jvmDefaultStable?.let { JvmDefaultMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `jvm-target` = arguments.jvmTarget?.let { JvmTarget.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") } } catch (_: NoSuchMethodError) {  }
    try { `module-name` = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { `no-jdk` = arguments.noJdk } catch (_: NoSuchMethodError) {  }
    try { `no-reflect` = arguments.noReflect } catch (_: NoSuchMethodError) {  }
    try { `no-stdlib` = arguments.noStdlib } catch (_: NoSuchMethodError) {  }
    try { `script-templates` = arguments.scriptTemplates.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { Xprofile = applyProfileCompilerCommand(Xprofile, arguments) } catch (_: NoSuchMethodError) {  }
    try { `Xnullability-annotations` = applyNullabilityAnnotations(`Xnullability-annotations`, arguments) } catch (_: NoSuchMethodError) {  }
    try { Xjsr305 = applyJsr305(Xjsr305, arguments) } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

  @Deprecated(
    message = "This method is deprecated. Use applyCommandLineArguments instead.",
    level = DeprecationLevel.WARNING,
  )
  override fun applyArgumentStrings(arguments: List<String>) {
    val compilerArgs: K2JVMCompilerArguments = parseCommandLineArguments(arguments)
    validateArguments(compilerArgs.errors)?.let { throw CompilerArgumentsParseException(it) }
    applyCompilerArguments(compilerArgs)
  }

  @DelicateBuildToolsApi
  override fun applyCommandLineArguments(arguments: List<String>) {
    error("Will never be called, it's handled in JvmCompilerArgumentsImplV1Adapter")
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

    public val X_FRIEND_PATHS: JvmCompilerArgument<List<Path>> =
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

    public val X_JAVA_SOURCE_ROOTS: JvmCompilerArgument<List<Path>> =
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

    public val X_KLIB: JvmCompilerArgument<List<Path>?> = JvmCompilerArgument("X_KLIB")

    public val X_LAMBDAS: JvmCompilerArgument<LambdasMode?> = JvmCompilerArgument("X_LAMBDAS")

    public val X_LINK_VIA_SIGNATURES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_LINK_VIA_SIGNATURES")

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
