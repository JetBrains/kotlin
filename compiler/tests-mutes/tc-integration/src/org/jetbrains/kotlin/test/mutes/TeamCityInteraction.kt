package org.jetbrains.kotlin.test.mutes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal const val TAG = "[MUTED-BY-CSVFILE]"
private val buildServerUrl = getMandatoryProperty("org.jetbrains.kotlin.test.mutes.teamcity.server.url")
private val token = getMandatoryProperty("org.jetbrains.kotlin.test.mutes.teamcity.server.token")
private val REQUEST_TIMEOUT = 120.toDuration(DurationUnit.SECONDS)

private val httpClient = HttpClient {
    install(HttpTimeout)
    install(ContentNegotiation) {
        jackson()
    }
}

private fun HttpRequestBuilder.applyCommonProperties() {
    headers {
        bearerAuth(token)
        append(HttpHeaders.Accept, "application/json")
    }
    timeout { requestTimeoutMillis = REQUEST_TIMEOUT.inWholeMilliseconds }
}

internal suspend fun getMutedTestsOnTeamcityForRootProject(rootScopeId: String): List<MuteTestJson> {
    val requestHref = "/app/rest/mutes"
    val requestParams = mapOf(
        "locator" to "project:(id:$rootScopeId)",
        "fields" to "mute(id,assignment(text),scope(project(id),buildTypes(buildType(id))),target(tests(test(name))),resolution),nextHref"
    )
    val jsonResponses = traverseAll(requestHref, requestParams)

    val alreadyMutedTestsOnTeamCity = jsonResponses.flatMap {
        it.get("mute").filter { jn -> jn.get("assignment").get("text")?.textValue().toString().startsWith(TAG) }
    }

    return alreadyMutedTestsOnTeamCity.mapNotNull { jsonObjectMapper.treeToValue<MuteTestJson>(it) }
}

private suspend fun traverseAll(
    @Suppress("SameParameterValue") requestHref: String,
    requestParams: Map<String, String>,
): List<JsonNode> {
    val jsonResponses = mutableListOf<JsonNode>()

    suspend fun request(url: String, params: Map<String, String>): String {
        val currentResponse = httpClient.get(url) {
            applyCommonProperties()
            url {
                for (entry in params) {
                    parameters.append(entry.key, entry.value)
                }
            }
        }
        checkResponseAndLog(currentResponse)
        val currentJsonResponse = jsonObjectMapper.readTree(currentResponse.bodyAsText())
        jsonResponses.add(currentJsonResponse)
        return currentJsonResponse.get("nextHref")?.textValue() ?: ""
    }

    var nextHref = request("$buildServerUrl$requestHref", requestParams)
    while (nextHref.isNotBlank()) {
        nextHref = request("$buildServerUrl$nextHref", emptyMap())
    }

    return jsonResponses
}

internal suspend fun uploadMutedTests(uploadMap: Map<String, MuteTestJson>) {
    for ((_, muteTestJson) in uploadMap) {
        val response = httpClient.post("$buildServerUrl/app/rest/mutes") {
            applyCommonProperties()
            contentType(ContentType.Application.Json)
            setBody(muteTestJson)
        }
        checkResponseAndLog(response)
    }
}

internal suspend fun deleteMutedTests(deleteMap: Map<String, MuteTestJson>) {
    for ((_, muteTestJson) in deleteMap) {
        val response = httpClient.delete("$buildServerUrl/app/rest/mutes/id:${muteTestJson.id}") {
            applyCommonProperties()
        }
        try {
            checkResponseAndLog(response)
        } catch (e: Exception) {
            System.err.println(e.message)
        }
    }
}

private suspend fun checkResponseAndLog(response: HttpResponse) {
    val isResponseBad = response.status.value !in 200..299
    if (isResponseBad) {
        throw Exception(
            "${response.request.method}-request to ${response.request.url} failed:\n" +
                    "${response.bodyAsText()}\n" +
                    "${response.request.content}"
        )
    }
}
