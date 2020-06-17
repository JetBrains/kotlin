/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

import java.io.File

fun main() {
    syncMutedTestsOnTeamCityWithDatabase()
}

/**
 * Synchronize muted tests on teamcity with flaky tests in database
 *
 * Purpose: possibility to run flaky tests on teamcity that will not affect on build status
 */
fun syncMutedTestsOnTeamCityWithDatabase() {
    val remotelyMutedTests = getMutedTestsOnTeamcityForRootProject()
    for (scope in Scope.values().filter { it.id != null }) {
        val remotelyMutedTestsForSpecificScope: Map<String, MuteTestJson> = filterMutedTestsByScope(remotelyMutedTests, scope)
        val locallyMutedTests: Map<String, MuteTestJson> = getMutedTestsFromDatabase(scope)
        val deleteList = remotelyMutedTestsForSpecificScope - locallyMutedTests.keys
        val uploadList = locallyMutedTests - remotelyMutedTestsForSpecificScope.keys
        deleteMutedTests(deleteList)
        uploadMutedTests(uploadList)
    }
}

private fun getMutedTestsFromDatabase(scope: Scope): Map<String, MuteTestJson> {
    val mutedSet = MutedSet(loadMutedTests(scope.localDBPath))
    val mutedMap = mutableMapOf<String, MuteTestJson>()
    for (muted in mutedSet.flakyTests) {
        val testName = formatClassnameWithInnerClasses(muted.key)
        mutedMap[testName] = createMuteTestJson(testName, muted.issue ?: "", scope)
    }
    return mutedMap
}

private fun formatClassnameWithInnerClasses(classname: String): String {
    val classFindRegex = "\\.(?=[A-Z])".toRegex()
    val (pkg, name) = classname.split(classFindRegex, limit = 2)
    return "$pkg.${name.replace(classFindRegex, "\\$")}"
}

private fun filterMutedTestsByScope(muteTestJson: List<MuteTestJson>, scope: Scope): Map<String, MuteTestJson> {
    val filterCondition = { testJson: MuteTestJson ->
        if (scope.isBuildType) {
            val buildTypes = testJson.scope.get("buildTypes")
            val buildTypeIds = buildTypes?.get("buildType")?.toList()?.map {
                it.get("id").textValue()
            } ?: listOf()
            buildTypeIds.contains(scope.id)
        } else {
            testJson.scope.get("project")?.get("id")?.textValue() == scope.id
        }
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

private const val databaseDir = "../../tests"
private const val mutesPackageName = "org.jetbrains.kotlin.test.mutes"

// FIX ME WHEN BUNCH 192 REMOVED
// FIX ME WHEN BUNCH as36 REMOVED
internal enum class Scope(val id: String?, val localDBPath: File, val isBuildType: Boolean) {
    COMMON(System.getProperty("$mutesPackageName.tests.project.id"), File("$databaseDir/mute-common.csv"), false),
    IJ193(System.getProperty("$mutesPackageName.193"), File("$databaseDir/mute-platform.csv"), true),
    IJ192(System.getProperty("$mutesPackageName.192"), File("$databaseDir/mute-platform.csv.192"), true),
    IJ201(System.getProperty("$mutesPackageName.201"), File("$databaseDir/mute-platform.csv.201"), true),
    AS36(System.getProperty("$mutesPackageName.as36"), File("$databaseDir/mute-platform.csv.as36"), true),
    AS40(System.getProperty("$mutesPackageName.as40"), File("$databaseDir/mute-platform.csv.as40"), true);
}