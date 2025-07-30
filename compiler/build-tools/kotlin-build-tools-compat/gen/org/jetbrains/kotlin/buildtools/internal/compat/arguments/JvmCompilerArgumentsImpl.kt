// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
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
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments

internal class JvmCompilerArgumentsImpl : CommonCompilerArgumentsImpl(), JvmCompilerArguments {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = optionsMap[key.id] as V

  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: JvmCompilerArguments.JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V = optionsMap[key.id] as V

  public operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("DEPRECATION")
  override fun applyArgumentStrings(arguments: List<String>) {
    super.applyArgumentStrings(arguments)
    val compilerArgs: K2JVMCompilerArguments = parseCommandLineArguments(arguments)
    this[CLASSPATH] = compilerArgs.classpath
    this[INCLUDE_RUNTIME] = compilerArgs.includeRuntime
    this[JDK_HOME] = compilerArgs.jdkHome
    this[NO_JDK] = compilerArgs.noJdk
    this[NO_STDLIB] = compilerArgs.noStdlib
    this[NO_REFLECT] = compilerArgs.noReflect
    this[SCRIPT_TEMPLATES] = compilerArgs.scriptTemplates
    this[MODULE_NAME] = compilerArgs.moduleName
    this[JVM_TARGET] = compilerArgs.jvmTarget?.let { JvmTarget.valueOf(it) }
    this[JAVA_PARAMETERS] = compilerArgs.javaParameters
    this[JVM_DEFAULT] = compilerArgs.jvmDefaultStable
    this[X_ALLOW_UNSTABLE_DEPENDENCIES] = compilerArgs.allowUnstableDependencies
    this[X_ABI_STABILITY] = compilerArgs.abiStability
    this[X_IR_DO_NOT_CLEAR_BINDING_CONTEXT] = compilerArgs.doNotClearBindingContext
    this[X_BACKEND_THREADS] = compilerArgs.backendThreads.let { it.toInt() }
    this[X_MODULE_PATH] = compilerArgs.javaModulePath
    this[X_ADD_MODULES] = compilerArgs.additionalJavaModules
    this[X_NO_CALL_ASSERTIONS] = compilerArgs.noCallAssertions
    this[X_NO_RECEIVER_ASSERTIONS] = compilerArgs.noReceiverAssertions
    this[X_NO_PARAM_ASSERTIONS] = compilerArgs.noParamAssertions
    this[X_NO_OPTIMIZE] = compilerArgs.noOptimize
    this[X_ASSERTIONS] = compilerArgs.assertionsMode
    this[X_MULTIFILE_PARTS_INHERIT] = compilerArgs.inheritMultifileParts
    this[X_USE_TYPE_TABLE] = compilerArgs.useTypeTable
    this[X_USE_OLD_CLASS_FILES_READING] = compilerArgs.useOldClassFilesReading
    this[X_USE_FAST_JAR_FILE_SYSTEM] = compilerArgs.useFastJarFileSystem
    this[X_SUPPRESS_MISSING_BUILTINS_ERROR] = compilerArgs.suppressMissingBuiltinsError
    this[X_SCRIPT_RESOLVER_ENVIRONMENT] = compilerArgs.scriptResolverEnvironment
    this[X_JAVA_SOURCE_ROOTS] = compilerArgs.javaSourceRoots
    this[X_JAVA_PACKAGE_PREFIX] = compilerArgs.javaPackagePrefix
    this[X_JSR305] = compilerArgs.jsr305
    this[X_NULLABILITY_ANNOTATIONS] = compilerArgs.nullabilityAnnotations
    this[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = compilerArgs.supportCompatqualCheckerFrameworkAnnotations
    this[X_JSPECIFY_ANNOTATIONS] = compilerArgs.jspecifyAnnotations
    this[X_JVM_DEFAULT] = compilerArgs.jvmDefault
    this[X_DEFAULT_SCRIPT_EXTENSION] = compilerArgs.defaultScriptExtension
    this[X_DISABLE_STANDARD_SCRIPT] = compilerArgs.disableStandardScript
    this[X_GENERATE_STRICT_METADATA_VERSION] = compilerArgs.strictMetadataVersionSemantics
    this[X_SANITIZE_PARENTHESES] = compilerArgs.sanitizeParentheses
    this[X_FRIEND_PATHS] = compilerArgs.friendPaths
    this[X_ALLOW_NO_SOURCE_FILES] = compilerArgs.allowNoSourceFiles
    this[X_EMIT_JVM_TYPE_ANNOTATIONS] = compilerArgs.emitJvmTypeAnnotations
    this[X_JVM_EXPOSE_BOXED] = compilerArgs.jvmExposeBoxed
    this[X_STRING_CONCAT] = compilerArgs.stringConcat
    this[X_JDK_RELEASE] = compilerArgs.jdkRelease
    this[X_SAM_CONVERSIONS] = compilerArgs.samConversions
    this[X_LAMBDAS] = compilerArgs.lambdas
    this[X_INDY_ALLOW_ANNOTATED_LAMBDAS] = compilerArgs.indyAllowAnnotatedLambdas
    this[X_KLIB] = compilerArgs.klibLibraries
    this[X_NO_RESET_JAR_TIMESTAMPS] = compilerArgs.noResetJarTimestamps
    this[X_NO_UNIFIED_NULL_CHECKS] = compilerArgs.noUnifiedNullChecks
    this[X_NO_SOURCE_DEBUG_EXTENSION] = compilerArgs.noSourceDebugExtension
    this[X_PROFILE] = compilerArgs.profileCompilerCommand
    this[X_USE_14_INLINE_CLASSES_MANGLING_SCHEME] = compilerArgs.useOldInlineClassesManglingScheme
    this[X_JVM_ENABLE_PREVIEW] = compilerArgs.enableJvmPreview
    this[X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING] = compilerArgs.suppressDeprecatedJvmTargetWarning
    this[X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE] = compilerArgs.typeEnhancementImprovementsInStrictMode
    this[X_SERIALIZE_IR] = compilerArgs.serializeIr
    this[X_VALIDATE_BYTECODE] = compilerArgs.validateBytecode
    this[X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL] = compilerArgs.enhanceTypeParameterTypesToDefNotNull
    this[X_LINK_VIA_SIGNATURES] = compilerArgs.linkViaSignatures
    this[X_DEBUG] = compilerArgs.enableDebugMode
    this[X_ENHANCED_COROUTINES_DEBUGGING] = compilerArgs.enhancedCoroutinesDebugging
    this[X_NO_NEW_JAVA_ANNOTATION_TARGETS] = compilerArgs.noNewJavaAnnotationTargets
    this[X_VALUE_CLASSES] = compilerArgs.valueClasses
    this[X_USE_INLINE_SCOPES_NUMBERS] = compilerArgs.useInlineScopesNumbers
    this[X_USE_K2_KAPT] = compilerArgs.useK2Kapt
    this[X_COMPILE_BUILTINS_AS_PART_OF_STDLIB] = compilerArgs.expectBuiltinsAsPartOfStdlib
    this[X_OUTPUT_BUILTINS_METADATA] = compilerArgs.outputBuiltinsMetadata
    this[X_ANNOTATIONS_IN_METADATA] = compilerArgs.annotationsInMetadata
    this[X_WHEN_EXPRESSIONS] = compilerArgs.whenExpressionsGeneration
  }

  @Suppress("DEPRECATION")
  @OptIn(ExperimentalCompilerArgument::class)
  override fun toArgumentStrings(): List<String> {
    val arguments = mutableListOf<String>()
    arguments.addAll(super.toArgumentStrings())
    if ("CLASSPATH" in optionsMap) { arguments.add("-classpath=" + get(CLASSPATH)) }
    if ("INCLUDE_RUNTIME" in optionsMap) { arguments.add("-include-runtime=" + get(INCLUDE_RUNTIME)) }
    if ("JDK_HOME" in optionsMap) { arguments.add("-jdk-home=" + get(JDK_HOME)) }
    if ("NO_JDK" in optionsMap) { arguments.add("-no-jdk=" + get(NO_JDK)) }
    if ("NO_STDLIB" in optionsMap) { arguments.add("-no-stdlib=" + get(NO_STDLIB)) }
    if ("NO_REFLECT" in optionsMap) { arguments.add("-no-reflect=" + get(NO_REFLECT)) }
    if ("SCRIPT_TEMPLATES" in optionsMap) { arguments.add("-script-templates=" + get(SCRIPT_TEMPLATES)) }
    if ("MODULE_NAME" in optionsMap) { arguments.add("-module-name=" + get(MODULE_NAME)) }
    if ("JVM_TARGET" in optionsMap) { arguments.add("-jvm-target=" + get(JVM_TARGET)?.stringValue) }
    if ("JAVA_PARAMETERS" in optionsMap) { arguments.add("-java-parameters=" + get(JAVA_PARAMETERS)) }
    if ("JVM_DEFAULT" in optionsMap) { arguments.add("-jvm-default=" + get(JVM_DEFAULT)) }
    if ("X_ALLOW_UNSTABLE_DEPENDENCIES" in optionsMap) { arguments.add("-Xallow-unstable-dependencies=" + get(X_ALLOW_UNSTABLE_DEPENDENCIES)) }
    if ("X_ABI_STABILITY" in optionsMap) { arguments.add("-Xabi-stability=" + get(X_ABI_STABILITY)) }
    if ("X_IR_DO_NOT_CLEAR_BINDING_CONTEXT" in optionsMap) { arguments.add("-Xir-do-not-clear-binding-context=" + get(X_IR_DO_NOT_CLEAR_BINDING_CONTEXT)) }
    if ("X_BACKEND_THREADS" in optionsMap) { arguments.add("-Xbackend-threads=" + get(X_BACKEND_THREADS).toString()) }
    if ("X_MODULE_PATH" in optionsMap) { arguments.add("-Xmodule-path=" + get(X_MODULE_PATH)) }
    if ("X_ADD_MODULES" in optionsMap) { arguments.add("-Xadd-modules=" + get(X_ADD_MODULES)) }
    if ("X_NO_CALL_ASSERTIONS" in optionsMap) { arguments.add("-Xno-call-assertions=" + get(X_NO_CALL_ASSERTIONS)) }
    if ("X_NO_RECEIVER_ASSERTIONS" in optionsMap) { arguments.add("-Xno-receiver-assertions=" + get(X_NO_RECEIVER_ASSERTIONS)) }
    if ("X_NO_PARAM_ASSERTIONS" in optionsMap) { arguments.add("-Xno-param-assertions=" + get(X_NO_PARAM_ASSERTIONS)) }
    if ("X_NO_OPTIMIZE" in optionsMap) { arguments.add("-Xno-optimize=" + get(X_NO_OPTIMIZE)) }
    if ("X_ASSERTIONS" in optionsMap) { arguments.add("-Xassertions=" + get(X_ASSERTIONS)) }
    if ("X_MULTIFILE_PARTS_INHERIT" in optionsMap) { arguments.add("-Xmultifile-parts-inherit=" + get(X_MULTIFILE_PARTS_INHERIT)) }
    if ("X_USE_TYPE_TABLE" in optionsMap) { arguments.add("-Xuse-type-table=" + get(X_USE_TYPE_TABLE)) }
    if ("X_USE_OLD_CLASS_FILES_READING" in optionsMap) { arguments.add("-Xuse-old-class-files-reading=" + get(X_USE_OLD_CLASS_FILES_READING)) }
    if ("X_USE_FAST_JAR_FILE_SYSTEM" in optionsMap) { arguments.add("-Xuse-fast-jar-file-system=" + get(X_USE_FAST_JAR_FILE_SYSTEM)) }
    if ("X_SUPPRESS_MISSING_BUILTINS_ERROR" in optionsMap) { arguments.add("-Xsuppress-missing-builtins-error=" + get(X_SUPPRESS_MISSING_BUILTINS_ERROR)) }
    if ("X_SCRIPT_RESOLVER_ENVIRONMENT" in optionsMap) { arguments.add("-Xscript-resolver-environment=" + get(X_SCRIPT_RESOLVER_ENVIRONMENT)) }
    if ("X_JAVA_SOURCE_ROOTS" in optionsMap) { arguments.add("-Xjava-source-roots=" + get(X_JAVA_SOURCE_ROOTS)) }
    if ("X_JAVA_PACKAGE_PREFIX" in optionsMap) { arguments.add("-Xjava-package-prefix=" + get(X_JAVA_PACKAGE_PREFIX)) }
    if ("X_JSR305" in optionsMap) { arguments.add("-Xjsr305=" + get(X_JSR305)) }
    if ("X_NULLABILITY_ANNOTATIONS" in optionsMap) { arguments.add("-Xnullability-annotations=" + get(X_NULLABILITY_ANNOTATIONS)) }
    if ("X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS" in optionsMap) { arguments.add("-Xsupport-compatqual-checker-framework-annotations=" + get(X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS)) }
    if ("X_JSPECIFY_ANNOTATIONS" in optionsMap) { arguments.add("-Xjspecify-annotations=" + get(X_JSPECIFY_ANNOTATIONS)) }
    if ("X_JVM_DEFAULT" in optionsMap) { arguments.add("-Xjvm-default=" + get(X_JVM_DEFAULT)) }
    if ("X_DEFAULT_SCRIPT_EXTENSION" in optionsMap) { arguments.add("-Xdefault-script-extension=" + get(X_DEFAULT_SCRIPT_EXTENSION)) }
    if ("X_DISABLE_STANDARD_SCRIPT" in optionsMap) { arguments.add("-Xdisable-standard-script=" + get(X_DISABLE_STANDARD_SCRIPT)) }
    if ("X_GENERATE_STRICT_METADATA_VERSION" in optionsMap) { arguments.add("-Xgenerate-strict-metadata-version=" + get(X_GENERATE_STRICT_METADATA_VERSION)) }
    if ("X_SANITIZE_PARENTHESES" in optionsMap) { arguments.add("-Xsanitize-parentheses=" + get(X_SANITIZE_PARENTHESES)) }
    if ("X_FRIEND_PATHS" in optionsMap) { arguments.add("-Xfriend-paths=" + get(X_FRIEND_PATHS)) }
    if ("X_ALLOW_NO_SOURCE_FILES" in optionsMap) { arguments.add("-Xallow-no-source-files=" + get(X_ALLOW_NO_SOURCE_FILES)) }
    if ("X_EMIT_JVM_TYPE_ANNOTATIONS" in optionsMap) { arguments.add("-Xemit-jvm-type-annotations=" + get(X_EMIT_JVM_TYPE_ANNOTATIONS)) }
    if ("X_JVM_EXPOSE_BOXED" in optionsMap) { arguments.add("-Xjvm-expose-boxed=" + get(X_JVM_EXPOSE_BOXED)) }
    if ("X_STRING_CONCAT" in optionsMap) { arguments.add("-Xstring-concat=" + get(X_STRING_CONCAT)) }
    if ("X_JDK_RELEASE" in optionsMap) { arguments.add("-Xjdk-release=" + get(X_JDK_RELEASE)) }
    if ("X_SAM_CONVERSIONS" in optionsMap) { arguments.add("-Xsam-conversions=" + get(X_SAM_CONVERSIONS)) }
    if ("X_LAMBDAS" in optionsMap) { arguments.add("-Xlambdas=" + get(X_LAMBDAS)) }
    if ("X_INDY_ALLOW_ANNOTATED_LAMBDAS" in optionsMap) { arguments.add("-Xindy-allow-annotated-lambdas=" + get(X_INDY_ALLOW_ANNOTATED_LAMBDAS)) }
    if ("X_KLIB" in optionsMap) { arguments.add("-Xklib=" + get(X_KLIB)) }
    if ("X_NO_RESET_JAR_TIMESTAMPS" in optionsMap) { arguments.add("-Xno-reset-jar-timestamps=" + get(X_NO_RESET_JAR_TIMESTAMPS)) }
    if ("X_NO_UNIFIED_NULL_CHECKS" in optionsMap) { arguments.add("-Xno-unified-null-checks=" + get(X_NO_UNIFIED_NULL_CHECKS)) }
    if ("X_NO_SOURCE_DEBUG_EXTENSION" in optionsMap) { arguments.add("-Xno-source-debug-extension=" + get(X_NO_SOURCE_DEBUG_EXTENSION)) }
    if ("X_PROFILE" in optionsMap) { arguments.add("-Xprofile=" + get(X_PROFILE)) }
    if ("X_USE_14_INLINE_CLASSES_MANGLING_SCHEME" in optionsMap) { arguments.add("-Xuse-14-inline-classes-mangling-scheme=" + get(X_USE_14_INLINE_CLASSES_MANGLING_SCHEME)) }
    if ("X_JVM_ENABLE_PREVIEW" in optionsMap) { arguments.add("-Xjvm-enable-preview=" + get(X_JVM_ENABLE_PREVIEW)) }
    if ("X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING" in optionsMap) { arguments.add("-Xsuppress-deprecated-jvm-target-warning=" + get(X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING)) }
    if ("X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE" in optionsMap) { arguments.add("-Xtype-enhancement-improvements-strict-mode=" + get(X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE)) }
    if ("X_SERIALIZE_IR" in optionsMap) { arguments.add("-Xserialize-ir=" + get(X_SERIALIZE_IR)) }
    if ("X_VALIDATE_BYTECODE" in optionsMap) { arguments.add("-Xvalidate-bytecode=" + get(X_VALIDATE_BYTECODE)) }
    if ("X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL" in optionsMap) { arguments.add("-Xenhance-type-parameter-types-to-def-not-null=" + get(X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL)) }
    if ("X_LINK_VIA_SIGNATURES" in optionsMap) { arguments.add("-Xlink-via-signatures=" + get(X_LINK_VIA_SIGNATURES)) }
    if ("X_DEBUG" in optionsMap) { arguments.add("-Xdebug=" + get(X_DEBUG)) }
    if ("X_ENHANCED_COROUTINES_DEBUGGING" in optionsMap) { arguments.add("-Xenhanced-coroutines-debugging=" + get(X_ENHANCED_COROUTINES_DEBUGGING)) }
    if ("X_NO_NEW_JAVA_ANNOTATION_TARGETS" in optionsMap) { arguments.add("-Xno-new-java-annotation-targets=" + get(X_NO_NEW_JAVA_ANNOTATION_TARGETS)) }
    if ("X_VALUE_CLASSES" in optionsMap) { arguments.add("-Xvalue-classes=" + get(X_VALUE_CLASSES)) }
    if ("X_USE_INLINE_SCOPES_NUMBERS" in optionsMap) { arguments.add("-Xuse-inline-scopes-numbers=" + get(X_USE_INLINE_SCOPES_NUMBERS)) }
    if ("X_USE_K2_KAPT" in optionsMap) { arguments.add("-Xuse-k2-kapt=" + get(X_USE_K2_KAPT)) }
    if ("X_COMPILE_BUILTINS_AS_PART_OF_STDLIB" in optionsMap) { arguments.add("-Xcompile-builtins-as-part-of-stdlib=" + get(X_COMPILE_BUILTINS_AS_PART_OF_STDLIB)) }
    if ("X_OUTPUT_BUILTINS_METADATA" in optionsMap) { arguments.add("-Xoutput-builtins-metadata=" + get(X_OUTPUT_BUILTINS_METADATA)) }
    if ("X_ANNOTATIONS_IN_METADATA" in optionsMap) { arguments.add("-Xannotations-in-metadata=" + get(X_ANNOTATIONS_IN_METADATA)) }
    if ("X_WHEN_EXPRESSIONS" in optionsMap) { arguments.add("-Xwhen-expressions=" + get(X_WHEN_EXPRESSIONS)) }
    return arguments
  }

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
