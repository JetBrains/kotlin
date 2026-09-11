// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

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
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JvmCompilerArgumentsImpl.Companion.X_JSR305
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JvmCompilerArgumentsImpl.Companion.X_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.`internal`.arguments.JvmCompilerArgumentsImpl.Companion.X_PROFILE
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
  argumentValidationErrors: Set<String> = emptySet(),
  restrictedArgViolations: List<RestrictedArgViolation> = emptyList(),
  argumentParseDiagnostics: ArgumentParseDiagnostics = ArgumentParseDiagnostics(),
) : CommonCompilerArgumentsImpl(argumentValidationErrors, restrictedArgViolations, argumentParseDiagnostics),
    JvmCompilerArguments,
    JvmCompilerArguments.Builder,
    DeepCopyable<JvmCompilerArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @SerialName("X_ABI_STABILITY")
  protected var `Xabi-stability`: AbiStabilityMode?

  @SerialName("X_ADD_MODULES")
  protected var `Xadd-modules`: List<String>

  @SerialName("X_ALLOW_NO_SOURCE_FILES")
  protected var `Xallow-no-source-files`: Boolean

  @SerialName("X_ALLOW_UNSTABLE_DEPENDENCIES")
  protected var `Xallow-unstable-dependencies`: Boolean

  @SerialName("X_ANNOTATIONS_IN_METADATA")
  protected var `Xannotations-in-metadata`: Boolean

  @SerialName("X_ASSERTIONS")
  protected var Xassertions: AssertionsMode?

  @SerialName("X_BACKEND_THREADS")
  protected var `Xbackend-threads`: Int

  @SerialName("X_BUILD_FILE")
  protected var `Xbuild-file`: String?

  @SerialName("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB")
  protected var `Xcompile-builtins-as-part-of-stdlib`: Boolean

  @SerialName("X_COMPILE_JAVA")
  protected var `Xcompile-java`: Boolean

  @SerialName("X_DEBUG")
  protected var Xdebug: Boolean

  @SerialName("X_DEFAULT_SCRIPT_EXTENSION")
  protected var `Xdefault-script-extension`: String?

  @SerialName("X_DIRECT_JAVA_ACTUALIZATION")
  protected var `Xdirect-java-actualization`: Boolean

  @SerialName("X_DISABLE_STANDARD_SCRIPT")
  protected var `Xdisable-standard-script`: Boolean

  @SerialName("X_EMIT_JVM_TYPE_ANNOTATIONS")
  protected var `Xemit-jvm-type-annotations`: Boolean

  @SerialName("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL")
  protected var `Xenhance-type-parameter-types-to-def-not-null`: Boolean

  @SerialName("X_ENHANCED_COROUTINES_DEBUGGING")
  protected var `Xenhanced-coroutines-debugging`: Boolean

  @SerialName("X_FRIEND_PATHS")
  protected var `Xfriend-paths`: List<Path>

  @SerialName("X_GENERATE_STRICT_METADATA_VERSION")
  protected var `Xgenerate-strict-metadata-version`: Boolean

  @SerialName("X_IGNORED_ANNOTATIONS_FOR_BRIDGES")
  protected var `Xignored-annotations-for-bridges`: List<String>

  @SerialName("X_INDY_ALLOW_ANNOTATED_LAMBDAS")
  protected var `Xindy-allow-annotated-lambdas`: Boolean?

  @SerialName("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT")
  protected var `Xir-do-not-clear-binding-context`: Boolean

  @SerialName("X_IR_INLINER")
  protected var `Xir-inliner`: Boolean

  @SerialName("X_JAVA_DIRECT")
  protected var `Xjava-direct`: Boolean

  @SerialName("X_JAVA_PACKAGE_PREFIX")
  protected var `Xjava-package-prefix`: String?

  @SerialName("X_JAVA_SOURCE_ROOTS")
  protected var `Xjava-source-roots`: List<Path>

  @SerialName("X_JAVAC_ARGUMENTS")
  protected var `Xjavac-arguments`: Array<String>?

  @SerialName("X_JDK_RELEASE")
  protected var `Xjdk-release`: JdkRelease?

  @SerialName("X_JSPECIFY_ANNOTATIONS")
  protected var `Xjspecify-annotations`: JspecifyAnnotationsMode?

  @SerialName("X_JVM_DEFAULT")
  protected var `Xjvm-default`: String?

  @SerialName("X_JVM_ENABLE_PREVIEW")
  protected var `Xjvm-enable-preview`: Boolean

  @SerialName("X_JVM_EXPOSE_BOXED")
  protected var `Xjvm-expose-boxed`: Boolean

  @SerialName("X_KLIB")
  protected var Xklib: List<Path>?

  @SerialName("X_LAMBDAS")
  protected var Xlambdas: LambdasMode?

  @SerialName("X_LINK_VIA_SIGNATURES")
  protected var `Xlink-via-signatures`: Boolean

  @SerialName("X_MODULE_PATH")
  protected var `Xmodule-path`: List<Path>?

  @SerialName("X_MULTIFILE_PARTS_INHERIT")
  protected var `Xmultifile-parts-inherit`: Boolean

  @SerialName("X_NO_CALL_ASSERTIONS")
  protected var `Xno-call-assertions`: Boolean

  @SerialName("X_NO_NEW_JAVA_ANNOTATION_TARGETS")
  protected var `Xno-new-java-annotation-targets`: Boolean

  @SerialName("X_NO_OPTIMIZE")
  protected var `Xno-optimize`: Boolean

  @SerialName("X_NO_PARAM_ASSERTIONS")
  protected var `Xno-param-assertions`: Boolean

  @SerialName("X_NO_RECEIVER_ASSERTIONS")
  protected var `Xno-receiver-assertions`: Boolean

  @SerialName("X_NO_RESET_JAR_TIMESTAMPS")
  protected var `Xno-reset-jar-timestamps`: Boolean

  @SerialName("X_NO_SOURCE_DEBUG_EXTENSION")
  protected var `Xno-source-debug-extension`: Boolean

  @SerialName("X_NO_UNIFIED_NULL_CHECKS")
  protected var `Xno-unified-null-checks`: Boolean

  @SerialName("X_OUTPUT_BUILTINS_METADATA")
  protected var `Xoutput-builtins-metadata`: Boolean

  @SerialName("X_SAM_CONVERSIONS")
  protected var `Xsam-conversions`: SamConversionsMode?

  @SerialName("X_SANITIZE_PARENTHESES")
  protected var `Xsanitize-parentheses`: Boolean

  @SerialName("X_SCRIPT_RESOLVER_ENVIRONMENT")
  protected var `Xscript-resolver-environment`: List<String>

  @SerialName("X_SERIALIZE_IR")
  protected var `Xserialize-ir`: String

  @SerialName("X_STRING_CONCAT")
  protected var `Xstring-concat`: StringConcatMode?

  @SerialName("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")
  protected var `Xsupport-compatqual-checker-framework-annotations`: CompatqualAnnotationsMode?

  @SerialName("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING")
  protected var `Xsuppress-deprecated-jvm-target-warning`: Boolean

  @SerialName("X_SUPPRESS_MISSING_BUILTINS_ERROR")
  protected var `Xsuppress-missing-builtins-error`: Boolean

  @SerialName("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE")
  protected var `Xtype-enhancement-improvements-strict-mode`: Boolean

  @SerialName("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")
  protected var `Xuse-14-inline-classes-mangling-scheme`: Boolean

  @SerialName("X_USE_FAST_JAR_FILE_SYSTEM")
  protected var `Xuse-fast-jar-file-system`: Boolean?

  @SerialName("X_USE_INLINE_SCOPES_NUMBERS")
  protected var `Xuse-inline-scopes-numbers`: Boolean

  @SerialName("X_USE_JAVAC")
  protected var `Xuse-javac`: Boolean

  @SerialName("X_USE_K2_KAPT")
  protected var `Xuse-k2-kapt`: Boolean?

  @SerialName("X_USE_METADATA_ON_INCREMENTAL_CLASSPATH")
  protected var `Xuse-metadata-on-incremental-classpath`: Boolean

  @SerialName("X_USE_OLD_CLASS_FILES_READING")
  protected var `Xuse-old-class-files-reading`: Boolean

  @SerialName("X_USE_TYPE_TABLE")
  protected var `Xuse-type-table`: Boolean

  @SerialName("X_VALHALLA_SUPPORT")
  protected var `Xvalhalla-support`: ValhallaSupportMode?

  @SerialName("X_VALIDATE_BYTECODE")
  protected var `Xvalidate-bytecode`: Boolean

  @SerialName("X_VALUE_CLASSES")
  protected var `Xvalue-classes`: Boolean

  @SerialName("X_WHEN_EXPRESSIONS")
  protected var `Xwhen-expressions`: WhenExpressionsMode?

  @SerialName("CLASSPATH")
  protected var classpath: List<Path>?

  @SerialName("D")
  protected var d: String?

  @SerialName("EXPRESSION")
  protected var expression: String?

  @SerialName("INCLUDE_RUNTIME")
  protected var `include-runtime`: Boolean

  @SerialName("JAVA_PARAMETERS")
  protected var `java-parameters`: Boolean

  @SerialName("JDK_HOME")
  protected var `jdk-home`: Path?

  @SerialName("JVM_DEFAULT")
  protected var `jvm-default`: JvmDefaultMode?

  @SerialName("JVM_TARGET")
  protected var `jvm-target`: JvmTarget?

  @SerialName("MODULE_NAME")
  protected var `module-name`: String?

  @SerialName("NO_JDK")
  protected var `no-jdk`: Boolean

  @SerialName("NO_REFLECT")
  protected var `no-reflect`: Boolean

  @SerialName("NO_STDLIB")
  protected var `no-stdlib`: Boolean

  @SerialName("SCRIPT_TEMPLATES")
  protected var `script-templates`: List<String>

  @SerialName("X_PROFILE")
  protected var Xprofile: ProfileCompilerCommand?

  @SerialName("X_NULLABILITY_ANNOTATIONS")
  protected var `Xnullability-annotations`: List<NullabilityAnnotation>

  @SerialName("X_JSR305")
  protected var Xjsr305: List<Jsr305>
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

  override fun deepCopy(): JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(argumentValidationErrors.toSet(), restrictedArgViolations.toList(), argumentParseDiagnostics.copy()).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }

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
    try { arguments.setUsingReflection("expectBuiltinsAsPartOfStdlib", `Xcompile-builtins-as-part-of-stdlib`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.20""").initCause(e) }
    try { arguments.setUsingReflection("compileJava", `Xcompile-java`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_JAVA. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.enableDebugMode = Xdebug
    arguments.defaultScriptExtension = `Xdefault-script-extension`
    arguments.directJavaActualization = `Xdirect-java-actualization`
    arguments.disableStandardScript = `Xdisable-standard-script`
    arguments.emitJvmTypeAnnotations = `Xemit-jvm-type-annotations`
    try { arguments.setUsingReflection("enhanceTypeParameterTypesToDefNotNull", `Xenhance-type-parameter-types-to-def-not-null`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.enhancedCoroutinesDebugging = `Xenhanced-coroutines-debugging`
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = `Xgenerate-strict-metadata-version`
    arguments.ignoredAnnotationsForBridges = `Xignored-annotations-for-bridges`.toTypedArray()
    arguments.indyAllowAnnotatedLambdas = `Xindy-allow-annotated-lambdas`
    try { arguments.setUsingReflection("doNotClearBindingContext", `Xir-do-not-clear-binding-context`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_DO_NOT_CLEAR_BINDING_CONTEXT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("enableIrInliner", `Xir-inliner`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.javaDirect = `Xjava-direct`
    arguments.javaPackagePrefix = `Xjava-package-prefix`
    arguments.javaSourceRoots = `Xjava-source-roots`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    try { arguments.setUsingReflection("javacArguments", `Xjavac-arguments` ?: emptyArray()) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JAVAC_ARGUMENTS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.jdkRelease = `Xjdk-release`?.stringValue
    arguments.jspecifyAnnotations = `Xjspecify-annotations`?.stringValue
    arguments.jvmDefault = `Xjvm-default`
    arguments.enableJvmPreview = `Xjvm-enable-preview`
    arguments.jvmExposeBoxed = `Xjvm-expose-boxed`
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
    arguments.outputBuiltinsMetadata = `Xoutput-builtins-metadata`
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
    arguments.useInlineScopesNumbers = `Xuse-inline-scopes-numbers`
    try { arguments.setUsingReflection("useJavac", `Xuse-javac`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_JAVAC. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    try { arguments.setUsingReflection("useK2Kapt", `Xuse-k2-kapt`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.useMetadataOnIncrementalClasspath = `Xuse-metadata-on-incremental-classpath`
    arguments.useOldClassFilesReading = `Xuse-old-class-files-reading`
    arguments.useTypeTable = `Xuse-type-table`
    arguments.valhallaSupport = `Xvalhalla-support`?.stringValue
    arguments.validateBytecode = `Xvalidate-bytecode`
    try { arguments.setUsingReflection("valueClasses", `Xvalue-classes`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VALUE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
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
    if (X_PROFILE in this) { arguments.applyProfileCompilerCommand(get(X_PROFILE))}
    if (X_NULLABILITY_ANNOTATIONS in this) { arguments.applyNullabilityAnnotations(get(X_NULLABILITY_ANNOTATIONS))}
    if (X_JSR305 in this) { arguments.applyJsr305(get(X_JSR305))}
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
    try { `Xcompile-builtins-as-part-of-stdlib` = arguments.getUsingReflection<Boolean>("expectBuiltinsAsPartOfStdlib") } catch (_: NoSuchMethodError) {  }
    try { `Xcompile-java` = arguments.getUsingReflection<Boolean>("compileJava") } catch (_: NoSuchMethodError) {  }
    try { Xdebug = arguments.enableDebugMode } catch (_: NoSuchMethodError) {  }
    try { `Xdefault-script-extension` = arguments.defaultScriptExtension } catch (_: NoSuchMethodError) {  }
    try { `Xdirect-java-actualization` = arguments.directJavaActualization } catch (_: NoSuchMethodError) {  }
    try { `Xdisable-standard-script` = arguments.disableStandardScript } catch (_: NoSuchMethodError) {  }
    try { `Xemit-jvm-type-annotations` = arguments.emitJvmTypeAnnotations } catch (_: NoSuchMethodError) {  }
    try { `Xenhance-type-parameter-types-to-def-not-null` = arguments.getUsingReflection<Boolean>("enhanceTypeParameterTypesToDefNotNull") } catch (_: NoSuchMethodError) {  }
    try { `Xenhanced-coroutines-debugging` = arguments.enhancedCoroutinesDebugging } catch (_: NoSuchMethodError) {  }
    try { `Xfriend-paths` = arguments.friendPaths.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xgenerate-strict-metadata-version` = arguments.strictMetadataVersionSemantics } catch (_: NoSuchMethodError) {  }
    try { `Xignored-annotations-for-bridges` = arguments.ignoredAnnotationsForBridges.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xindy-allow-annotated-lambdas` = arguments.indyAllowAnnotatedLambdas } catch (_: NoSuchMethodError) {  }
    try { `Xir-do-not-clear-binding-context` = arguments.getUsingReflection<Boolean>("doNotClearBindingContext") } catch (_: NoSuchMethodError) {  }
    try { `Xir-inliner` = arguments.getUsingReflection<Boolean>("enableIrInliner") } catch (_: NoSuchMethodError) {  }
    try { `Xjava-direct` = arguments.javaDirect } catch (_: NoSuchMethodError) {  }
    try { `Xjava-package-prefix` = arguments.javaPackagePrefix } catch (_: NoSuchMethodError) {  }
    try { `Xjava-source-roots` = arguments.javaSourceRoots.mapOrEmpty { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { `Xjavac-arguments` = arguments.getUsingReflection<Array<String>>("javacArguments") } catch (_: NoSuchMethodError) {  }
    try { `Xjdk-release` = arguments.jdkRelease?.let { JdkRelease.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jdkRelease, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xjspecify-annotations` = arguments.jspecifyAnnotations?.let { JspecifyAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::jspecifyAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-default` = arguments.jvmDefault } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-enable-preview` = arguments.enableJvmPreview } catch (_: NoSuchMethodError) {  }
    try { `Xjvm-expose-boxed` = arguments.jvmExposeBoxed } catch (_: NoSuchMethodError) {  }
    try { Xklib = arguments.getUsingReflection<String?>("klibLibraries")?.split(File.pathSeparator)?.map { kotlin.io.path.Path(it) } } catch (_: NoSuchMethodError) {  }
    try { Xlambdas = arguments.lambdas?.let { LambdasMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::lambdas, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
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
    try { `Xsam-conversions` = arguments.samConversions?.let { SamConversionsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::samConversions, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xsanitize-parentheses` = arguments.sanitizeParentheses } catch (_: NoSuchMethodError) {  }
    try { `Xscript-resolver-environment` = arguments.scriptResolverEnvironment.toListOrEmpty() } catch (_: NoSuchMethodError) {  }
    try { `Xserialize-ir` = arguments.getUsingReflection<String>("serializeIr") } catch (_: NoSuchMethodError) {  }
    try { `Xstring-concat` = arguments.stringConcat?.let { StringConcatMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::stringConcat, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xsupport-compatqual-checker-framework-annotations` = arguments.supportCompatqualCheckerFrameworkAnnotations?.let { CompatqualAnnotationsMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::supportCompatqualCheckerFrameworkAnnotations, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-deprecated-jvm-target-warning` = arguments.getUsingReflection<Boolean>("suppressDeprecatedJvmTargetWarning") } catch (_: NoSuchMethodError) {  }
    try { `Xsuppress-missing-builtins-error` = arguments.suppressMissingBuiltinsError } catch (_: NoSuchMethodError) {  }
    try { `Xtype-enhancement-improvements-strict-mode` = arguments.getUsingReflection<Boolean>("typeEnhancementImprovementsInStrictMode") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-14-inline-classes-mangling-scheme` = arguments.useOldInlineClassesManglingScheme } catch (_: NoSuchMethodError) {  }
    try { `Xuse-fast-jar-file-system` = arguments.useFastJarFileSystem } catch (_: NoSuchMethodError) {  }
    try { `Xuse-inline-scopes-numbers` = arguments.useInlineScopesNumbers } catch (_: NoSuchMethodError) {  }
    try { `Xuse-javac` = arguments.getUsingReflection<Boolean>("useJavac") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-k2-kapt` = arguments.getUsingReflection<Boolean?>("useK2Kapt") } catch (_: NoSuchMethodError) {  }
    try { `Xuse-metadata-on-incremental-classpath` = arguments.useMetadataOnIncrementalClasspath } catch (_: NoSuchMethodError) {  }
    try { `Xuse-old-class-files-reading` = arguments.useOldClassFilesReading } catch (_: NoSuchMethodError) {  }
    try { `Xuse-type-table` = arguments.useTypeTable } catch (_: NoSuchMethodError) {  }
    try { `Xvalhalla-support` = arguments.valhallaSupport?.let { ValhallaSupportMode.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> checkCaseMatches(_restrictedArgViolations, arguments::valhallaSupport, entry.stringValue, it) } ?: throw CompilerArgumentsParseException("Unknown -Xvalhalla-support value: $it") } } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { `Xvalidate-bytecode` = arguments.validateBytecode } catch (_: NoSuchMethodError) {  }
    try { `Xvalue-classes` = arguments.getUsingReflection<Boolean>("valueClasses") } catch (_: NoSuchMethodError) {  }
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
    try { this[X_PROFILE] = applyProfileCompilerCommand(if(X_PROFILE in this) this[X_PROFILE] else null, arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { this[X_NULLABILITY_ANNOTATIONS] = applyNullabilityAnnotations(if(X_NULLABILITY_ANNOTATIONS in this) this[X_NULLABILITY_ANNOTATIONS] else emptyList<NullabilityAnnotation>(), arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
    try { this[X_JSR305] = applyJsr305(if(X_JSR305 in this) this[X_JSR305] else emptyList<Jsr305>(), arguments) } catch (ex: CompilerArgumentsParseException) { _argumentValidationErrors.add(ex.message ?: "Error parsing compiler arguments") } catch (_: NoSuchMethodError) {  }
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
    try { arguments.setUsingReflection("expectBuiltinsAsPartOfStdlib", `Xcompile-builtins-as-part-of-stdlib`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.20""").initCause(e) }
    try { arguments.setUsingReflection("compileJava", `Xcompile-java`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_JAVA. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.enableDebugMode = Xdebug
    arguments.defaultScriptExtension = `Xdefault-script-extension`
    arguments.directJavaActualization = `Xdirect-java-actualization`
    arguments.disableStandardScript = `Xdisable-standard-script`
    arguments.emitJvmTypeAnnotations = `Xemit-jvm-type-annotations`
    try { arguments.setUsingReflection("enhanceTypeParameterTypesToDefNotNull", `Xenhance-type-parameter-types-to-def-not-null`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    arguments.enhancedCoroutinesDebugging = `Xenhanced-coroutines-debugging`
    arguments.friendPaths = `Xfriend-paths`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    arguments.strictMetadataVersionSemantics = `Xgenerate-strict-metadata-version`
    arguments.ignoredAnnotationsForBridges = `Xignored-annotations-for-bridges`.toTypedArray()
    arguments.indyAllowAnnotatedLambdas = `Xindy-allow-annotated-lambdas`
    try { arguments.setUsingReflection("doNotClearBindingContext", `Xir-do-not-clear-binding-context`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_DO_NOT_CLEAR_BINDING_CONTEXT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.5.0""").initCause(e) }
    try { arguments.setUsingReflection("enableIrInliner", `Xir-inliner`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.javaDirect = `Xjava-direct`
    arguments.javaPackagePrefix = `Xjava-package-prefix`
    arguments.javaSourceRoots = `Xjava-source-roots`.map { it.absolutePathStringOrThrow() }.also { list -> list.checkNoneContains(",") }.toTypedArray()
    try { arguments.setUsingReflection("javacArguments", `Xjavac-arguments` ?: emptyArray()) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JAVAC_ARGUMENTS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    arguments.jdkRelease = `Xjdk-release`?.stringValue
    arguments.jspecifyAnnotations = `Xjspecify-annotations`?.stringValue
    arguments.jvmDefault = `Xjvm-default`
    arguments.enableJvmPreview = `Xjvm-enable-preview`
    arguments.jvmExposeBoxed = `Xjvm-expose-boxed`
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
    arguments.outputBuiltinsMetadata = `Xoutput-builtins-metadata`
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
    arguments.useInlineScopesNumbers = `Xuse-inline-scopes-numbers`
    try { arguments.setUsingReflection("useJavac", `Xuse-javac`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_JAVAC. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    try { arguments.setUsingReflection("useK2Kapt", `Xuse-k2-kapt`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    arguments.useMetadataOnIncrementalClasspath = `Xuse-metadata-on-incremental-classpath`
    arguments.useOldClassFilesReading = `Xuse-old-class-files-reading`
    arguments.useTypeTable = `Xuse-type-table`
    arguments.valhallaSupport = `Xvalhalla-support`?.stringValue
    arguments.validateBytecode = `Xvalidate-bytecode`
    try { arguments.setUsingReflection("valueClasses", `Xvalue-classes`) } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_VALUE_CLASSES. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.20""").initCause(e) }
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
    if (X_NULLABILITY_ANNOTATIONS in this) { arguments.applyNullabilityAnnotations(get(X_NULLABILITY_ANNOTATIONS))}
    if (X_JSR305 in this) { arguments.applyJsr305(get(X_JSR305))}
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

    public val X_COMPILE_BUILTINS_AS_PART_OF_STDLIB: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB")

    public val X_COMPILE_JAVA: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_COMPILE_JAVA")

    public val X_DEBUG: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_DEBUG")

    public val X_DEFAULT_SCRIPT_EXTENSION: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_DEFAULT_SCRIPT_EXTENSION")

    public val X_DIRECT_JAVA_ACTUALIZATION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_DIRECT_JAVA_ACTUALIZATION")

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

    public val X_IGNORED_ANNOTATIONS_FOR_BRIDGES: JvmCompilerArgument<List<String>> =
        JvmCompilerArgument("X_IGNORED_ANNOTATIONS_FOR_BRIDGES")

    public val X_INDY_ALLOW_ANNOTATED_LAMBDAS: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_INDY_ALLOW_ANNOTATED_LAMBDAS")

    public val X_IR_DO_NOT_CLEAR_BINDING_CONTEXT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT")

    public val X_IR_INLINER: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_IR_INLINER")

    public val X_JAVA_DIRECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_JAVA_DIRECT")

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
