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
    val remotelyMutedTests = RemotelyMutedTests()
    val locallyMutedTests = LocallyMutedTests()

    syncMutedTests(remotelyMutedTests.projectTests, locallyMutedTests.projectTests)
}

private fun syncMutedTests(
    remotelyMutedTests: Map<String, MuteTestJson>,
    locallyMutedTests: Map<String, MuteTestJson>
) {
    val deleteList = remotelyMutedTests - locallyMutedTests.keys
    val uploadList = locallyMutedTests - remotelyMutedTests.keys
    deleteMutedTests(deleteList)
    uploadMutedTests(uploadList)
}

internal fun getMandatoryProperty(propertyName: String) = (System.getProperty(propertyName)
    ?: throw Exception("Property $propertyName must be set"))

private const val mutesPackageName = "org.jetbrains.kotlin.test.mutes"
internal val projectId = getMandatoryProperty("$mutesPackageName.tests.project.id")

class RemotelyMutedTests {
    val tests = getMutedTestsOnTeamcityForRootProject(projectId)
    val projectTests = getTestsJson(projectId, false)
    internal fun getTestsJson(scopeId: String, isBuildType: Boolean = true): Map<String, MuteTestJson> {
        return filterMutedTestsByScope(tests, scopeId, isBuildType)
    }
}

class LocallyMutedTests {
    private val muteCommonTestKey = "COMMON"
    val tests = getMutedTestsFromDatabase()
    val projectTests = getTestsJson(muteCommonTestKey, projectId, false)

    internal fun getTestsJson(platformId: String, scopeId: String, isBuildType: Boolean = true): Map<String, MuteTestJson> {
        return transformMutedTestsToJson(tests[platformId], scopeId, isBuildType)
    }

    private fun getMutedTestsFromDatabase(): Map<String, List<MutedTest>> {
        val databaseDir = "../../../tests"

        val commonDatabaseFile = File(databaseDir, "mute-common.csv")

        val mutedTestsMap = mutableMapOf<String, List<MutedTest>>()
        mutedTestsMap[muteCommonTestKey] = flakyTests(commonDatabaseFile)
        return mutedTestsMap
    }
}