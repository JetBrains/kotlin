/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.decodeStringToJsonTree
import kotlinx.serialization.serializer
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import org.jetbrains.kotlin.test.frontend.fir.differences.ContentTypes.JSON
import org.jetbrains.kotlin.test.frontend.fir.differences.ContentTypes.MULTIPART

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

fun request(
    url: String,
    headers: Map<String, String>,
    configureConnection: HttpURLConnection.() -> Unit,
    writeBody: ((DataOutputStream) -> Unit)? = null,
): String {
    val connection = createConnection(url) {
        for ((key, value) in headers) {
            setRequestProperty(key, value)
        }

        configureConnection()
    }

    if (writeBody != null) {
        DataOutputStream(connection.outputStream).use {
            writeBody(it)
        }
    }

    return BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
        reader.readText()
    }
}

fun requestViaJson(
    url: String,
    headers: Map<String, String>,
    body: @kotlinx.serialization.Serializable Any? = null,
    configureConnection: HttpURLConnection.() -> Unit,
) = request(
    url, headers,
    configureConnection = configureConnection,
    writeBody = when {
        body != null -> fun(it: DataOutputStream) {
            it.writeBytes(body.encodeToJsonDynamically().toString())
        }
        else -> null
    },
)

fun postJson(url: String, headers: Map<String, String>, body: @kotlinx.serialization.Serializable Any) =
    requestViaJson(url, headers, body) {
        requestMethod = "POST"
    }

fun getJson(url: String, headers: Map<String, String>) =
    requestViaJson(url, headers) {
        requestMethod = "GET"
    }

fun deleteJson(url: String, headers: Map<String, String>) =
    requestViaJson(url, headers) {
        requestMethod = "DELETE"
    }

private const val dashDash = "--"
private const val crlf = "\r\n"

fun uploadFiles(url: String, files: List<File>) =
    request(
        url,
        headers = mapOf(
            Headers.accept(JSON),
            Headers.authorization,
            Headers.contentType(MULTIPART),
            Headers.keepAlive,
        ),
        configureConnection = {
            requestMethod = "POST"
        },
        writeBody = {
            for (file in files) {
                val relativeName = file.name

                it.writeBytes(dashDash + MULTIPARY_BOUNDARY + crlf)
                it.writeBytes("Content-Disposition: form-data; name=\"upload\"; filename=\"$relativeName\"$crlf")
                it.writeBytes(crlf)
                it.write(file.readBytes())
                it.writeBytes(crlf)
            }

            it.writeBytes(dashDash + MULTIPARY_BOUNDARY + dashDash + crlf)
            it.flush()
        },
    )

val YT_TOKEN by lazy {
    System.getenv("YT_TOKEN") ?: error("The `YT_TOKEN` environment has not been set. It's required to make YT API calls")
}

object Headers {
    val authorization get() = "Authorization" to "Bearer $YT_TOKEN"
    fun accept(type: String) = "Accept" to type
    fun contentType(type: String) = "Content-Type" to type
    val keepAlive = "Connection" to "Keep-Alive"
}

const val MULTIPARY_BOUNDARY = "*****"

object ContentTypes {
    val JSON = "application/json"
    val MULTIPART = "multipart/form-data;boundary=$MULTIPARY_BOUNDARY"
}

val API_HEADERS by lazy {
    mapOf(
        Headers.accept(JSON),
        Headers.authorization,
        Headers.contentType(JSON),
    )
}

const val KOTLIN_PROJECT_ID = "22-68"

@Suppress("unused")
object Tags {
    const val K2 = "68-186397"
    const val K1_RED_K2_GREEN = "68-291983"
    const val K2_POTENTIAL_FEATURE = "68-284223"
    const val K2_POTENTIAL_BREAKING_CHANGE = "68-136106"
    const val FIXED_IN_K2 = "68-169920"
    const val K2_COMPILER_CRASH = "68-320807"
    const val K2_RUNTIME_CRASH = "68-320989"
    const val K2_NAIVE_BOX_PASSES_SOMETIMES = "68-321017"
}

fun extractDiagnosticsFrom(text: String): List<Pair<String, String>> {
    return MISSING_DIAGNOSTIC_PATTERN.findAll(text)
        .map { it.groupValues[1] to it.groupValues[2] }
        .toList()
}

fun collectMissingDiagnostics() = extractDiagnosticsFrom(PublishableArtifacts.k2UnimplementedDiagnostics.readText())

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

object ArbitraryK2Differences {
    @JvmStatic
    fun main(args: Array<String>) {
        val (disappearedPart, introducedPart) = PublishableArtifacts.containmentDiagnosticsStats.readText()
            .split(MOST_COMMON_REASONS_OF_BREAKING_CHANGES)
        val disappearedDiagnostics = extractDiagnosticsFrom(disappearedPart)
        val introducedDiagnostics = extractDiagnosticsFrom(introducedPart)

        for ((diagnostic, filesCount) in disappearedDiagnostics) {
//            val existing = knownDisappearedDiagnostics[diagnostic] ?: continue
//            existing.makeSubtaskOf(DISAPPEARED_DIAGNOSTICS_UMBRELLA.numberInProject)

            if (diagnostic in knownMissingDiagnostics || diagnostic in knownDisappearedDiagnostics) {
                continue
            }

            generateTicketFor(
                diagnostic, filesCount.toInt(),
                title = "disappeared",
                description = "this diagnostic was present in K1, but disappeared",
                listOf(Tags.K1_RED_K2_GREEN, Tags.K2),
            )
        }

        for ((diagnostic, filesCount) in introducedDiagnostics) {
//            val existing = knownIntroducedDiagnostics[diagnostic] ?: continue
//            existing.makeSubtaskOf(INTRODUCED_DIAGNOSTICS_UMBRELLA.numberInProject)

//            val disappearedIssue = knownDisappearedDiagnostics[diagnostic] ?: continue
//            existing.relateTo(disappearedIssue.numberInProject)

            if (diagnostic in knownMissingDiagnostics || diagnostic in knownIntroducedDiagnostics) {
                continue
            }

            generateTicketFor(
                diagnostic, filesCount.toInt(),
                title = "introduced",
                description = "this diagnostic was introduced in K2",
                listOf(Tags.K2_POTENTIAL_BREAKING_CHANGE, Tags.K2),
            )
        }
    }

    private fun generateTicketFor(
        diagnostic: String,
        filesCount: Int,
        title: String,
        description: String,
        tags: List<String>,
    ) {
        print("$diagnostic :: ")

        postJson(
            "https://youtrack.jetbrains.com/api/issues?fields=id,numberInProject",
            API_HEADERS,
            mapOf(
                "project" to mapOf(
                    "id" to KOTLIN_PROJECT_ID,
                ),
                "summary" to "K2: $title $diagnostic",
                "tags" to tags.map { mapOf("id" to it) },
                "description" to "There are $filesCount tests, where $description (see the reports in KT-58630).",
            ),
        ).also(::println)
    }
}

fun IssueInfo.runCommand(query: String) {
    try {
        postJson(
            "https://youtrack.jetbrains.com/api/commands",
            API_HEADERS,
            mapOf(
                "query" to query,
                "issues" to listOf(
                    mapOf(
                        "idReadable" to "KT-$numberInProject",
                    ),
                ),
            ),
        ).also(::println)
    } catch (e: IOException) {
        println(e.message)
    }
}

fun IssueInfo.makeSubtaskOf(parentNumberInProject: Long) = runCommand("subtask of KT-$parentNumberInProject")
fun IssueInfo.relateTo(anotherNumberInProject: Long) = runCommand("relates to KT-$anotherNumberInProject")

fun updateMissingDiagnosticsTags(diagnosticsStatistics: DiagnosticsStatistics) {
    status.doneSilently("Assigning #k2-compiler-crash to diagnostics with all corresponding box tests failing..")
    val disappearances = diagnosticsStatistics.extractDisappearances()

    val testsAlwaysCausingCompilationCrashes = disappearances.filter { (_, files) ->
        files.all { it.analogousK2RelativePath in knownFailingAdditionalBoxTests }
    }

    for ((diagnostic, files) in testsAlwaysCausingCompilationCrashes) {
        val missingIssue = knownMissingDiagnostics[diagnostic] ?: continue

        try {
            if (files.any { knownFailingAdditionalBoxTests[it.analogousK2RelativePath] == AdditionalTestFailureKind.RUNTIME }) {
                postJson(
                    "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/tags?fields=id,name",
                    API_HEADERS,
                    mapOf("id" to Tags.K2_RUNTIME_CRASH),
                ).also(::println)
            }

            if (files.any { knownFailingAdditionalBoxTests[it.analogousK2RelativePath] == AdditionalTestFailureKind.COMPILE_TIME }) {
                postJson(
                    "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/tags?fields=id,name",
                    API_HEADERS,
                    mapOf("id" to Tags.K2_COMPILER_CRASH),
                ).also(::println)
            }

        } catch (e: IOException) {
            println(e)
        }
    }

    status.doneSilently("Assigning #k2-naive-box-passes-sometimes to diagnostics with some corresponding box tests failing..")

    val testsSometimesCausingCompilationCrashes = disappearances.filter { (diagnostic, files) ->
        files.any { it.analogousK2RelativePath in knownFailingAdditionalBoxTests }
                && diagnostic !in testsAlwaysCausingCompilationCrashes
    }

    for ((diagnostic, files) in testsSometimesCausingCompilationCrashes) {
        val missingIssue = knownMissingDiagnostics[diagnostic] ?: continue

        val (failingBoxes, passingBoxes) = files.partition {
            it.analogousK2RelativePath in knownFailingAdditionalBoxTests
        }

        try {
            postJson(
                "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/tags?fields=id,name",
                API_HEADERS,
                mapOf("id" to Tags.K2_NAIVE_BOX_PASSES_SOMETIMES),
            ).also(::println)

            if (failingBoxes.any { knownFailingAdditionalBoxTests[it.analogousK2RelativePath] == AdditionalTestFailureKind.RUNTIME }) {
                postJson(
                    "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/tags?fields=id,name",
                    API_HEADERS,
                    mapOf("id" to Tags.K2_RUNTIME_CRASH),
                ).also(::println)
            }

            if (failingBoxes.any { knownFailingAdditionalBoxTests[it.analogousK2RelativePath] == AdditionalTestFailureKind.COMPILE_TIME }) {
                postJson(
                    "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/tags?fields=id,name",
                    API_HEADERS,
                    mapOf("id" to Tags.K2_COMPILER_CRASH),
                ).also(::println)
            }

            val commentsResult = getJson(
                "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/comments?fields=text",
                API_HEADERS,
            )

            if (WHEN_TURNED_INTO_BOX_TEST in commentsResult) {
                continue
            }

            val addCommentResult = postJson(
                "https://youtrack.jetbrains.com/api/issues/${missingIssue.id}/comments?fields=id,author(name),text",
                API_HEADERS,
                mapOf(
                    "text" to buildFailingPassingAdditionalTestsStatisticsMessage(failingBoxes, passingBoxes),
                ),
            )

            println(addCommentResult)
        } catch (e: IOException) {
            println(e)
        }
    }
}

fun IssueInfo.setDefaultMetadata() {
    val response = getJson(
        "https://youtrack.jetbrains.com/api/issues/$id?fields=customFields(name,value(name))",
        API_HEADERS,
    )

    @OptIn(InternalSerializationApi::class)
    val fields = Json.decodeStringToJsonTree<String>(Json.serializersModule.serializer(), response).cast<JsonObject>()
        .getChildAs<JsonArray>("customFields")
        .mapChildrenAs<JsonObject, _> { field ->
            val name = field.getChildAs<JsonPrimitive>("name").content
            name to field["value"]?.takeIf { it !is JsonNull }
        }.toMap()

    val oldState = fields["State"]?.cast<JsonObject?>()?.getChildAs<JsonPrimitive>("name")?.content
    val oldPriority = fields["Priority"]?.cast<JsonObject?>()?.getChildAs<JsonPrimitive>("name")?.content
    val oldTargetVersions = fields["Target versions"]?.cast<JsonArray?>()
        ?.takeIf { it.isNotEmpty() }
        ?.mapChildrenAs<JsonObject, _> { it.getChildAs<JsonPrimitive>("name").content }
    val oldSubsystems = fields["Subsystems"]?.cast<JsonArray?>()
        ?.takeIf { it.isNotEmpty() }
        ?.mapChildrenAs<JsonObject, _> { it.getChildAs<JsonPrimitive>("name").content }

    if (
        oldState != "Submitted" &&
        oldPriority != null &&
        oldTargetVersions != null &&
        oldSubsystems != null
    ) {
        return
    }

    val newState = oldState?.takeIf { it != "Submitted" } ?: NEW_ISSUE_STATE
    val newPriority = oldPriority ?: NEW_ISSUE_PRIORITY
    val newTargetVersions = oldTargetVersions ?: NEW_ISSUE_TARGET_VERSIONS
    val newSubsystems = oldSubsystems ?: NEW_ISSUE_SUBSYSTEMS

    try {
        postJson(
            "https://youtrack.jetbrains.com/api/issues/KT-$numberInProject?fields=customFields(name,value(name))",
            API_HEADERS,
            mapOf(
                buildIssueCustomFields(newState, newPriority, newTargetVersions, newSubsystems),
            ),
        ).also(::println)
    } catch (e: IOException) {
        println(e.message)
    }
}
