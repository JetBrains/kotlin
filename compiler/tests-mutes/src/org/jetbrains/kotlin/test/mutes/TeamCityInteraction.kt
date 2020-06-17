package org.jetbrains.kotlin.test.mutes

import com.fasterxml.jackson.module.kotlin.treeToValue
import khttp.responses.Response
import khttp.structures.authorization.Authorization

internal const val TAG = "[MUTED-BY-CSVFILE]"
private val buildServerUrl = System.getProperty("org.jetbrains.kotlin.test.mutes.teamcity.server.url")
private val headers = mapOf("Content-type" to "application/json", "Accept" to "application/json")
private val authUser = object : Authorization {
    override val header = "Authorization" to "Bearer ${System.getProperty("org.jetbrains.kotlin.test.mutes.teamcity.server.token")}"
}


internal fun getMutedTestsOnTeamcityForRootProject(): List<MuteTestJson> {
    val projectScopeId = Scope.COMMON.id

    val params = mapOf(
        "locator" to "project:(id:$projectScopeId)",
        "fields" to "mute(id,assignment(text),scope(project(id),buildTypes(buildType(id))),target(tests(test(name))),resolution)"
    )

    val response = khttp.get("$buildServerUrl/app/rest/mutes", headers, params, auth = authUser)
    checkResponseAndLog(response)

    val alreadyMutedTestsOnTeamCity = jsonObjectMapper.readTree(response.text).get("mute")
        .filter { jn -> jn.get("assignment").get("text").textValue().startsWith(TAG) }

    return alreadyMutedTestsOnTeamCity.mapNotNull { jsonObjectMapper.treeToValue<MuteTestJson>(it) }
}

internal fun uploadMutedTests(uploadMap: Map<String, MuteTestJson>) {
    for ((_, muteTestJson) in uploadMap) {
        val response = khttp.post(
            "$buildServerUrl/app/rest/mutes",
            headers = headers,
            data = jsonObjectMapper.writeValueAsString(muteTestJson),
            auth = authUser
        )
        checkResponseAndLog(response)
    }
}

internal fun deleteMutedTests(deleteMap: Map<String, MuteTestJson>) {
    for ((_, muteTestJson) in deleteMap) {
        val response = khttp.delete(
            "$buildServerUrl/app/rest/mutes/id:${muteTestJson.id}",
            headers = headers,
            auth = authUser
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
