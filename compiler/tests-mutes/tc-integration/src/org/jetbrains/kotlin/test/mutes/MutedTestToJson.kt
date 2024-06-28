/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal val jsonObjectMapper = jacksonObjectMapper()

data class MuteTestJson(
    val id: Int,
    val assignment: JsonNode,
    val scope: JsonNode,
    val target: JsonNode,
    val resolution: JsonNode
)

internal fun createMuteTestJson(testName: String, description: String, scopeId: String): MuteTestJson {
    val assignmentJson = """{ "text" : "$TAG $description" }"""
    val scopeJson = """{"project":{"id":"$scopeId"}}"""
    val targetJson = """{ "tests" : { "test" : [ { "name" : "$testName" } ] } }"""
    val resolutionJson = """{ "type" : "manually" }"""

    return MuteTestJson(
        0,
        jsonObjectMapper.readTree(assignmentJson),
        jsonObjectMapper.readTree(scopeJson),
        jsonObjectMapper.readTree(targetJson),
        jsonObjectMapper.readTree(resolutionJson)
    )
}

internal fun filterMutedTestsByScope(muteTestJson: List<MuteTestJson>, scopeId: String): Map<String, MuteTestJson> {
    val filterCondition = { testJson: MuteTestJson ->
        testJson.scope.get("project")?.get("id")?.textValue() == scopeId
    }

    return muteTestJson.filter(filterCondition)
        .flatMap { mutedTestJson ->
            val testNames = mutedTestJson.target.get("tests").get("test").toList().map { it.get("name").textValue() }
            testNames.map { testName ->
                testName to mutedTestJson
            }
        }
        .toMap()
}

internal fun transformMutedTestsToJson(flakyTests: List<MutedTest>?, scopeId: String): Map<String, MuteTestJson> {
    val mutedMap = mutableMapOf<String, MuteTestJson>()
    if (flakyTests != null) {
        for (muted in flakyTests) {
            val testName = formatClassnameWithInnerClasses(muted.key)
            mutedMap[testName] = createMuteTestJson(testName, muted.issue ?: "", scopeId)
        }
    }
    return mutedMap
}

private fun formatClassnameWithInnerClasses(classname: String): String {
    val classFindRegex = "\\.(?=[A-Z])".toRegex()
    val (pkg, name) = classname.split(classFindRegex, limit = 2)
    return "$pkg.${name.replace(classFindRegex, "\\$")}"
}