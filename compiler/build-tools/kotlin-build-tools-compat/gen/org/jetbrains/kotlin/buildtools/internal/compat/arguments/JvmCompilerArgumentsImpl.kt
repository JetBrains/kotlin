// This file was generated automatically. See the README.md file
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.`internal`.compat.arguments

import java.lang.IllegalStateException
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.io.path.Path
import org.jetbrains.kotlin.buildtools.`internal`.compat.DeepCopyable
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.D
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.EXPRESSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.INCLUDE_RUNTIME
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.JAVA_PARAMETERS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.JDK_HOME
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.JVM_TARGET
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.NO_JDK
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.SCRIPT_TEMPLATES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ABI_STABILITY
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ADD_MODULES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ALLOW_NO_SOURCE_FILES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ALLOW_UNSTABLE_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ANNOTATIONS_IN_METADATA
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_BACKEND_THREADS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_BUILD_FILE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_COMPILE_BUILTINS_AS_PART_OF_STDLIB
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_COMPILE_JAVA
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_DEBUG
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_DEFAULT_SCRIPT_EXTENSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_DISABLE_STANDARD_SCRIPT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_EMIT_JVM_TYPE_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ENHANCED_COROUTINES_DEBUGGING
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_GENERATE_STRICT_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_INDY_ALLOW_ANNOTATED_LAMBDAS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_IR_DO_NOT_CLEAR_BINDING_CONTEXT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_IR_INLINER
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JAVAC_ARGUMENTS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JAVA_PACKAGE_PREFIX
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JAVA_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JDK_RELEASE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JSPECIFY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JSR305
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JVM_ENABLE_PREVIEW
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_JVM_EXPOSE_BOXED
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_KLIB
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_LAMBDAS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_LINK_VIA_SIGNATURES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_MODULE_PATH
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_MULTIFILE_PARTS_INHERIT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_CALL_ASSERTIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_NEW_JAVA_ANNOTATION_TARGETS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_OPTIMIZE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_PARAM_ASSERTIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_RECEIVER_ASSERTIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_RESET_JAR_TIMESTAMPS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_SOURCE_DEBUG_EXTENSION
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NO_UNIFIED_NULL_CHECKS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_OUTPUT_BUILTINS_METADATA
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_PROFILE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SANITIZE_PARENTHESES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SERIALIZE_IR
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_STRING_CONCAT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_SUPPRESS_MISSING_BUILTINS_ERROR
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_14_INLINE_CLASSES_MANGLING_SCHEME
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_FAST_JAR_FILE_SYSTEM
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_INLINE_SCOPES_NUMBERS
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_JAVAC
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_K2_KAPT
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_OLD_CLASS_FILES_READING
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_USE_TYPE_TABLE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_VALIDATE_BYTECODE
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_VALUE_CLASSES
import org.jetbrains.kotlin.buildtools.`internal`.compat.arguments.JvmCompilerArgumentsImpl.Companion.X_WHEN_EXPRESSIONS
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings as compilerToArgumentStrings
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KC_VERSION

internal class JvmCompilerArgumentsImpl() : CommonCompilerArgumentsImpl(), JvmCompilerArguments,
    JvmCompilerArguments.Builder, DeepCopyable<JvmCompilerArgumentsImpl> {
  private val optionsMap: MutableMap<String, Any?> = mutableMapOf()
  init {
    applyCompilerArguments(K2JVMCompilerArguments())
  }

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V {
    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
    return optionsMap[key.id] as V
  }

  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    val currentKotlinVersion = KotlinToolingVersion(KC_VERSION)
    if (key.availableSinceVersion > KotlinReleaseVersion(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)) {
      throw IllegalStateException("${key.id} is available only since ${key.availableSinceVersion}")
    }
    optionsMap[key.id] = `value`
  }

  override operator fun contains(key: JvmCompilerArguments.JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  @Suppress("UNCHECKED_CAST")
  public operator fun <V> `get`(key: JvmCompilerArgument<V>): V = optionsMap[key.id] as V

  private operator fun <V> `set`(key: JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key.id] = `value`
  }

  public operator fun contains(key: JvmCompilerArgument<*>): Boolean = key.id in optionsMap

  override fun deepCopy(): JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl().also { newArgs -> newArgs.applyArgumentStrings(toArgumentStrings()) }

  override fun build(): JvmCompilerArguments = deepCopy()

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2JVMCompilerArguments = K2JVMCompilerArguments()): K2JVMCompilerArguments {
    super.toCompilerArguments(arguments)
    val unknownArgs = optionsMap.keys.filter { it !in knownArguments }
    if (unknownArgs.isNotEmpty()) {
      throw IllegalStateException("Unknown arguments: ${unknownArgs.joinToString()}")
    }
    if (X_ABI_STABILITY in this) { arguments.abiStability = get(X_ABI_STABILITY)}
    if (X_ADD_MODULES in this) { arguments.additionalJavaModules = get(X_ADD_MODULES)}
    if (X_ALLOW_NO_SOURCE_FILES in this) { arguments.allowNoSourceFiles = get(X_ALLOW_NO_SOURCE_FILES)}
    if (X_ALLOW_UNSTABLE_DEPENDENCIES in this) { arguments.allowUnstableDependencies = get(X_ALLOW_UNSTABLE_DEPENDENCIES)}
    try { if (X_ANNOTATIONS_IN_METADATA in this) { arguments.annotationsInMetadata = get(X_ANNOTATIONS_IN_METADATA)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ANNOTATIONS_IN_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    if (X_ASSERTIONS in this) { arguments.assertionsMode = get(X_ASSERTIONS)}
    if (X_BACKEND_THREADS in this) { arguments.backendThreads = get(X_BACKEND_THREADS).toString()}
    if (X_BUILD_FILE in this) { arguments.buildFile = get(X_BUILD_FILE)}
    try { if (X_COMPILE_BUILTINS_AS_PART_OF_STDLIB in this) { arguments.setUsingReflection("expectBuiltinsAsPartOfStdlib", get(X_COMPILE_BUILTINS_AS_PART_OF_STDLIB))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_BUILTINS_AS_PART_OF_STDLIB. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20 and removed in 2.3.20""").initCause(e) }
    try { if (X_COMPILE_JAVA in this) { arguments.setUsingReflection("compileJava", get(X_COMPILE_JAVA))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_COMPILE_JAVA. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    if (X_DEBUG in this) { arguments.enableDebugMode = get(X_DEBUG)}
    if (X_DEFAULT_SCRIPT_EXTENSION in this) { arguments.defaultScriptExtension = get(X_DEFAULT_SCRIPT_EXTENSION)}
    if (X_DISABLE_STANDARD_SCRIPT in this) { arguments.disableStandardScript = get(X_DISABLE_STANDARD_SCRIPT)}
    if (X_EMIT_JVM_TYPE_ANNOTATIONS in this) { arguments.emitJvmTypeAnnotations = get(X_EMIT_JVM_TYPE_ANNOTATIONS)}
    if (X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL in this) { arguments.enhanceTypeParameterTypesToDefNotNull = get(X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL)}
    try { if (X_ENHANCED_COROUTINES_DEBUGGING in this) { arguments.enhancedCoroutinesDebugging = get(X_ENHANCED_COROUTINES_DEBUGGING)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_ENHANCED_COROUTINES_DEBUGGING. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    if (X_FRIEND_PATHS in this) { arguments.friendPaths = get(X_FRIEND_PATHS)}
    if (X_GENERATE_STRICT_METADATA_VERSION in this) { arguments.strictMetadataVersionSemantics = get(X_GENERATE_STRICT_METADATA_VERSION)}
    try { if (X_INDY_ALLOW_ANNOTATED_LAMBDAS in this) { arguments.indyAllowAnnotatedLambdas = get(X_INDY_ALLOW_ANNOTATED_LAMBDAS)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_INDY_ALLOW_ANNOTATED_LAMBDAS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    if (X_IR_DO_NOT_CLEAR_BINDING_CONTEXT in this) { arguments.doNotClearBindingContext = get(X_IR_DO_NOT_CLEAR_BINDING_CONTEXT)}
    try { if (X_IR_INLINER in this) { arguments.setUsingReflection("enableIrInliner", get(X_IR_INLINER))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_IR_INLINER. Current compiler version is: $KC_VERSION, but the argument was removed in 2.3.0""").initCause(e) }
    if (X_JAVA_PACKAGE_PREFIX in this) { arguments.javaPackagePrefix = get(X_JAVA_PACKAGE_PREFIX)}
    if (X_JAVA_SOURCE_ROOTS in this) { arguments.javaSourceRoots = get(X_JAVA_SOURCE_ROOTS)}
    try { if (X_JAVAC_ARGUMENTS in this) { arguments.setUsingReflection("javacArguments", get(X_JAVAC_ARGUMENTS))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JAVAC_ARGUMENTS. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    if (X_JDK_RELEASE in this) { arguments.jdkRelease = get(X_JDK_RELEASE)}
    if (X_JSPECIFY_ANNOTATIONS in this) { arguments.jspecifyAnnotations = get(X_JSPECIFY_ANNOTATIONS)}
    if (X_JSR305 in this) { arguments.jsr305 = get(X_JSR305)}
    if (X_JVM_DEFAULT in this) { arguments.jvmDefault = get(X_JVM_DEFAULT)}
    if (X_JVM_ENABLE_PREVIEW in this) { arguments.enableJvmPreview = get(X_JVM_ENABLE_PREVIEW)}
    try { if (X_JVM_EXPOSE_BOXED in this) { arguments.jvmExposeBoxed = get(X_JVM_EXPOSE_BOXED)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_JVM_EXPOSE_BOXED. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    if (X_KLIB in this) { arguments.klibLibraries = get(X_KLIB)}
    if (X_LAMBDAS in this) { arguments.lambdas = get(X_LAMBDAS)}
    if (X_LINK_VIA_SIGNATURES in this) { arguments.linkViaSignatures = get(X_LINK_VIA_SIGNATURES)}
    if (X_MODULE_PATH in this) { arguments.javaModulePath = get(X_MODULE_PATH)}
    if (X_MULTIFILE_PARTS_INHERIT in this) { arguments.inheritMultifileParts = get(X_MULTIFILE_PARTS_INHERIT)}
    if (X_NO_CALL_ASSERTIONS in this) { arguments.noCallAssertions = get(X_NO_CALL_ASSERTIONS)}
    if (X_NO_NEW_JAVA_ANNOTATION_TARGETS in this) { arguments.noNewJavaAnnotationTargets = get(X_NO_NEW_JAVA_ANNOTATION_TARGETS)}
    if (X_NO_OPTIMIZE in this) { arguments.noOptimize = get(X_NO_OPTIMIZE)}
    if (X_NO_PARAM_ASSERTIONS in this) { arguments.noParamAssertions = get(X_NO_PARAM_ASSERTIONS)}
    if (X_NO_RECEIVER_ASSERTIONS in this) { arguments.noReceiverAssertions = get(X_NO_RECEIVER_ASSERTIONS)}
    if (X_NO_RESET_JAR_TIMESTAMPS in this) { arguments.noResetJarTimestamps = get(X_NO_RESET_JAR_TIMESTAMPS)}
    if (X_NO_SOURCE_DEBUG_EXTENSION in this) { arguments.noSourceDebugExtension = get(X_NO_SOURCE_DEBUG_EXTENSION)}
    if (X_NO_UNIFIED_NULL_CHECKS in this) { arguments.noUnifiedNullChecks = get(X_NO_UNIFIED_NULL_CHECKS)}
    if (X_NULLABILITY_ANNOTATIONS in this) { arguments.nullabilityAnnotations = get(X_NULLABILITY_ANNOTATIONS)}
    try { if (X_OUTPUT_BUILTINS_METADATA in this) { arguments.outputBuiltinsMetadata = get(X_OUTPUT_BUILTINS_METADATA)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_OUTPUT_BUILTINS_METADATA. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.20""").initCause(e) }
    if (X_PROFILE in this) { try { arguments.profileCompilerCommand = get(X_PROFILE)?.toArgumentString() } catch(e: ClassCastException) { arguments.applyProfileCompilerCommand(get(X_PROFILE)) }}
    if (X_SAM_CONVERSIONS in this) { arguments.samConversions = get(X_SAM_CONVERSIONS)}
    if (X_SANITIZE_PARENTHESES in this) { arguments.sanitizeParentheses = get(X_SANITIZE_PARENTHESES)}
    if (X_SCRIPT_RESOLVER_ENVIRONMENT in this) { arguments.scriptResolverEnvironment = get(X_SCRIPT_RESOLVER_ENVIRONMENT)}
    try { if (X_SERIALIZE_IR in this) { arguments.setUsingReflection("serializeIr", get(X_SERIALIZE_IR))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_SERIALIZE_IR. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    if (X_STRING_CONCAT in this) { arguments.stringConcat = get(X_STRING_CONCAT)}
    if (X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS in this) { arguments.supportCompatqualCheckerFrameworkAnnotations = get(X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS)}
    if (X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING in this) { arguments.suppressDeprecatedJvmTargetWarning = get(X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING)}
    if (X_SUPPRESS_MISSING_BUILTINS_ERROR in this) { arguments.suppressMissingBuiltinsError = get(X_SUPPRESS_MISSING_BUILTINS_ERROR)}
    if (X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE in this) { arguments.typeEnhancementImprovementsInStrictMode = get(X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE)}
    if (X_USE_14_INLINE_CLASSES_MANGLING_SCHEME in this) { arguments.useOldInlineClassesManglingScheme = get(X_USE_14_INLINE_CLASSES_MANGLING_SCHEME)}
    if (X_USE_FAST_JAR_FILE_SYSTEM in this) { arguments.useFastJarFileSystem = get(X_USE_FAST_JAR_FILE_SYSTEM)}
    try { if (X_USE_INLINE_SCOPES_NUMBERS in this) { arguments.useInlineScopesNumbers = get(X_USE_INLINE_SCOPES_NUMBERS)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_INLINE_SCOPES_NUMBERS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.0.0""").initCause(e) }
    try { if (X_USE_JAVAC in this) { arguments.setUsingReflection("useJavac", get(X_USE_JAVAC))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_JAVAC. Current compiler version is: $KC_VERSION, but the argument was removed in 2.4.0""").initCause(e) }
    try { if (X_USE_K2_KAPT in this) { arguments.setUsingReflection("useK2Kapt", get(X_USE_K2_KAPT))} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_USE_K2_KAPT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.1.0 and removed in 2.3.0""").initCause(e) }
    if (X_USE_OLD_CLASS_FILES_READING in this) { arguments.useOldClassFilesReading = get(X_USE_OLD_CLASS_FILES_READING)}
    if (X_USE_TYPE_TABLE in this) { arguments.useTypeTable = get(X_USE_TYPE_TABLE)}
    if (X_VALIDATE_BYTECODE in this) { arguments.validateBytecode = get(X_VALIDATE_BYTECODE)}
    if (X_VALUE_CLASSES in this) { arguments.valueClasses = get(X_VALUE_CLASSES)}
    try { if (X_WHEN_EXPRESSIONS in this) { arguments.whenExpressionsGeneration = get(X_WHEN_EXPRESSIONS)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: X_WHEN_EXPRESSIONS. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.20""").initCause(e) }
    if (CLASSPATH in this) { arguments.classpath = get(CLASSPATH)}
    if (D in this) { arguments.destination = get(D)}
    if (EXPRESSION in this) { arguments.expression = get(EXPRESSION)}
    if (INCLUDE_RUNTIME in this) { arguments.includeRuntime = get(INCLUDE_RUNTIME)}
    if (JAVA_PARAMETERS in this) { arguments.javaParameters = get(JAVA_PARAMETERS)}
    if (JDK_HOME in this) { try { arguments.jdkHome = get(JDK_HOME)?.absolutePathStringOrThrow() } catch(e: ClassCastException) { arguments.applyJdkHome(get(JDK_HOME)) }}
    try { if (JVM_DEFAULT in this) { arguments.jvmDefaultStable = get(JVM_DEFAULT)} } catch (e: NoSuchMethodError) { throw IllegalStateException("""Compiler parameter not recognized: JVM_DEFAULT. Current compiler version is: $KC_VERSION, but the argument was introduced in 2.2.0""").initCause(e) }
    if (JVM_TARGET in this) { arguments.jvmTarget = get(JVM_TARGET)?.stringValue}
    if (MODULE_NAME in this) { arguments.moduleName = get(MODULE_NAME)}
    if (NO_JDK in this) { arguments.noJdk = get(NO_JDK)}
    if (NO_REFLECT in this) { arguments.noReflect = get(NO_REFLECT)}
    if (NO_STDLIB in this) { arguments.noStdlib = get(NO_STDLIB)}
    if (SCRIPT_TEMPLATES in this) { arguments.scriptTemplates = get(SCRIPT_TEMPLATES)}
    arguments.internalArguments = parseCommandLineArguments<K2JVMCompilerArguments>(internalArguments.toList()).internalArguments
    return arguments
  }

  @Suppress("DEPRECATION")
  public fun applyCompilerArguments(arguments: K2JVMCompilerArguments) {
    super.applyCompilerArguments(arguments)
    try { this[X_ABI_STABILITY] = arguments.abiStability } catch (_: NoSuchMethodError) {  }
    try { this[X_ADD_MODULES] = arguments.additionalJavaModules } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_NO_SOURCE_FILES] = arguments.allowNoSourceFiles } catch (_: NoSuchMethodError) {  }
    try { this[X_ALLOW_UNSTABLE_DEPENDENCIES] = arguments.allowUnstableDependencies } catch (_: NoSuchMethodError) {  }
    try { this[X_ANNOTATIONS_IN_METADATA] = arguments.annotationsInMetadata } catch (_: NoSuchMethodError) {  }
    try { this[X_ASSERTIONS] = arguments.assertionsMode } catch (_: NoSuchMethodError) {  }
    try { this[X_BACKEND_THREADS] = arguments.backendThreads.let { it.toInt() } } catch (_: NoSuchMethodError) {  }
    try { this[X_BUILD_FILE] = arguments.buildFile } catch (_: NoSuchMethodError) {  }
    try { this[X_COMPILE_BUILTINS_AS_PART_OF_STDLIB] = arguments.getUsingReflection("expectBuiltinsAsPartOfStdlib") } catch (_: NoSuchMethodError) {  }
    try { this[X_COMPILE_JAVA] = arguments.getUsingReflection("compileJava") } catch (_: NoSuchMethodError) {  }
    try { this[X_DEBUG] = arguments.enableDebugMode } catch (_: NoSuchMethodError) {  }
    try { this[X_DEFAULT_SCRIPT_EXTENSION] = arguments.defaultScriptExtension } catch (_: NoSuchMethodError) {  }
    try { this[X_DISABLE_STANDARD_SCRIPT] = arguments.disableStandardScript } catch (_: NoSuchMethodError) {  }
    try { this[X_EMIT_JVM_TYPE_ANNOTATIONS] = arguments.emitJvmTypeAnnotations } catch (_: NoSuchMethodError) {  }
    try { this[X_ENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL] = arguments.enhanceTypeParameterTypesToDefNotNull } catch (_: NoSuchMethodError) {  }
    try { this[X_ENHANCED_COROUTINES_DEBUGGING] = arguments.enhancedCoroutinesDebugging } catch (_: NoSuchMethodError) {  }
    try { this[X_FRIEND_PATHS] = arguments.friendPaths } catch (_: NoSuchMethodError) {  }
    try { this[X_GENERATE_STRICT_METADATA_VERSION] = arguments.strictMetadataVersionSemantics } catch (_: NoSuchMethodError) {  }
    try { this[X_INDY_ALLOW_ANNOTATED_LAMBDAS] = arguments.indyAllowAnnotatedLambdas } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_DO_NOT_CLEAR_BINDING_CONTEXT] = arguments.doNotClearBindingContext } catch (_: NoSuchMethodError) {  }
    try { this[X_IR_INLINER] = arguments.getUsingReflection("enableIrInliner") } catch (_: NoSuchMethodError) {  }
    try { this[X_JAVA_PACKAGE_PREFIX] = arguments.javaPackagePrefix } catch (_: NoSuchMethodError) {  }
    try { this[X_JAVA_SOURCE_ROOTS] = arguments.javaSourceRoots } catch (_: NoSuchMethodError) {  }
    try { this[X_JAVAC_ARGUMENTS] = arguments.getUsingReflection("javacArguments") } catch (_: NoSuchMethodError) {  }
    try { this[X_JDK_RELEASE] = arguments.jdkRelease } catch (_: NoSuchMethodError) {  }
    try { this[X_JSPECIFY_ANNOTATIONS] = arguments.jspecifyAnnotations } catch (_: NoSuchMethodError) {  }
    try { this[X_JSR305] = arguments.jsr305 } catch (_: NoSuchMethodError) {  }
    try { this[X_JVM_DEFAULT] = arguments.jvmDefault } catch (_: NoSuchMethodError) {  }
    try { this[X_JVM_ENABLE_PREVIEW] = arguments.enableJvmPreview } catch (_: NoSuchMethodError) {  }
    try { this[X_JVM_EXPOSE_BOXED] = arguments.jvmExposeBoxed } catch (_: NoSuchMethodError) {  }
    try { this[X_KLIB] = arguments.klibLibraries } catch (_: NoSuchMethodError) {  }
    try { this[X_LAMBDAS] = arguments.lambdas } catch (_: NoSuchMethodError) {  }
    try { this[X_LINK_VIA_SIGNATURES] = arguments.linkViaSignatures } catch (_: NoSuchMethodError) {  }
    try { this[X_MODULE_PATH] = arguments.javaModulePath } catch (_: NoSuchMethodError) {  }
    try { this[X_MULTIFILE_PARTS_INHERIT] = arguments.inheritMultifileParts } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_CALL_ASSERTIONS] = arguments.noCallAssertions } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_NEW_JAVA_ANNOTATION_TARGETS] = arguments.noNewJavaAnnotationTargets } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_OPTIMIZE] = arguments.noOptimize } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_PARAM_ASSERTIONS] = arguments.noParamAssertions } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_RECEIVER_ASSERTIONS] = arguments.noReceiverAssertions } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_RESET_JAR_TIMESTAMPS] = arguments.noResetJarTimestamps } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_SOURCE_DEBUG_EXTENSION] = arguments.noSourceDebugExtension } catch (_: NoSuchMethodError) {  }
    try { this[X_NO_UNIFIED_NULL_CHECKS] = arguments.noUnifiedNullChecks } catch (_: NoSuchMethodError) {  }
    try { this[X_NULLABILITY_ANNOTATIONS] = arguments.nullabilityAnnotations } catch (_: NoSuchMethodError) {  }
    try { this[X_OUTPUT_BUILTINS_METADATA] = arguments.outputBuiltinsMetadata } catch (_: NoSuchMethodError) {  }
    try { try { this[X_PROFILE] = arguments.profileCompilerCommand?.toXprofile() } catch (e: ClassCastException) { applyProfileCompilerCommand(this[X_PROFILE], arguments) } } catch (_: NoSuchMethodError) {  }
    try { this[X_SAM_CONVERSIONS] = arguments.samConversions } catch (_: NoSuchMethodError) {  }
    try { this[X_SANITIZE_PARENTHESES] = arguments.sanitizeParentheses } catch (_: NoSuchMethodError) {  }
    try { this[X_SCRIPT_RESOLVER_ENVIRONMENT] = arguments.scriptResolverEnvironment } catch (_: NoSuchMethodError) {  }
    try { this[X_SERIALIZE_IR] = arguments.getUsingReflection("serializeIr") } catch (_: NoSuchMethodError) {  }
    try { this[X_STRING_CONCAT] = arguments.stringConcat } catch (_: NoSuchMethodError) {  }
    try { this[X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS] = arguments.supportCompatqualCheckerFrameworkAnnotations } catch (_: NoSuchMethodError) {  }
    try { this[X_SUPPRESS_DEPRECATED_JVM_TARGET_WARNING] = arguments.suppressDeprecatedJvmTargetWarning } catch (_: NoSuchMethodError) {  }
    try { this[X_SUPPRESS_MISSING_BUILTINS_ERROR] = arguments.suppressMissingBuiltinsError } catch (_: NoSuchMethodError) {  }
    try { this[X_TYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE] = arguments.typeEnhancementImprovementsInStrictMode } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_14_INLINE_CLASSES_MANGLING_SCHEME] = arguments.useOldInlineClassesManglingScheme } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_FAST_JAR_FILE_SYSTEM] = arguments.useFastJarFileSystem } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_INLINE_SCOPES_NUMBERS] = arguments.useInlineScopesNumbers } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_JAVAC] = arguments.getUsingReflection("useJavac") } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_K2_KAPT] = arguments.getUsingReflection("useK2Kapt") } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_OLD_CLASS_FILES_READING] = arguments.useOldClassFilesReading } catch (_: NoSuchMethodError) {  }
    try { this[X_USE_TYPE_TABLE] = arguments.useTypeTable } catch (_: NoSuchMethodError) {  }
    try { this[X_VALIDATE_BYTECODE] = arguments.validateBytecode } catch (_: NoSuchMethodError) {  }
    try { this[X_VALUE_CLASSES] = arguments.valueClasses } catch (_: NoSuchMethodError) {  }
    try { this[X_WHEN_EXPRESSIONS] = arguments.whenExpressionsGeneration } catch (_: NoSuchMethodError) {  }
    try { this[CLASSPATH] = arguments.classpath } catch (_: NoSuchMethodError) {  }
    try { this[D] = arguments.destination } catch (_: NoSuchMethodError) {  }
    try { this[EXPRESSION] = arguments.expression } catch (_: NoSuchMethodError) {  }
    try { this[INCLUDE_RUNTIME] = arguments.includeRuntime } catch (_: NoSuchMethodError) {  }
    try { this[JAVA_PARAMETERS] = arguments.javaParameters } catch (_: NoSuchMethodError) {  }
    try { try { this[JDK_HOME] = arguments.jdkHome?.let { Path(it) } } catch (e: ClassCastException) { applyJdkHome(this[JDK_HOME], arguments) } } catch (_: NoSuchMethodError) {  }
    try { this[JVM_DEFAULT] = arguments.jvmDefaultStable } catch (_: NoSuchMethodError) {  }
    try { this[JVM_TARGET] = arguments.jvmTarget?.let { JvmTarget.entries.firstOrNull { entry -> entry.stringValue == it } ?: throw CompilerArgumentsParseException("Unknown -jvm-target value: $it") } } catch (_: NoSuchMethodError) {  }
    try { this[MODULE_NAME] = arguments.moduleName } catch (_: NoSuchMethodError) {  }
    try { this[NO_JDK] = arguments.noJdk } catch (_: NoSuchMethodError) {  }
    try { this[NO_REFLECT] = arguments.noReflect } catch (_: NoSuchMethodError) {  }
    try { this[NO_STDLIB] = arguments.noStdlib } catch (_: NoSuchMethodError) {  }
    try { this[SCRIPT_TEMPLATES] = arguments.scriptTemplates } catch (_: NoSuchMethodError) {  }
    internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })
  }

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

    public val X_ABI_STABILITY: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_ABI_STABILITY")

    public val X_ADD_MODULES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_ADD_MODULES")

    public val X_ALLOW_NO_SOURCE_FILES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_NO_SOURCE_FILES")

    public val X_ALLOW_UNSTABLE_DEPENDENCIES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ALLOW_UNSTABLE_DEPENDENCIES")

    public val X_ANNOTATIONS_IN_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_ANNOTATIONS_IN_METADATA")

    public val X_ASSERTIONS: JvmCompilerArgument<String?> = JvmCompilerArgument("X_ASSERTIONS")

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

    public val X_FRIEND_PATHS: JvmCompilerArgument<Array<String>?> =
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

    public val X_JAVA_SOURCE_ROOTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_JAVA_SOURCE_ROOTS")

    public val X_JAVAC_ARGUMENTS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_JAVAC_ARGUMENTS")

    public val X_JDK_RELEASE: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JDK_RELEASE")

    public val X_JSPECIFY_ANNOTATIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_JSPECIFY_ANNOTATIONS")

    public val X_JSR305: JvmCompilerArgument<Array<String>?> = JvmCompilerArgument("X_JSR305")

    public val X_JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("X_JVM_DEFAULT")

    public val X_JVM_ENABLE_PREVIEW: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_ENABLE_PREVIEW")

    public val X_JVM_EXPOSE_BOXED: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_JVM_EXPOSE_BOXED")

    public val X_KLIB: JvmCompilerArgument<String?> = JvmCompilerArgument("X_KLIB")

    public val X_LAMBDAS: JvmCompilerArgument<String?> = JvmCompilerArgument("X_LAMBDAS")

    public val X_LINK_VIA_SIGNATURES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_LINK_VIA_SIGNATURES")

    public val X_MODULE_PATH: JvmCompilerArgument<String?> = JvmCompilerArgument("X_MODULE_PATH")

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

    public val X_NULLABILITY_ANNOTATIONS: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_NULLABILITY_ANNOTATIONS")

    public val X_OUTPUT_BUILTINS_METADATA: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_OUTPUT_BUILTINS_METADATA")

    public val X_PROFILE: JvmCompilerArgument<ProfileCompilerCommand?> =
        JvmCompilerArgument("X_PROFILE")

    public val X_SAM_CONVERSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_SAM_CONVERSIONS")

    public val X_SANITIZE_PARENTHESES: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("X_SANITIZE_PARENTHESES")

    public val X_SCRIPT_RESOLVER_ENVIRONMENT: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("X_SCRIPT_RESOLVER_ENVIRONMENT")

    public val X_SERIALIZE_IR: JvmCompilerArgument<String> = JvmCompilerArgument("X_SERIALIZE_IR")

    public val X_STRING_CONCAT: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_STRING_CONCAT")

    public val X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS: JvmCompilerArgument<String?> =
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

    public val X_WHEN_EXPRESSIONS: JvmCompilerArgument<String?> =
        JvmCompilerArgument("X_WHEN_EXPRESSIONS")

    public val CLASSPATH: JvmCompilerArgument<String?> = JvmCompilerArgument("CLASSPATH")

    public val D: JvmCompilerArgument<String?> = JvmCompilerArgument("D")

    public val EXPRESSION: JvmCompilerArgument<String?> = JvmCompilerArgument("EXPRESSION")

    public val INCLUDE_RUNTIME: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("INCLUDE_RUNTIME")

    public val JAVA_PARAMETERS: JvmCompilerArgument<Boolean> =
        JvmCompilerArgument("JAVA_PARAMETERS")

    public val JDK_HOME: JvmCompilerArgument<java.nio.`file`.Path?> =
        JvmCompilerArgument("JDK_HOME")

    public val JVM_DEFAULT: JvmCompilerArgument<String?> = JvmCompilerArgument("JVM_DEFAULT")

    public val JVM_TARGET: JvmCompilerArgument<JvmTarget?> = JvmCompilerArgument("JVM_TARGET")

    public val MODULE_NAME: JvmCompilerArgument<String?> = JvmCompilerArgument("MODULE_NAME")

    public val NO_JDK: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_JDK")

    public val NO_REFLECT: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_REFLECT")

    public val NO_STDLIB: JvmCompilerArgument<Boolean> = JvmCompilerArgument("NO_STDLIB")

    public val SCRIPT_TEMPLATES: JvmCompilerArgument<Array<String>?> =
        JvmCompilerArgument("SCRIPT_TEMPLATES")
  }
}
