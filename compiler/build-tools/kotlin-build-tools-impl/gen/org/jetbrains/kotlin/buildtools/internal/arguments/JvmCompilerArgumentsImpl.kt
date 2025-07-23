// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.arguments

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.`internal`.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.INCLUDE_RUNTIME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JAVA_PARAMETERS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JDK_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JVM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_JDK
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.SCRIPT_TEMPLATES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ABI_STABILITY
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ADD_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ALLOW_NO_SOURCE_FILES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ALLOW_UNSTABLE_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ANNOTATIONS_IN_METADATA
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_BACKEND_THREADS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_COMPILE_BUILTINS_AS_PART_OF_STDLIB
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_DEBUG
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_DEFAULT_SCRIPT_EXTENSION
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_DISABLE_STANDARD_SCRIPT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_EMIT_JVM_TYPE_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ENHANCED_COROUTINES_DEBUGGING
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_GENERATE_STRICT_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_INDY_ALLOW_ANNOTATED_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_IR_DO_NOT_CLEAR_BINDING_CONTEXT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JAVA_PACKAGE_PREFIX
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JAVA_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JDK_RELEASE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSPECIFY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSR305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JVM_ENABLE_PREVIEW
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JVM_EXPOSE_BOXED
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_KLIB
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_LINK_VIA_SIGNATURES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_MODULE_PATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_MULTIFILE_PARTS_INHERIT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_CALL_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_NEW_JAVA_ANNOTATION_TARGETS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_OPTIMIZE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_PARAM_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_RECEIVER_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_RESET_JAR_TIMESTAMPS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_SOURCE_DEBUG_EXTENSION
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NO_UNIFIED_NULL_CHECKS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_OUTPUT_BUILTINS_METADATA
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_PROFILE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SANITIZE_PARENTHESES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SERIALIZE_IR
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_STRING_CONCAT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPRESS_MISSING_BUILTINS_ERROR
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_USE_14_INLINE_CLASSES_MANGLING_SCHEME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_USE_FAST_JAR_FILE_SYSTEM
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_USE_INLINE_SCOPES_NUMBERS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_USE_K2_KAPT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_USE_OLD_CLASS_FILES_READING
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_USE_TYPE_TABLE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_VALIDATE_BYTECODE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_VALUE_CLASSES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_WHEN_EXPRESSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

public class JvmCompilerArgumentsImpl : CommonCompilerArgumentsImpl(), JvmCompilerArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  @UseFromImplModuleRestricted
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = optionsMap[key.id] as V

  @UseFromImplModuleRestricted
  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2JVMCompilerArguments = K2JVMCompilerArguments()): K2JVMCompilerArguments {
    super.toCompilerArguments(arguments)
    if ("CLASSPATH" in optionsMap) { arguments.classpath = get(CLASSPATH) }
    if ("INCLUDE_RUNTIME" in optionsMap) { arguments.includeRuntime = get(INCLUDE_RUNTIME) }
    if ("JDK_HOME" in optionsMap) { arguments.jdkHome = get(JDK_HOME) }
    if ("NO_JDK" in optionsMap) { arguments.noJdk = get(NO_JDK) }
    if ("NO_STDLIB" in optionsMap) { arguments.noStdlib = get(NO_STDLIB) }
    if ("NO_REFLECT" in optionsMap) { arguments.noReflect = get(NO_REFLECT) }
    if ("SCRIPT_TEMPLATES" in optionsMap) { arguments.scriptTemplates = get(SCRIPT_TEMPLATES) }
    if ("MODULE_NAME" in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if ("JVM_TARGET" in optionsMap) { arguments.jvmTarget = get(JVM_TARGET)?.stringValue }
    if ("JAVA_PARAMETERS" in optionsMap) { arguments.javaParameters = get(JAVA_PARAMETERS) }
    if ("JVM_DEFAULT" in optionsMap) { arguments.jvmDefaultStable = get(JVM_DEFAULT) }
    if ("X_ALLOW_UNSTABLE_DEPENDENCIES" in optionsMap) { arguments.allowUnstableDependencies = get(X_ALLOW_UNSTABLE_DEPENDENCIES) }
    if ("X_ABI_STABILITY" in optionsMap) { arguments.abiStability = get(X_ABI_STABILITY) }
    if ("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT" in optionsMap) { arguments.doNotClearBindingContext = get(X_IR_DO_NOT_CLEAR_BINDING_CONTEXT) }
    if ("X_BACKEND_THREADS" in optionsMap) { arguments.backendThreads = get(X_BACKEND_THREADS).toString() }
    if ("X_MODULE_PATH" in optionsMap) { arguments.javaModulePath = get(X_MODULE_PATH) }
    if ("X_ADD_MODULES" in optionsMap) { arguments.additionalJavaModules = get(X_ADD_MODULES) }
    if ("X_NO_CALL_ASSERTIONS" in optionsMap) { arguments.noCallAssertions = get(X_NO_CALL_ASSERTIONS) }
    if ("X_NO_RECEIVER_ASSERTIONS" in optionsMap) { arguments.noReceiverAssertions = get(X_NO_RECEIVER_ASSERTIONS) }
    if ("X_NO_PARAM_ASSERTIONS" in optionsMap) { arguments.noParamAssertions = get(X_NO_PARAM_ASSERTIONS) }
    if ("X_NO_OPTIMIZE" in optionsMap) { arguments.noOptimize = get(X_NO_OPTIMIZE) }
    if ("X_ASSERTIONS" in optionsMap) { arguments.assertionsMode = get(X_ASSERTIONS) }
    if ("X_MULTIFILE_PARTS_INHERIT" in optionsMap) { arguments.inheritMultifileParts = get(X_MULTIFILE_PARTS_INHERIT) }
    if ("X_USE_TYPE_TABLE" in optionsMap) { arguments.useTypeTable = get(X_USE_TYPE_TABLE) }
    if ("X_USE_OLD_CLASS_FILES_READING" in optionsMap) { arguments.useOldClassFilesReading = get(X_USE_OLD_CLASS_FILES_READING) }
    if ("X_USE_FAST_JAR_FILE_SYSTEM" in optionsMap) { arguments.useFastJarFileSystem = get(X_USE_FAST_JAR_FILE_SYSTEM) }
    if ("X_SUPPRESS_MISSING_BUILTINS_ERROR" in optionsMap) { arguments.suppressMissingBuiltinsError = get(X_SUPPRESS_MISSING_BUILTINS_ERROR) }
    if ("X_SCRIPT_RESOLVER_ENVIRONMENT" in optionsMap) { arguments.scriptResolverEnvironment = get(X_SCRIPT_RESOLVER_ENVIRONMENT) }
    if ("X_JAVA_SOURCE_ROOTS" in optionsMap) { arguments.javaSourceRoots = get(X_JAVA_SOURCE_ROOTS) }
    if ("X_JAVA_PACKAGE_PREFIX" in optionsMap) { arguments.javaPackagePrefix = get(X_JAVA_PACKAGE_PREFIX) }
    if ("X_JSR305" in optionsMap) { arguments.jsr305 = get(X_JSR305) }
    if ("X_NULLABILITY_ANNOTATIONS" in optionsMap) { arguments.nullabilityAnnotations = get(X_NULLABILITY_ANNOTATIONS) }
    if ("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS" in optionsMap) { arguments.supportCompatqualCheckerFrameworkAnnotations = get(X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS) }
    if ("X_JSPECIFY_ANNOTATIONS" in optionsMap) { arguments.jspecifyAnnotations = get(X_JSPECIFY_ANNOTATIONS) }
    if ("X_JVM_DEFAULT" in optionsMap) { arguments.jvmDefault = get(X_JVM_DEFAULT) }
    if ("X_DEFAULT_SCRIPT_EXTENSION" in optionsMap) { arguments.defaultScriptExtension = get(X_DEFAULT_SCRIPT_EXTENSION) }
    if ("X_DISABLE_STANDARD_SCRIPT" in optionsMap) { arguments.disableStandardScript = get(X_DISABLE_STANDARD_SCRIPT) }
    if ("X_GENERATE_STRICT_METADATA_VERSION" in optionsMap) { arguments.strictMetadataVersionSemantics = get(X_GENERATE_STRICT_METADATA_VERSION) }
    if ("X_SANITIZE_PARENTHESES" in optionsMap) { arguments.sanitizeParentheses = get(X_SANITIZE_PARENTHESES) }
    if ("X_FRIEND_PATHS" in optionsMap) { arguments.friendPaths = get(X_FRIEND_PATHS) }
    if ("X_ALLOW_NO_SOURCE_FILES" in optionsMap) { arguments.allowNoSourceFiles = get(X_ALLOW_NO_SOURCE_FILES) }
    if ("X_EMIT_JVM_TYPE_ANNOTATIONS" in optionsMap) { arguments.emitJvmTypeAnnotations = get(X_EMIT_JVM_TYPE_ANNOTATIONS) }
    if ("X_JVM_EXPOSE_BOXED" in optionsMap) { arguments.jvmExposeBoxed = get(X_JVM_EXPOSE_BOXED) }
    if ("X_STRING_CONCAT" in optionsMap) { arguments.stringConcat = get(X_STRING_CONCAT) }
    if ("X_JDK_RELEASE" in optionsMap) { arguments.jdkRelease = get(X_JDK_RELEASE) }
    if ("X_SAM_CONVERSIONS" in optionsMap) { arguments.samConversions = get(X_SAM_CONVERSIONS) }
    if ("X_LAMBDAS" in optionsMap) { arguments.lambdas = get(X_LAMBDAS) }
    if ("X_INDY_ALLOW_ANNOTATED_LAMBDAS" in optionsMap) { arguments.indyAllowAnnotatedLambdas = get(X_INDY_ALLOW_ANNOTATED_LAMBDAS) }
    if ("X_KLIB" in optionsMap) { arguments.klibLibraries = get(X_KLIB) }
    if ("X_NO_RESET_JAR_TIMESTAMPS" in optionsMap) { arguments.noResetJarTimestamps = get(X_NO_RESET_JAR_TIMESTAMPS) }
    if ("X_NO_UNIFIED_NULL_CHECKS" in optionsMap) { arguments.noUnifiedNullChecks = get(X_NO_UNIFIED_NULL_CHECKS) }
    if ("X_NO_SOURCE_DEBUG_EXTENSION" in optionsMap) { arguments.noSourceDebugExtension = get(X_NO_SOURCE_DEBUG_EXTENSION) }
    if ("X_PROFILE" in optionsMap) { arguments.profileCompilerCommand = get(X_PROFILE) }
    if ("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME" in optionsMap) { arguments.useOldInlineClassesManglingScheme = get(X_USE_14_INLINE_CLASSES_MANGLING_SCHEME) }
    if ("X_JVM_ENABLE_PREVIEW" in optionsMap) { arguments.enableJvmPreview = get(X_JVM_ENABLE_PREVIEW) }
    if ("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING" in optionsMap) { arguments.suppressDeprecatedJvmTargetWarning = get(X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING) }
    if ("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE" in optionsMap) { arguments.typeEnhancementImprovementsInStrictMode = get(X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE) }
    if ("X_SERIALIZE_IR" in optionsMap) { arguments.serializeIr = get(X_SERIALIZE_IR) }
    if ("X_VALIDATE_BYTECODE" in optionsMap) { arguments.validateBytecode = get(X_VALIDATE_BYTECODE) }
    if ("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL" in optionsMap) { arguments.enhanceTypeParameterTypesToDefNotNull = get(X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL) }
    if ("X_LINK_VIA_SIGNATURES" in optionsMap) { arguments.linkViaSignatures = get(X_LINK_VIA_SIGNATURES) }
    if ("X_DEBUG" in optionsMap) { arguments.enableDebugMode = get(X_DEBUG) }
    if ("X_ENHANCED_COROUTINES_DEBUGGING" in optionsMap) { arguments.enhancedCoroutinesDebugging = get(X_ENHANCED_COROUTINES_DEBUGGING) }
    if ("X_NO_NEW_JAVA_ANNOTATION_TARGETS" in optionsMap) { arguments.noNewJavaAnnotationTargets = get(X_NO_NEW_JAVA_ANNOTATION_TARGETS) }
    if ("X_VALUE_CLASSES" in optionsMap) { arguments.valueClasses = get(X_VALUE_CLASSES) }
    if ("X_USE_INLINE_SCOPES_NUMBERS" in optionsMap) { arguments.useInlineScopesNumbers = get(X_USE_INLINE_SCOPES_NUMBERS) }
    if ("X_USE_K2_KAPT" in optionsMap) { arguments.useK2Kapt = get(X_USE_K2_KAPT) }
    if ("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB" in optionsMap) { arguments.expectBuiltinsAsPartOfStdlib = get(X_COMPILE_BUILTINS_AS_PART_OF_STDLIB) }
    if ("X_OUTPUT_BUILTINS_METADATA" in optionsMap) { arguments.outputBuiltinsMetadata = get(X_OUTPUT_BUILTINS_METADATA) }
    if ("X_ANNOTATIONS_IN_METADATA" in optionsMap) { arguments.annotationsInMetadata = get(X_ANNOTATIONS_IN_METADATA) }
    if ("X_WHEN_EXPRESSIONS" in optionsMap) { arguments.whenExpressionsGeneration = get(X_WHEN_EXPRESSIONS) }
    return arguments
  }

  /**
   * Base class for [JvmCompilerArguments] options.
   *
   * @see get
   * @see set    
   */
  public class JvmCompilerArgument<V>(
    public val id: String,
  )

  public companion object {
    public val CLASSPATH: JvmCompilerArgument<String?> = JvmCompilerArgument("CLASSPATH")

    public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("INCLUDE_RUNTIME")

    public val JDK_HOME: JvmCompilerArgument<String?> = JvmCompilerArgument("JDK_HOME")

    public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

    public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")

    public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

    public val SCRIPT_TEMPLATES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("SCRIPT_TEMPLATES")

    public val MODULE_NAME: JvmCompilerArgument<String?> = JvmCompilerArgument("MODULE_NAME")

    public val JVM_TARGET: JvmCompilerArgument<JvmTarget?> = JvmCompilerArgument("JVM_TARGET")

    public val JAVA_PARAMETERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("JAVA_PARAMETERS")

    public val JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("JVM_DEFAULT")

    public val X_ALLOW_UNSTABLE_DEPENDENCIES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_UNSTABLE_DEPENDENCIES")

    public val X_ABI_STABILITY: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_ABI_STABILITY")

    public val X_IR_DO_NOT_CLEAR_BINDING_CONTEXT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT")

    public val X_BACKEND_THREADS: JvmCompilerArgument<Int> =
        JvmCompilerArgument("X_BACKEND_THREADS")

    public val X_MODULE_PATH: JvmCompilerArgument<String?> = JvmCompilerArgument("X_MODULE_PATH")

    public val X_ADD_MODULES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_ADD_MODULES")

    public val X_NO_CALL_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_CALL_ASSERTIONS")

    public val X_NO_RECEIVER_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_RECEIVER_ASSERTIONS")

    public val X_NO_PARAM_ASSERTIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_PARAM_ASSERTIONS")

    public val X_NO_OPTIMIZE: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_NO_OPTIMIZE")

    public val X_ASSERTIONS: JvmCompilerArgument<String?> = JvmCompilerArgument("X_ASSERTIONS")

    public val X_MULTIFILE_PARTS_INHERIT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_MULTIFILE_PARTS_INHERIT")

    public val X_USE_TYPE_TABLE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_TYPE_TABLE")

    public val X_USE_OLD_CLASS_FILES_READING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_OLD_CLASS_FILES_READING")

    public val X_USE_FAST_JAR_FILE_SYSTEM: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_USE_FAST_JAR_FILE_SYSTEM")

    public val X_SUPPRESS_MISSING_BUILTINS_ERROR: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_MISSING_BUILTINS_ERROR")

    public val X_SCRIPT_RESOLVER_ENVIRONMENT: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_SCRIPT_RESOLVER_ENVIRONMENT")

    public val X_JAVA_SOURCE_ROOTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_JAVA_SOURCE_ROOTS")

    public val X_JAVA_PACKAGE_PREFIX: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JAVA_PACKAGE_PREFIX")

    public val X_JSR305: JvmCompilerArgument<Array<String>?> = JvmCompilerArgument("X_JSR305")

    public val X_NULLABILITY_ANNOTATIONS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_NULLABILITY_ANNOTATIONS")

    public val X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS")

    public val X_JSPECIFY_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JSPECIFY_ANNOTATIONS")

    public val X_JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JVM_DEFAULT")

    public val X_DEFAULT_SCRIPT_EXTENSION: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_DEFAULT_SCRIPT_EXTENSION")

    public val X_DISABLE_STANDARD_SCRIPT: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_DISABLE_STANDARD_SCRIPT")

    public val X_GENERATE_STRICT_METADATA_VERSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_GENERATE_STRICT_METADATA_VERSION")

    public val X_SANITIZE_PARENTHESES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SANITIZE_PARENTHESES")

    public val X_FRIEND_PATHS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_FRIEND_PATHS")

    public val X_ALLOW_NO_SOURCE_FILES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_NO_SOURCE_FILES")

    public val X_EMIT_JVM_TYPE_ANNOTATIONS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_EMIT_JVM_TYPE_ANNOTATIONS")

    public val X_JVM_EXPOSE_BOXED: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_EXPOSE_BOXED")

    public val X_STRING_CONCAT: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_STRING_CONCAT")

    public val X_JDK_RELEASE: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JDK_RELEASE")

    public val X_SAM_CONVERSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_SAM_CONVERSIONS")

    public val X_LAMBDAS: JvmCompilerArgument<String?> = JvmCompilerArgument("X_LAMBDAS")

    public val X_INDY_ALLOW_ANNOTATED_LAMBDAS: JvmCompilerArgument<Boolean?> =
        JvmCompilerArgument("X_INDY_ALLOW_ANNOTATED_LAMBDAS")

    public val X_KLIB: JvmCompilerArgument<String?> = JvmCompilerArgument("X_KLIB")

    public val X_NO_RESET_JAR_TIMESTAMPS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_RESET_JAR_TIMESTAMPS")

    public val X_NO_UNIFIED_NULL_CHECKS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_UNIFIED_NULL_CHECKS")

    public val X_NO_SOURCE_DEBUG_EXTENSION: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_SOURCE_DEBUG_EXTENSION")

    public val X_PROFILE: JvmCompilerArgument<String?> = JvmCompilerArgument("X_PROFILE")

    public val X_USE_14_INLINE_CLASSES_MANGLING_SCHEME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME")

    public val X_JVM_ENABLE_PREVIEW: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_ENABLE_PREVIEW")

    public val X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING")

    public val X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE")

    public val X_SERIALIZE_IR: JvmCompilerArgument<String> = JvmCompilerArgument("X_SERIALIZE_IR")

    public val X_VALIDATE_BYTECODE: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALIDATE_BYTECODE")

    public val X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL")

    public val X_LINK_VIA_SIGNATURES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_LINK_VIA_SIGNATURES")

    public val X_DEBUG: JvmCompilerArgument<Boolean> = JvmCompilerArgument("X_DEBUG")

    public val X_ENHANCED_COROUTINES_DEBUGGING: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ENHANCED_COROUTINES_DEBUGGING")

    public val X_NO_NEW_JAVA_ANNOTATION_TARGETS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_NO_NEW_JAVA_ANNOTATION_TARGETS")

    public val X_VALUE_CLASSES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_VALUE_CLASSES")

    public val X_USE_INLINE_SCOPES_NUMBERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_USE_INLINE_SCOPES_NUMBERS")

    public val X_USE_K2_KAPT: JvmCompilerArgument<Boolean?> = JvmCompilerArgument("X_USE_K2_KAPT")

    public val X_COMPILE_BUILTINS_AS_PART_OF_STDLIB: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB")

    public val X_OUTPUT_BUILTINS_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_OUTPUT_BUILTINS_METADATA")

    public val X_ANNOTATIONS_IN_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ANNOTATIONS_IN_METADATA")

    public val X_WHEN_EXPRESSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_WHEN_EXPRESSIONS")
  }
}
