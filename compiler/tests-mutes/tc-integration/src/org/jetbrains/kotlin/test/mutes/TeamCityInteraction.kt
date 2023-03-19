package org.jetbrains.kotlin.test.mutes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import khttp.responses.Response
import khttp.structures.authorization.Authorization

internal const val TAG = "[MUTED-BY-CSVFILE]"
private val buildServerUrl = getMandatoryProperty("org.jetbrains.kotlin.test.mutes.teamcity.server.url")
private val headers = mapOf("Content-type" to "application/json", "Accept" to "application/json")
private val authUser = object : Authorization {
    override val header = "Authorization" to "Bearer ${getMandatoryProperty("org.jetbrains.kotlin.test.mutes.teamcity.server.token")}"
}
private const val REQUEST_TIMEOUT_SEC = 120.0


internal fun getMutedTestsOnTeamcityForRootProject(rootScopeId: String): List<MuteTestJson> {
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

private fun traverseAll(
    @Suppress("SameParameterValue") requestHref: String,
    requestParams: Map<String, String>
): List<JsonNode> {
    val jsonResponses = mutableListOf<JsonNode>()

    fun request(url: String, params: Map<String, String>): String {
        val currentResponse = khttp.get(url, headers, params, auth = authUser, timeout = REQUEST_TIMEOUT_SEC)
        checkResponseAndLog(currentResponse)
        val currentJsonResponse = jsonObjectMapper.readTree(currentResponse.text)
        jsonResponses.add(currentJsonResponse)
        return currentJsonResponse.get("nextHref")?.textValue() ?: ""
    }

    var nextHref = request("$buildServerUrl$requestHref", requestParams)
    while (nextHref.isNotBlank()) {
        nextHref = request("$buildServerUrl$nextHref", emptyMap())
    }

    return jsonResponses
}

internal fun uploadMutedTests(uploadMap: Map<String, MuteTestJson>) {
    for ((_, muteTestJson) in uploadMap) {
        val response = khttp.post(
            "$buildServerUrl/app/rest/mutes",
            headers = headers,
            data = jsonObjectMapper.writeValueAsString(muteTestJson),
            auth = authUser,
            timeout = REQUEST_TIMEOUT_SEC
        )
        checkResponseAndLog(response)
    }
}

internal fun deleteMutedTests(deleteMap: Map<String, MuteTestJson>) {
    for ((_, muteTestJson) in deleteMap) {
        val response = khttp.delete(
            "$buildServerUrl/app/rest/mutes/id:${muteTestJson.id}",
            headers = headers,
            auth = authUser,
            timeout = REQUEST_TIMEOUT_SEC
        )
        try {
            checkResponseAndLog(response)
        } catch (e: Exception) {
            System.err.println(e.message)
        }
    }
}

private fun checkResponseAndLog(response: Response) {
    val isResponseBad = response.connection.responseCode !in 200..299
    if (isResponseBad) {
        throw Exception(
            "${response.request.method}-request to ${response.request.url} failed:\n" +
                    "${response.text}\n" +
                    "${response.request.data ?: ""}"
        )
    }
}
