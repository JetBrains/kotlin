/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import kotlinx.serialization.json.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

val MISSING_DIAGNOSTIC_PATTERN = """- `(\w+)`: (\d+) files""".toRegex()

inline fun createConnection(url: String, setup: HttpURLConnection.() -> Unit): HttpURLConnection {
    return (URL(url).openConnection() as HttpURLConnection).apply {
        doOutput = true
        useCaches = false
        setup()
    }
}

fun Any?.encodeToJsonDynamically(): JsonElement {
    return when (this) {
        null -> JsonNull
        is Map<*, *> -> {
            JsonObject(map { (key, value) ->
                require(key is String) { "Keys must be strings" }
                key to value.encodeToJsonDynamically()
            }.toMap())
        }
        is List<*> -> {
            JsonArray(map { it.encodeToJsonDynamically() })
        }
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else -> error("Can't encode this object as JSON: $this")
    }
}

fun requestViaJson(
    url: String,
    headers: Map<String, String>,
    body: @kotlinx.serialization.Serializable Any? = null,
    configureConnection: HttpURLConnection.() -> Unit,
): String {
    val connection = createConnection(url) {
        for ((key, value) in headers) {
            setRequestProperty(key, value)
        }

        configureConnection()
    }

    if (body != null) {
        DataOutputStream(connection.outputStream).use {
            it.writeBytes(body.encodeToJsonDynamically().toString())
        }
    }

    return BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
        reader.readText()
    }
}

fun postJson(url: String, headers: Map<String, String>, body: @kotlinx.serialization.Serializable Any) =
    requestViaJson(url, headers, body) {
        requestMethod = "POST"
    }

fun getJson(url: String, headers: Map<String, String>) =
    requestViaJson(url, headers) {
        requestMethod = "GET"
    }

val YT_TOKEN by lazy {
    System.getenv("YT_TOKEN") ?: error("The `YT_TOKEN` environment has not been set. It's required to make YT API calls")
}

val API_HEADERS by lazy {
    mapOf(
        "Accept" to "application/json",
        "Authorization" to "Bearer $YT_TOKEN",
        "Content-Type" to "application/json",
    )
}

const val KOTLIN_PROJECT_ID = "22-68"

@Suppress("unused")
object Tags {
    const val K2 = "68-186397"
    const val K1_RED_K2_GREEN = "68-291983"
    const val K2_POTENTIAL_FEATURE = "68-284223"
    const val FIXED_IN_K2 = "68-169920"
    const val K2_COMPILER_CRASH = "68-320807"
    const val K2_RUNTIME_CRASH = "68-320989"
    const val K2_NAIVE_BOX_PASSES_SOMETIMES = "68-321017"
}

@Suppress("unused")
class IssueInfo(val id: String, val numberInProject: Long)

val MISSING_DIAGNOSTICS_UMBRELLA = IssueInfo("25-4537231", 59443)

val knownMissingDiagnostics = mapOf(
    "MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES" to IssueInfo("25-4536950", 59367),
    "SUBTYPING_BETWEEN_CONTEXT_RECEIVERS" to IssueInfo("25-4536951", 59368),
    "BUILDER_INFERENCE_STUB_RECEIVER" to IssueInfo("25-4536952", 59369),
    "JS_NAME_CLASH" to IssueInfo("25-4536953", 59370),
    "MISSING_DEPENDENCY_CLASS" to IssueInfo("25-4536954", 59371),
    "SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR" to IssueInfo("25-4536955", 59372),
    "INVISIBLE_MEMBER" to IssueInfo("25-4536956", 59373),
    "COMPARE_TO_TYPE_MISMATCH" to IssueInfo("25-4536957", 59374),
    "SUPER_CALL_FROM_PUBLIC_INLINE_ERROR" to IssueInfo("25-4536958", 59375),
    "TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR" to IssueInfo("25-4536960", 59376),
    "CALL_TO_JS_MODULE_WITHOUT_MODULE_SYSTEM" to IssueInfo("25-4536961", 59377),
    "FINITE_BOUNDS_VIOLATION" to IssueInfo("25-4536962", 59378),
    "MIXING_NAMED_AND_POSITIONED_ARGUMENTS" to IssueInfo("25-4536963", 59379),
    "ACCIDENTAL_OVERRIDE" to IssueInfo("25-4536964", 59380),
    "CALL_TO_JS_NON_MODULE_WITH_MODULE_SYSTEM" to IssueInfo("25-4536965", 59381),
    "PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL" to IssueInfo("25-4536966", 59382),
    "ENUM_ENTRY_SHOULD_BE_INITIALIZED" to IssueInfo("25-4536967", 59383),
    "DYNAMIC_NOT_ALLOWED" to IssueInfo("25-4536968", 59384),
    "NON_VARARG_SPREAD_ERROR" to IssueInfo("25-4536969", 59385),
    "CONSTANT_EXPECTED_TYPE_MISMATCH" to IssueInfo("25-4536970", 59386),
    "NO_CONSTRUCTOR" to IssueInfo("25-4536971", 59387),
    "JSCODE_ERROR" to IssueInfo("25-4536972", 59388),
    "AMBIGUOUS_LABEL" to IssueInfo("25-4536973", 59389),
    "BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION" to IssueInfo("25-4536974", 59390),
    "JS_BUILTIN_NAME_CLASH" to IssueInfo("25-4536975", 59391),
    "NAME_CONTAINS_ILLEGAL_CHARS" to IssueInfo("25-4536976", 59392),
    "TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED" to IssueInfo("25-4536977", 59393),
    "EXPECTED_PARAMETERS_NUMBER_MISMATCH" to IssueInfo("25-4536978", 59394),
    "PROGRESSIONS_CHANGING_RESOLVE_ERROR" to IssueInfo("25-4536979", 59395),
    "MODIFIER_LIST_NOT_ALLOWED" to IssueInfo("25-4536980", 59396),
    "RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS" to IssueInfo("25-4536981", 59397),
    "NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE" to IssueInfo("25-4536982", 59398),
    "JSCODE_NO_JAVASCRIPT_PRODUCED" to IssueInfo("25-4536983", 59399),
    "CANNOT_INFER_VISIBILITY" to IssueInfo("25-4536984", 59400),
    "ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE" to IssueInfo("25-4536985", 59401),
    "EXPANSIVE_INHERITANCE" to IssueInfo("25-4536986", 59402),
    "SUPER_CANT_BE_EXTENSION_RECEIVER" to IssueInfo("25-4536987", 59403),
    "EXPECT_TYPE_IN_WHEN_WITHOUT_ELSE" to IssueInfo("25-4536988", 59404),
    "INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING" to IssueInfo("25-4536989", 59405),
    "PROPERTY_DELEGATION_BY_DYNAMIC" to IssueInfo("25-4536990", 59406),
    "MISSING_CONSTRUCTOR_KEYWORD" to IssueInfo("25-4536991", 59407),
    "MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES" to IssueInfo("25-4536992", 59408),
    "DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE" to IssueInfo("25-4536993", 59409),
    "TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE" to IssueInfo("25-4536994", 59410),
    "ENUM_CLASS_CONSTRUCTOR_CALL" to IssueInfo("25-4536995", 59411),
    "EXPECTED_PARAMETER_TYPE_MISMATCH" to IssueInfo("25-4536996", 59412),
    "VALUE_CLASS_CANNOT_HAVE_CONTEXT_RECEIVERS" to IssueInfo("25-4536997", 59413),
    "PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE_ERROR" to IssueInfo("25-4536998", 59414),
    "DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR" to IssueInfo("25-4537000", 59415),
    "EXTERNAL_INTERFACE_AS_REIFIED_TYPE_ARGUMENT" to IssueInfo("25-4537001", 59416),
    "CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE" to IssueInfo("25-4537002", 59417),
    "DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE" to IssueInfo("25-4537003", 59418),
    "MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE" to IssueInfo("25-4537004", 59419),
    "ABBREVIATED_NOTHING_PROPERTY_TYPE" to IssueInfo("25-4537005", 59420),
    "CONTEXT_RECEIVERS_WITH_BACKING_FIELD" to IssueInfo("25-4537006", 59421),
    "NON_SOURCE_ANNOTATION_ON_INLINED_LAMBDA_EXPRESSION" to IssueInfo("25-4537007", 59422),
    "FORBIDDEN_BINARY_MOD_AS_REM" to IssueInfo("25-4537008", 59423),
    "TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS" to IssueInfo("25-4537009", 59424),
    "JS_FAKE_NAME_CLASH" to IssueInfo("25-4537010", 59425),
    "RECEIVER_TYPE_MISMATCH" to IssueInfo("25-4537011", 59426),
    "EQUALS_MISSING" to IssueInfo("25-4537012", 59427),
    "CONFLICTING_INHERITED_JVM_DECLARATIONS" to IssueInfo("25-4537013", 59428),
    "ABBREVIATED_NOTHING_RETURN_TYPE" to IssueInfo("25-4537014", 59429),
    "CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY" to IssueInfo("25-4537015", 59430),
    "EXPLICIT_BACKING_FIELDS_UNSUPPORTED" to IssueInfo("25-4537016", 59431),
    "INACCESSIBLE_OUTER_CLASS_EXPRESSION" to IssueInfo("25-4537017", 59432),
    "NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE" to IssueInfo("25-4537018", 59433),
    "DECLARATION_IN_ILLEGAL_CONTEXT" to IssueInfo("25-4537019", 59434),
    "JSCODE_ARGUMENT_SHOULD_BE_CONSTANT" to IssueInfo("25-4537020", 59435),
    "NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION" to IssueInfo("25-4537021", 59436),
    "UPPER_BOUND_VIOLATION_IN_CONSTRAINT" to IssueInfo("25-4537022", 59437),
    "OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES" to IssueInfo("25-4537023", 59438),
    "STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY" to IssueInfo("25-4537024", 59439),
)

val obsoleteIssues = listOf(
    "NON_VARARG_SPREAD_ERROR",
    "PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE_ERROR",
    "FORBIDDEN_BINARY_MOD_AS_REM",
    "TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS",
    "EXPLICIT_BACKING_FIELDS_UNSUPPORTED",
    "INACCESSIBLE_OUTER_CLASS_EXPRESSION",
    "NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION",
    "SUPER_CANT_BE_EXTENSION_RECEIVER",
)

fun collectMissingDiagnostics(): List<Pair<String, String>> {
    val projectDirectory = File(System.getProperty("user.dir"))
    val build = projectDirectory.child("compiler").child("k2-differences").child("build")
    val missingDiagnosticsText = build.child("k2-unimplemented-diagnostics.md").readText()

    return MISSING_DIAGNOSTIC_PATTERN.findAll(missingDiagnosticsText)
        .map { it.groupValues[1] to it.groupValues[2] }
        .toList()
}

object MissingK2Diagnostics {
    @JvmStatic
    fun main(args: Array<String>) {
        val missingDiagnostics = collectMissingDiagnostics()

        for ((name, filesCount) in missingDiagnostics) {
            if (name in knownMissingDiagnostics) {
                continue
            }

            val result = postJson(
                "https://youtrack.jetbrains.com/api/issues?fields=id,numberInProject",
                API_HEADERS,
                mapOf(
                    "project" to mapOf(
                        "id" to KOTLIN_PROJECT_ID,
                    ),
                    "summary" to "K2: Missing $name",
                    "tags" to listOf(
                        mapOf("id" to Tags.K1_RED_K2_GREEN),
                    ),
                    "description" to "This diagnostic is backed up by $filesCount tests, but is missing in K2 (see the reports in KT-58630). If this is the desired behavior, please, replace the `#K1redK2green` tag with `#k2-potential-feature`",
                ),
            )

            println(result)
        }
    }
}

object RedundantMissingK2Diagnostics {
    @JvmStatic
    fun main(args: Array<String>) {
        val missingDiagnostics = collectMissingDiagnostics().map { it.first }.toSet()
        var index = 0

        for ((key, value) in knownMissingDiagnostics) {
            if (key in obsoleteIssues) {
                continue
            }

            if (key !in missingDiagnostics) {
                index++
                println("$index: The $key diagnostic is not present in the list of missing anymore, we can probably close $value")
            }
        }
    }
}
