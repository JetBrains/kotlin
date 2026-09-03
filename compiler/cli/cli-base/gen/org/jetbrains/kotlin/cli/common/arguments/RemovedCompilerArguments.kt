/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.LanguageFeature

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// DO NOT MODIFY IT MANUALLY.

@Deprecated("This class exists solely to facilitate detailed error reporting.", level = DeprecationLevel.ERROR)
class RemovedCompilerArguments {
    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xcontext-receivers",
        description = "Enable experimental context receivers.",
        removedVersion = "2.5.0",
    )
    var contextReceivers: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xignore-const-optimization-errors",
        description = "Ignore all compilation exceptions while optimizing some constant expressions.",
        removedVersion = "2.5.0",
    )
    var ignoreConstOptimizationErrors: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xinline-classes",
        description = "Enable experimental inline classes.",
        removedVersion = "2.5.0",
    )
    @Enables(LanguageFeature.InlineClasses)
    var inlineClasses: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xintellij-plugin-root",
        valueDescription = "<path>",
        description = "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.",
        deprecatedVersion = "2.4.20",
        removedVersion = "2.5.0",
    )
    var intellijPluginRoot: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xnew-inference",
        description = "Enable the new experimental generic type inference algorithm.",
        removedVersion = "2.5.0",
    )
    @Enables(LanguageFeature.NewInference)
    @Enables(LanguageFeature.SamConversionPerArgument)
    @Enables(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
    @Enables(LanguageFeature.DisableCompatibilityModeForNewInference)
    var newInference: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xno-check-actual",
        description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.",
        removedVersion = "2.5.0",
    )
    var noCheckActual: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "This is temporary solution (see KT-63712) intended to be used only for stdlib build.",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xsuppress-api-version-greater-than-language-version-error",
        description = "Suppress error about API version greater than language version.",
        deprecatedVersion = "2.0.0",
        removedVersion = "2.5.0",
    )
    var suppressApiVersionGreaterThanLanguageVersionError: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "Use '-Xwarning-level=<WARNING_NAME>:disabled' instead (and the same for other warnings).",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xsuppress-warning",
        valueDescription = "<WARNING_NAME>",
        description = "Suppress specified warning module-wide.",
        deprecatedVersion = "2.2.0",
        removedVersion = "2.5.0",
    )
    var suppressedDiagnostics: Array<String> = emptyArray()
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xunrestricted-builder-inference",
        description = "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.",
        removedVersion = "2.5.0",
    )
    @Enables(LanguageFeature.UnrestrictedBuilderInference)
    var unrestrictedBuilderInference: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xuse-fir-experimental-checkers",
        description = "Enable experimental frontend IR checkers that are not yet ready for production.",
        deprecatedVersion = "2.2.20",
        removedVersion = "2.5.0",
    )
    var useFirExperimentalCheckers: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "Compiler flag -Xuse-k2 is no more supported. Compiler versions 2.0+ use K2 by default, unless the language version is set to 1.9 or earlier.",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xuse-k2",
        description = "Compile using the K2 compiler pipeline.",
        deprecatedVersion = "1.9.0",
        removedVersion = "2.2.0",
    )
    var useK2: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xverify-ir-nested-offsets",
        description = "Check that offsets of nested IR elements conform to offsets of their containers. Only has effect if '-Xverify-ir' is not 'none'.",
        removedVersion = "2.4.20",
    )
    var verifyIrNestedOffsets: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xverify-ir-visibility",
        description = "Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.",
        removedVersion = "2.4.20",
    )
    var verifyIrVisibility: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xcompile-builtins-as-part-of-stdlib",
        description = "Enable behaviour needed to compile builtins as part of JVM stdlib",
        removedVersion = "2.3.20",
    )
    var expectBuiltinsAsPartOfStdlib: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xcompile-java",
        description = "Reuse 'javac' analysis and compile Java source files.",
        removedVersion = "2.4.0",
    )
    var compileJava: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xenhance-type-parameter-types-to-def-not-null",
        description = "Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').",
        removedVersion = "2.5.0",
    )
    @Enables(LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated)
    var enhanceTypeParameterTypesToDefNotNull: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xir-do-not-clear-binding-context",
        description = "When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.",
        removedVersion = "2.5.0",
    )
    var doNotClearBindingContext: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xir-inliner",
        description = "Inline functions using the IR inliner instead of the bytecode inliner.",
        removedVersion = "2.3.0",
    )
    var enableIrInliner: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xjavac-arguments",
        valueDescription = "<option[,]>",
        description = "Java compiler arguments.",
        removedVersion = "2.4.0",
    )
    var javacArguments: Array<String> = emptyArray()
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xklib",
        valueDescription = "<path>",
        description = "Paths to cross-platform libraries in the .klib format.",
        delimiter = Argument.Delimiters.pathSeparator,
        removedVersion = "2.5.0",
    )
    var klibLibraries: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xlink-via-signatures",
        description = """Link JVM IR symbols via signatures instead of descriptors.
This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend.
This option is deprecated and will be deleted in future versions.
It has no effect when -language-version is 2.0 or higher.""",
        deprecatedVersion = "2.0.0",
        removedVersion = "2.5.0",
    )
    var linkViaSignatures: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xserialize-ir",
        valueDescription = "{none|inline|all}",
        description = "Save the IR to metadata (Experimental).",
        removedVersion = "2.4.0",
    )
    var serializeIr: String = "none"
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xsuppress-deprecated-jvm-target-warning",
        description = "Suppress warnings about deprecated JVM target versions.",
        deprecatedVersion = "1.7.20",
        removedVersion = "2.5.0",
    )
    var suppressDeprecatedJvmTargetWarning: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xtype-enhancement-improvements-strict-mode",
        description = """Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations,
including the ability to read type-use annotations from class files.
See KT-45671 for more details.""",
        removedVersion = "2.5.0",
    )
    @Enables(LanguageFeature.TypeEnhancementImprovementsInStrictMode)
    var typeEnhancementImprovementsInStrictMode: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xuse-javac",
        description = "Use javac for Java source and class file analysis.",
        removedVersion = "2.4.0",
    )
    var useJavac: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xuse-k2-kapt",
        description = "Enable the experimental support for K2 KAPT.",
        removedVersion = "2.3.0",
    )
    var useK2Kapt: Boolean? = null
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xvalue-classes",
        description = "Enable experimental value classes.",
        removedVersion = "2.4.20",
    )
    var valueClasses: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xfake-override-validator",
        description = "Enable the IR fake override validator.",
        removedVersion = "2.5.0",
    )
    var fakeOverrideValidator: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xklib-normalize-absolute-path",
        description = "Normalize absolute paths in klibs.",
        deprecatedVersion = "2.4.20",
        removedVersion = "2.5.0",
    )
    var normalizeAbsolutePath: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xtyped-arrays",
        description = "This option does nothing and is left for compatibility with the legacy backend.",
        deprecatedVersion = "2.1.0",
        removedVersion = "2.3.0",
    )
    var typedArrays: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "It is senseless to use with IR compiler. Only for compatibility.",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-output",
        valueDescription = "<filepath>",
        description = "",
        deprecatedVersion = "2.1.0",
        removedVersion = "2.2.0",
    )
    var outputFile: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "Use '-Xbinary=bundleId=<id>'.",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xbundle-id",
        valueDescription = "<id>",
        description = "Bundle ID to be set in the Info.plist file of the produced framework.",
        deprecatedVersion = "1.7.20",
        removedVersion = "2.5.0",
    )
    var bundleId: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xdestroy-runtime-mode",
        valueDescription = "<mode>",
        description = "When to destroy the runtime.",
        removedVersion = "2.5.0",
    )
    var destroyRuntimeMode: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "Light debug information is enabled by default for Darwin platforms. For other targets use '-Xadd-light-debug=enable' instead.",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xg0",
        description = "Add light debug information.",
        deprecatedVersion = "1.5.20",
        removedVersion = "2.4.20",
    )
    var lightDebugDeprecated: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xpurge-user-libs",
        deprecatedName = "--purge_user_libs",
        description = "Don't link unused libraries even if explicitly specified.",
        removedVersion = "2.5.0",
    )
    var purgeUserLibs: Boolean = false
        set(value) {
            field = value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-Xworker-exception-handling",
        valueDescription = "<mode>",
        description = "Unhandled exception processing in 'Worker.executeAfter'. Possible values: 'legacy' and 'use-hook'. The default value is 'legacy' and for '-memory-model experimental', the default value is 'use-hook'.",
        deprecatedVersion = "2.4.20",
        removedVersion = "2.5.0",
    )
    var workerExceptionHandling: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-library-version",
        shortName = "-lv",
        valueDescription = "<version>",
        description = "The library version.",
        deprecatedVersion = "2.0.20",
        removedVersion = "2.4.20",
    )
    var libraryVersion: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @all:Deprecated(
        message = "The dist no longer has any endorsed libraries.",
        level = DeprecationLevel.ERROR,
    )
    @Argument(
        value = "-no-endorsed-libs",
        description = "Don't link endorsed libraries from the dist automatically.",
        deprecatedVersion = "1.9.20",
        removedVersion = "2.4.20",
    )
    var noendorsedlibs: Boolean = false
        set(value) {
            field = value
        }

}
