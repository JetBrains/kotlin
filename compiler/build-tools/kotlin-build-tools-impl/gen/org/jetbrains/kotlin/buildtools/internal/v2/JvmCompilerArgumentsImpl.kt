package org.jetbrains.kotlin.buildtools.`internal`.v2

import kotlin.Any
import kotlin.Suppress
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.D
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.EXPRESSION
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.INCLUDE_RUNTIME
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.JAVA_PARAMETERS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.JDK_HOME
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.JVM_TARGET
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.NO_JDK
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.SCRIPT_TEMPLATES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XABI_STABILITY
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XADD_MODULES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XALLOW_NO_SOURCE_FILES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XALLOW_UNSTABLE_DEPENDENCIES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XANNOTATIONS_IN_METADATA
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XASSERTIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XBACKEND_THREADS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XBUILD_FILE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XCOMPILE_BUILTINS_AS_PART_OF_STDLIB
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XCOMPILE_JAVA
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XDEBUG
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XDEFAULT_SCRIPT_EXTENSION
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XDISABLE_STANDARD_SCRIPT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XEMIT_JVM_TYPE_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XENHANCED_COROUTINES_DEBUGGING
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XFRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XGENERATE_STRICT_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XINDY_ALLOW_ANNOTATED_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XIR_DO_NOT_CLEAR_BINDING_CONTEXT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XIR_INLINER
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJAVAC_ARGUMENTS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJAVA_PACKAGE_PREFIX
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJAVA_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJDK_RELEASE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJSPECIFY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJSR305
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJVM_ENABLE_PREVIEW
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XJVM_EXPOSE_BOXED
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XKLIB
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XLAMBDAS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XLINK_VIA_SIGNATURES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XMODULE_PATH
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XMULTIFILE_PARTS_INHERIT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_CALL_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_NEW_JAVA_ANNOTATION_TARGETS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_OPTIMIZE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_PARAM_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_RECEIVER_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_RESET_JAR_TIMESTAMPS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_SOURCE_DEBUG_EXTENSION
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNO_UNIFIED_NULL_CHECKS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XNULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XOUTPUT_BUILTINS_METADATA
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XPROFILE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSANITIZE_PARENTHESES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSERIALIZE_IR
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSTRING_CONCAT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSUPPRESS_DEPRECATED_JVM_TARGET_WARNING
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XSUPPRESS_MISSING_BUILTINS_ERROR
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XTYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_14_INLINE_CLASSES_MANGLING_SCHEME
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_FAST_JAR_FILE_SYSTEM
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_INLINE_SCOPES_NUMBERS
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_JAVAC
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_K2_KAPT
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_OLD_CLASS_FILES_READING
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XUSE_TYPE_TABLE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XVALIDATE_BYTECODE
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XVALUE_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments.Companion.XWHEN_EXPRESSIONS
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

public class JvmCompilerArgumentsImpl : CommonCompilerArgumentsImpl(), JvmCompilerArguments {
  private val optionsMap: MutableMap<JvmCompilerArguments.JvmCompilerArgument<*>, Any?> =
      mutableMapOf()

  @Suppress("UNCHECKED_CAST")
  override operator fun <V> `get`(key: JvmCompilerArguments.JvmCompilerArgument<V>): V = optionsMap[key] as V

  override operator fun <V> `set`(key: JvmCompilerArguments.JvmCompilerArgument<V>, `value`: V) {
    optionsMap[key] = `value`
  }

  @Suppress("DEPRECATION")
  public fun toCompilerArguments(arguments: K2JVMCompilerArguments = K2JVMCompilerArguments()): K2JVMCompilerArguments {
    if (D in optionsMap) { arguments.destination = get(D)?.toString() }
    if (CLASSPATH in optionsMap) { arguments.classpath = get(CLASSPATH) }
    if (INCLUDE_RUNTIME in optionsMap) { arguments.includeRuntime = get(INCLUDE_RUNTIME) }
    if (JDK_HOME in optionsMap) { arguments.jdkHome = get(JDK_HOME)?.toString() }
    if (NO_JDK in optionsMap) { arguments.noJdk = get(NO_JDK) }
    if (NO_STDLIB in optionsMap) { arguments.noStdlib = get(NO_STDLIB) }
    if (NO_REFLECT in optionsMap) { arguments.noReflect = get(NO_REFLECT) }
    if (EXPRESSION in optionsMap) { arguments.expression = get(EXPRESSION) }
    if (SCRIPT_TEMPLATES in optionsMap) { arguments.scriptTemplates = get(SCRIPT_TEMPLATES) }
    if (MODULE_NAME in optionsMap) { arguments.moduleName = get(MODULE_NAME) }
    if (JVM_TARGET in optionsMap) { arguments.jvmTarget = get(JVM_TARGET)?.value }
    if (JAVA_PARAMETERS in optionsMap) { arguments.javaParameters = get(JAVA_PARAMETERS) }
    if (JVM_DEFAULT in optionsMap) { arguments.jvmDefaultStable = get(JVM_DEFAULT) }
    if (XALLOW_UNSTABLE_DEPENDENCIES in optionsMap) { arguments.allowUnstableDependencies = get(XALLOW_UNSTABLE_DEPENDENCIES) }
    if (XABI_STABILITY in optionsMap) { arguments.abiStability = get(XABI_STABILITY) }
    if (XIR_DO_NOT_CLEAR_BINDING_CONTEXT in optionsMap) { arguments.doNotClearBindingContext = get(XIR_DO_NOT_CLEAR_BINDING_CONTEXT) }
    if (XBACKEND_THREADS in optionsMap) { arguments.backendThreads = get(XBACKEND_THREADS).toString() }
    if (XMODULE_PATH in optionsMap) { arguments.javaModulePath = get(XMODULE_PATH)?.toString() }
    if (XADD_MODULES in optionsMap) { arguments.additionalJavaModules = get(XADD_MODULES) }
    if (XNO_CALL_ASSERTIONS in optionsMap) { arguments.noCallAssertions = get(XNO_CALL_ASSERTIONS) }
    if (XNO_RECEIVER_ASSERTIONS in optionsMap) { arguments.noReceiverAssertions = get(XNO_RECEIVER_ASSERTIONS) }
    if (XNO_PARAM_ASSERTIONS in optionsMap) { arguments.noParamAssertions = get(XNO_PARAM_ASSERTIONS) }
    if (XNO_OPTIMIZE in optionsMap) { arguments.noOptimize = get(XNO_OPTIMIZE) }
    if (XASSERTIONS in optionsMap) { arguments.assertionsMode = get(XASSERTIONS) }
    if (XBUILD_FILE in optionsMap) { arguments.buildFile = get(XBUILD_FILE) }
    if (XMULTIFILE_PARTS_INHERIT in optionsMap) { arguments.inheritMultifileParts = get(XMULTIFILE_PARTS_INHERIT) }
    if (XUSE_TYPE_TABLE in optionsMap) { arguments.useTypeTable = get(XUSE_TYPE_TABLE) }
    if (XUSE_OLD_CLASS_FILES_READING in optionsMap) { arguments.useOldClassFilesReading = get(XUSE_OLD_CLASS_FILES_READING) }
    if (XUSE_FAST_JAR_FILE_SYSTEM in optionsMap) { arguments.useFastJarFileSystem = get(XUSE_FAST_JAR_FILE_SYSTEM) }
    if (XSUPPRESS_MISSING_BUILTINS_ERROR in optionsMap) { arguments.suppressMissingBuiltinsError = get(XSUPPRESS_MISSING_BUILTINS_ERROR) }
    if (XSCRIPT_RESOLVER_ENVIRONMENT in optionsMap) { arguments.scriptResolverEnvironment = get(XSCRIPT_RESOLVER_ENVIRONMENT) }
    if (XUSE_JAVAC in optionsMap) { arguments.useJavac = get(XUSE_JAVAC) }
    if (XCOMPILE_JAVA in optionsMap) { arguments.compileJava = get(XCOMPILE_JAVA) }
    if (XJAVAC_ARGUMENTS in optionsMap) { arguments.javacArguments = get(XJAVAC_ARGUMENTS) }
    if (XJAVA_SOURCE_ROOTS in optionsMap) { arguments.javaSourceRoots = get(XJAVA_SOURCE_ROOTS)?.map{ it.toString() }?.toTypedArray() }
    if (XJAVA_PACKAGE_PREFIX in optionsMap) { arguments.javaPackagePrefix = get(XJAVA_PACKAGE_PREFIX) }
    if (XJSR305 in optionsMap) { arguments.jsr305 = get(XJSR305) }
    if (XNULLABILITY_ANNOTATIONS in optionsMap) { arguments.nullabilityAnnotations = get(XNULLABILITY_ANNOTATIONS) }
    if (XSUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS in optionsMap) { arguments.supportCompatqualCheckerFrameworkAnnotations = get(XSUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS) }
    if (XJSPECIFY_ANNOTATIONS in optionsMap) { arguments.jspecifyAnnotations = get(XJSPECIFY_ANNOTATIONS) }
    if (XJVM_DEFAULT in optionsMap) { arguments.jvmDefault = get(XJVM_DEFAULT) }
    if (XDEFAULT_SCRIPT_EXTENSION in optionsMap) { arguments.defaultScriptExtension = get(XDEFAULT_SCRIPT_EXTENSION) }
    if (XDISABLE_STANDARD_SCRIPT in optionsMap) { arguments.disableStandardScript = get(XDISABLE_STANDARD_SCRIPT) }
    if (XGENERATE_STRICT_METADATA_VERSION in optionsMap) { arguments.strictMetadataVersionSemantics = get(XGENERATE_STRICT_METADATA_VERSION) }
    if (XSANITIZE_PARENTHESES in optionsMap) { arguments.sanitizeParentheses = get(XSANITIZE_PARENTHESES) }
    if (XFRIEND_PATHS in optionsMap) { arguments.friendPaths = get(XFRIEND_PATHS)?.map{ it.toString() }?.toTypedArray() }
    if (XALLOW_NO_SOURCE_FILES in optionsMap) { arguments.allowNoSourceFiles = get(XALLOW_NO_SOURCE_FILES) }
    if (XEMIT_JVM_TYPE_ANNOTATIONS in optionsMap) { arguments.emitJvmTypeAnnotations = get(XEMIT_JVM_TYPE_ANNOTATIONS) }
    if (XJVM_EXPOSE_BOXED in optionsMap) { arguments.jvmExposeBoxed = get(XJVM_EXPOSE_BOXED) }
    if (XSTRING_CONCAT in optionsMap) { arguments.stringConcat = get(XSTRING_CONCAT) }
    if (XJDK_RELEASE in optionsMap) { arguments.jdkRelease = get(XJDK_RELEASE) }
    if (XSAM_CONVERSIONS in optionsMap) { arguments.samConversions = get(XSAM_CONVERSIONS) }
    if (XLAMBDAS in optionsMap) { arguments.lambdas = get(XLAMBDAS) }
    if (XINDY_ALLOW_ANNOTATED_LAMBDAS in optionsMap) { arguments.indyAllowAnnotatedLambdas = get(XINDY_ALLOW_ANNOTATED_LAMBDAS) }
    if (XKLIB in optionsMap) { arguments.klibLibraries = get(XKLIB) }
    if (XNO_RESET_JAR_TIMESTAMPS in optionsMap) { arguments.noResetJarTimestamps = get(XNO_RESET_JAR_TIMESTAMPS) }
    if (XNO_UNIFIED_NULL_CHECKS in optionsMap) { arguments.noUnifiedNullChecks = get(XNO_UNIFIED_NULL_CHECKS) }
    if (XNO_SOURCE_DEBUG_EXTENSION in optionsMap) { arguments.noSourceDebugExtension = get(XNO_SOURCE_DEBUG_EXTENSION) }
    if (XPROFILE in optionsMap) { arguments.profileCompilerCommand = get(XPROFILE) }
    if (XUSE_14_INLINE_CLASSES_MANGLING_SCHEME in optionsMap) { arguments.useOldInlineClassesManglingScheme = get(XUSE_14_INLINE_CLASSES_MANGLING_SCHEME) }
    if (XJVM_ENABLE_PREVIEW in optionsMap) { arguments.enableJvmPreview = get(XJVM_ENABLE_PREVIEW) }
    if (XSUPPRESS_DEPRECATED_JVM_TARGET_WARNING in optionsMap) { arguments.suppressDeprecatedJvmTargetWarning = get(XSUPPRESS_DEPRECATED_JVM_TARGET_WARNING) }
    if (XTYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE in optionsMap) { arguments.typeEnhancementImprovementsInStrictMode = get(XTYPE_ENHANCEMENT_IMPROVEMENTS_STRICT_MODE) }
    if (XSERIALIZE_IR in optionsMap) { arguments.serializeIr = get(XSERIALIZE_IR) }
    if (XVALIDATE_BYTECODE in optionsMap) { arguments.validateBytecode = get(XVALIDATE_BYTECODE) }
    if (XENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL in optionsMap) { arguments.enhanceTypeParameterTypesToDefNotNull = get(XENHANCE_TYPE_PARAMETER_TYPES_TO_DEF_NOT_NULL) }
    if (XLINK_VIA_SIGNATURES in optionsMap) { arguments.linkViaSignatures = get(XLINK_VIA_SIGNATURES) }
    if (XDEBUG in optionsMap) { arguments.enableDebugMode = get(XDEBUG) }
    if (XENHANCED_COROUTINES_DEBUGGING in optionsMap) { arguments.enhancedCoroutinesDebugging = get(XENHANCED_COROUTINES_DEBUGGING) }
    if (XNO_NEW_JAVA_ANNOTATION_TARGETS in optionsMap) { arguments.noNewJavaAnnotationTargets = get(XNO_NEW_JAVA_ANNOTATION_TARGETS) }
    if (XVALUE_CLASSES in optionsMap) { arguments.valueClasses = get(XVALUE_CLASSES) }
    if (XIR_INLINER in optionsMap) { arguments.enableIrInliner = get(XIR_INLINER) }
    if (XUSE_INLINE_SCOPES_NUMBERS in optionsMap) { arguments.useInlineScopesNumbers = get(XUSE_INLINE_SCOPES_NUMBERS) }
    if (XUSE_K2_KAPT in optionsMap) { arguments.useK2Kapt = get(XUSE_K2_KAPT) }
    if (XCOMPILE_BUILTINS_AS_PART_OF_STDLIB in optionsMap) { arguments.expectBuiltinsAsPartOfStdlib = get(XCOMPILE_BUILTINS_AS_PART_OF_STDLIB) }
    if (XOUTPUT_BUILTINS_METADATA in optionsMap) { arguments.outputBuiltinsMetadata = get(XOUTPUT_BUILTINS_METADATA) }
    if (XANNOTATIONS_IN_METADATA in optionsMap) { arguments.annotationsInMetadata = get(XANNOTATIONS_IN_METADATA) }
    if (XWHEN_EXPRESSIONS in optionsMap) { arguments.whenExpressionsGeneration = get(XWHEN_EXPRESSIONS) }
    return arguments
  }
}
