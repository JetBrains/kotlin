/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.api.v2.internal.compat

import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.API_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.PROGRESSIVE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_ALLOW_REIFIED_TYPE_IN_CATCH
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_ANNOTATION_TARGET_ALL
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_CHECK_PHASE_CONDITIONS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_CONTEXT_PARAMETERS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_CONTEXT_RECEIVERS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_CONTEXT_SENSITIVE_RESOLUTION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_DISABLE_DEFAULT_SCRIPTING_PLUGIN
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_DISABLE_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_DONT_WARN_ON_ERROR_SUPPRESSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_DUMP_FQNAME
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_EXPECT_ACTUAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_EXPLICIT_API
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_IGNORE_CONST_OPTIMIZATION_ERRORS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_INLINE_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_LIST_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_METADATA_KLIB
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_METADATA_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_MULTI_DOLLAR_INTERPOLATION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_NESTED_TYPE_ALIASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_NEW_INFERENCE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_NON_LOCAL_BREAK_CONTINUE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_NO_INLINE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_AFTER
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE_AFTER
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE_BEFORE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_PROFILE_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_RENDER_INTERNAL_DIAGNOSTIC_NAMES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_REPORT_ALL_WARNINGS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_REPORT_OUTPUT_FILES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_REPORT_PERF
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_RETURN_VALUE_CHECKER
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_SKIP_METADATA_VERSION_CHECK
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_SKIP_PRERELEASE_CHECK
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_SUPPRESS_VERSION_WARNINGS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_UNRESTRICTED_BUILDER_INFERENCE
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_USE_FIR_EXPERIMENTAL_CHECKERS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_USE_FIR_IC
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_USE_FIR_LT
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_VERIFY_IR_VISIBILITY
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.X_WHEN_GUARDS
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion._XDATA_FLOW_BASED_EXHAUSTIVENESS
import org.jetbrains.kotlin.buildtools.api.v2.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments as V2CommonCompilerArguments

internal open class CommonCompilerArgumentsV1Adapter : CommonToolArgumentsV1Adapter(),
    V2CommonCompilerArguments {
    private val optionsMap: MutableMap<String, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override operator fun <V> `get`(key: V2CommonCompilerArguments.CommonCompilerArgument<V>): V = optionsMap[key.id] as V

    override operator fun <V> `set`(key: V2CommonCompilerArguments.CommonCompilerArgument<V>, `value`: V) {
        optionsMap[key.id] = `value`
    }

    operator fun contains(key: V2CommonCompilerArguments.CommonCompilerArgument<*>): Boolean = key.id in optionsMap

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalCompilerArgument::class)
    override fun toArgumentStrings(): List<String> {
        val arguments = mutableListOf<String>()
        arguments.addAll(super.toArgumentStrings())
        if ("LANGUAGE_VERSION" in optionsMap) { arguments.add("-language-version=" + get(LANGUAGE_VERSION)?.stringValue) }
        if ("API_VERSION" in optionsMap) { arguments.add("-api-version=" + get(API_VERSION)?.stringValue) }
        if ("KOTLIN_HOME" in optionsMap) { arguments.add("-kotlin-home=" + get(KOTLIN_HOME)) }
        if ("PROGRESSIVE" in optionsMap) { arguments.add("-progressive=" + get(PROGRESSIVE)) }
        if ("OPT_IN" in optionsMap) { arguments.add("-opt-in=" + get(OPT_IN)) }
        if ("X_NO_INLINE" in optionsMap) { arguments.add("-Xno-inline=" + get(X_NO_INLINE)) }
        if ("X_SKIP_METADATA_VERSION_CHECK" in optionsMap) { arguments.add("-Xskip-metadata-version-check=" + get(X_SKIP_METADATA_VERSION_CHECK)) }
        if ("X_SKIP_PRERELEASE_CHECK" in optionsMap) { arguments.add("-Xskip-prerelease-check=" + get(X_SKIP_PRERELEASE_CHECK)) }
        if ("X_REPORT_OUTPUT_FILES" in optionsMap) { arguments.add("-Xreport-output-files=" + get(X_REPORT_OUTPUT_FILES)) }
        if ("X_NEW_INFERENCE" in optionsMap) { arguments.add("-Xnew-inference=" + get(X_NEW_INFERENCE)) }
        if ("X_INLINE_CLASSES" in optionsMap) { arguments.add("-Xinline-classes=" + get(X_INLINE_CLASSES)) }
        if ("X_REPORT_PERF" in optionsMap) { arguments.add("-Xreport-perf=" + get(X_REPORT_PERF)) }
        if ("X_DUMP_PERF" in optionsMap) { arguments.add("-Xdump-perf=" + get(X_DUMP_PERF)) }
        if ("X_METADATA_VERSION" in optionsMap) { arguments.add("-Xmetadata-version=" + get(X_METADATA_VERSION)) }
        if ("X_LIST_PHASES" in optionsMap) { arguments.add("-Xlist-phases=" + get(X_LIST_PHASES)) }
        if ("X_DISABLE_PHASES" in optionsMap) { arguments.add("-Xdisable-phases=" + get(X_DISABLE_PHASES)) }
        if ("X_VERBOSE_PHASES" in optionsMap) { arguments.add("-Xverbose-phases=" + get(X_VERBOSE_PHASES)) }
        if ("X_PHASES_TO_DUMP_BEFORE" in optionsMap) { arguments.add("-Xphases-to-dump-before=" + get(X_PHASES_TO_DUMP_BEFORE)) }
        if ("X_PHASES_TO_DUMP_AFTER" in optionsMap) { arguments.add("-Xphases-to-dump-after=" + get(X_PHASES_TO_DUMP_AFTER)) }
        if ("X_PHASES_TO_DUMP" in optionsMap) { arguments.add("-Xphases-to-dump=" + get(X_PHASES_TO_DUMP)) }
        if ("X_DUMP_DIRECTORY" in optionsMap) { arguments.add("-Xdump-directory=" + get(X_DUMP_DIRECTORY)) }
        if ("X_DUMP_FQNAME" in optionsMap) { arguments.add("-Xdump-fqname=" + get(X_DUMP_FQNAME)) }
        if ("X_PHASES_TO_VALIDATE_BEFORE" in optionsMap) { arguments.add("-Xphases-to-validate-before=" + get(X_PHASES_TO_VALIDATE_BEFORE)) }
        if ("X_PHASES_TO_VALIDATE_AFTER" in optionsMap) { arguments.add("-Xphases-to-validate-after=" + get(X_PHASES_TO_VALIDATE_AFTER)) }
        if ("X_PHASES_TO_VALIDATE" in optionsMap) { arguments.add("-Xphases-to-validate=" + get(X_PHASES_TO_VALIDATE)) }
        if ("X_VERIFY_IR" in optionsMap) { arguments.add("-Xverify-ir=" + get(X_VERIFY_IR)) }
        if ("X_VERIFY_IR_VISIBILITY" in optionsMap) { arguments.add("-Xverify-ir-visibility=" + get(X_VERIFY_IR_VISIBILITY)) }
        if ("X_PROFILE_PHASES" in optionsMap) { arguments.add("-Xprofile-phases=" + get(X_PROFILE_PHASES)) }
        if ("X_CHECK_PHASE_CONDITIONS" in optionsMap) { arguments.add("-Xcheck-phase-conditions=" + get(X_CHECK_PHASE_CONDITIONS)) }
        if ("X_USE_FIR_EXPERIMENTAL_CHECKERS" in optionsMap) { arguments.add("-Xuse-fir-experimental-checkers=" + get(X_USE_FIR_EXPERIMENTAL_CHECKERS)) }
        if ("X_USE_FIR_IC" in optionsMap) { arguments.add("-Xuse-fir-ic=" + get(X_USE_FIR_IC)) }
        if ("X_USE_FIR_LT" in optionsMap) { arguments.add("-Xuse-fir-lt=" + get(X_USE_FIR_LT)) }
        if ("X_METADATA_KLIB" in optionsMap) { arguments.add("-Xmetadata-klib=" + get(X_METADATA_KLIB)) }
        if ("X_DISABLE_DEFAULT_SCRIPTING_PLUGIN" in optionsMap) { arguments.add("-Xdisable-default-scripting-plugin=" + get(X_DISABLE_DEFAULT_SCRIPTING_PLUGIN)) }
        if ("X_EXPLICIT_API" in optionsMap) { arguments.add("-Xexplicit-api=" + get(X_EXPLICIT_API).stringValue) }
        if ("X_RETURN_VALUE_CHECKER" in optionsMap) { arguments.add("-Xreturn-value-checker=" + get(X_RETURN_VALUE_CHECKER).stringValue) }
        if ("X_SUPPRESS_VERSION_WARNINGS" in optionsMap) { arguments.add("-Xsuppress-version-warnings=" + get(X_SUPPRESS_VERSION_WARNINGS)) }
        if ("X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR" in optionsMap) { arguments.add("-Xsuppress-api-version-greater-than-language-version-error=" + get(X_SUPPRESS_API_VERSION_GREATER_THAN_LANGUAGE_VERSION_ERROR)) }
        if ("X_EXPECT_ACTUAL_CLASSES" in optionsMap) { arguments.add("-Xexpect-actual-classes=" + get(X_EXPECT_ACTUAL_CLASSES)) }
        if ("X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY" in optionsMap) { arguments.add("-Xconsistent-data-class-copy-visibility=" + get(X_CONSISTENT_DATA_CLASS_COPY_VISIBILITY)) }
        if ("X_UNRESTRICTED_BUILDER_INFERENCE" in optionsMap) { arguments.add("-Xunrestricted-builder-inference=" + get(X_UNRESTRICTED_BUILDER_INFERENCE)) }
        if ("X_CONTEXT_RECEIVERS" in optionsMap) { arguments.add("-Xcontext-receivers=" + get(X_CONTEXT_RECEIVERS)) }
        if ("X_CONTEXT_PARAMETERS" in optionsMap) { arguments.add("-Xcontext-parameters=" + get(X_CONTEXT_PARAMETERS)) }
        if ("X_CONTEXT_SENSITIVE_RESOLUTION" in optionsMap) { arguments.add("-Xcontext-sensitive-resolution=" + get(X_CONTEXT_SENSITIVE_RESOLUTION)) }
        if ("X_NON_LOCAL_BREAK_CONTINUE" in optionsMap) { arguments.add("-Xnon-local-break-continue=" + get(X_NON_LOCAL_BREAK_CONTINUE)) }
        if ("_XDATA_FLOW_BASED_EXHAUSTIVENESS" in optionsMap) { arguments.add("--Xdata-flow-based-exhaustiveness=" + get(_XDATA_FLOW_BASED_EXHAUSTIVENESS)) }
        if ("X_MULTI_DOLLAR_INTERPOLATION" in optionsMap) { arguments.add("-Xmulti-dollar-interpolation=" + get(X_MULTI_DOLLAR_INTERPOLATION)) }
        if ("X_RENDER_INTERNAL_DIAGNOSTIC_NAMES" in optionsMap) { arguments.add("-Xrender-internal-diagnostic-names=" + get(X_RENDER_INTERNAL_DIAGNOSTIC_NAMES)) }
        if ("X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS" in optionsMap) { arguments.add("-Xallow-any-scripts-in-source-roots=" + get(X_ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)) }
        if ("X_REPORT_ALL_WARNINGS" in optionsMap) { arguments.add("-Xreport-all-warnings=" + get(X_REPORT_ALL_WARNINGS)) }
        if ("X_IGNORE_CONST_OPTIMIZATION_ERRORS" in optionsMap) { arguments.add("-Xignore-const-optimization-errors=" + get(X_IGNORE_CONST_OPTIMIZATION_ERRORS)) }
        if ("X_DONT_WARN_ON_ERROR_SUPPRESSION" in optionsMap) { arguments.add("-Xdont-warn-on-error-suppression=" + get(X_DONT_WARN_ON_ERROR_SUPPRESSION)) }
        if ("X_WHEN_GUARDS" in optionsMap) { arguments.add("-Xwhen-guards=" + get(X_WHEN_GUARDS)) }
        if ("X_NESTED_TYPE_ALIASES" in optionsMap) { arguments.add("-Xnested-type-aliases=" + get(X_NESTED_TYPE_ALIASES)) }
        if ("X_SUPPRESS_WARNING" in optionsMap) { arguments.add("-Xsuppress-warning=" + get(X_SUPPRESS_WARNING)) }
        if ("X_WARNING_LEVEL" in optionsMap) { arguments.add("-Xwarning-level=" + get(X_WARNING_LEVEL)) }
        if ("X_ANNOTATION_DEFAULT_TARGET" in optionsMap) { arguments.add("-Xannotation-default-target=" + get(X_ANNOTATION_DEFAULT_TARGET)) }
        if ("X_ANNOTATION_TARGET_ALL" in optionsMap) { arguments.add("-Xannotation-target-all=" + get(X_ANNOTATION_TARGET_ALL)) }
        if ("X_ALLOW_REIFIED_TYPE_IN_CATCH" in optionsMap) { arguments.add("-Xallow-reified-type-in-catch=" + get(X_ALLOW_REIFIED_TYPE_IN_CATCH)) }
        return arguments
    }
}
