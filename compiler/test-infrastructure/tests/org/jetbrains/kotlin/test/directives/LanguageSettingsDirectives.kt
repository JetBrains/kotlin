/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object LanguageSettingsDirectives : SimpleDirectivesContainer() {
    val LANGUAGE by stringDirective(
        description = """
            List of enabled and disabled language features.
            Usage: // !LANGUAGE: +SomeFeature -OtherFeature warn:FeatureWithEarning
        """.trimIndent()
    )

    val API_VERSION by valueDirective<ApiVersion>(
        description = "Version of Kotlin API",
        parser = this::parseApiVersion
    )
    // --------------------- Analysis Flags ---------------------

    val OPT_IN by stringDirective(
        description = "List of opted in annotations (AnalysisFlags.optIn)"
    )

    val IGNORE_DATA_FLOW_IN_ASSERT by directive(
        description = "Enables corresponding analysis flag (AnalysisFlags.ignoreDataFlowInAssert)"
    )

    val ALLOW_RESULT_RETURN_TYPE by directive(
        description = "Allow using Result in return type position"
    )

    val EXPLICIT_API_MODE by enumDirective(
        "Configures explicit API mode (AnalysisFlags.explicitApiMode)",
        additionalParser = ExplicitApiMode.Companion::fromString
    )

    val ALLOW_KOTLIN_PACKAGE by directive(
        description = "Allow compiling code in package 'kotlin' and allow not requiring kotlin.stdlib in module-info (AnalysisFlags.allowKotlinPackage)"
    )

    // --------------------- Jvm Analysis Flags ---------------------

    val JVM_DEFAULT_MODE by enumDirective(
        description = "Configures corresponding analysis flag (JvmAnalysisFlags.jvmDefaultMode)",
        additionalParser = JvmDefaultMode.Companion::fromStringOrNull
    )

    val JDK_RELEASE by valueDirective(
        description = "Configures corresponding release flag",
        parser = Integer::valueOf
    )

    val INHERIT_MULTIFILE_PARTS by directive(
        description = "Enables corresponding analysis flag (JvmAnalysisFlags.inheritMultifileParts)"
    )

    val SANITIZE_PARENTHESES by directive(
        description = "Enables corresponding analysis flag (JvmAnalysisFlags.sanitizeParentheses)"
    )

    val ENABLE_JVM_PREVIEW by directive("Enable JVM preview features")
    val EMIT_JVM_TYPE_ANNOTATIONS by directive("Enable emitting jvm type annotations")
    val NO_OPTIMIZED_CALLABLE_REFERENCES by directive("Don't optimize callable references")
    val DISABLE_PARAM_ASSERTIONS by directive("Disable assertions on parameters")
    val DISABLE_CALL_ASSERTIONS by directive("Disable assertions on calls")
    val NO_UNIFIED_NULL_CHECKS by directive("No unified null checks")
    val PARAMETERS_METADATA by directive("Add parameters metadata for 1.8 reflection")
    val USE_TYPE_TABLE by directive("Use type table in metadata serialization")
    val NO_NEW_JAVA_ANNOTATION_TARGETS by directive("Do not generate Java annotation targets TYPE_USE/TYPE_PARAMETER for Kotlin annotation classes with Kotlin targets TYPE/TYPE_PARAMETER")
    val OLD_INNER_CLASSES_LOGIC by directive("Use old logic for generation of InnerClasses attributes")
    val LINK_VIA_SIGNATURES by directive("Use linkage via signatures instead of descriptors / FIR")
    val ENABLE_JVM_IR_INLINER by directive("Enable inlining on IR, instead of inlining on bytecode")

    // --------------------- Utils ---------------------

    fun parseApiVersion(versionString: String): ApiVersion = when (versionString) {
        "LATEST" -> ApiVersion.LATEST
        "LATEST_STABLE" -> ApiVersion.LATEST_STABLE
        else -> ApiVersion.parse(versionString) ?: error("Unknown API version: $versionString")
    }
}
