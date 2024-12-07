/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

import kotlinx.coroutines.runBlocking
import java.io.File

suspend fun main() {
    syncMutedTestsOnTeamCityWithDatabase()
}

/**
 * Synchronize muted tests on teamcity with flaky tests in database
 *
 * Purpose: possibility to run flaky tests on teamcity that will not affect on build status
 */
suspend fun syncMutedTestsOnTeamCityWithDatabase() {
    val remotelyMutedTests = RemotelyMutedTests()
    val locallyMutedTests = LocallyMutedTests()

    syncMutedTests(remotelyMutedTests.projectTests, locallyMutedTests.projectTests)
}

private suspend fun syncMutedTests(
    remotelyMutedTests: Map<String, MuteTestJson>,
    locallyMutedTests: Map<String, MuteTestJson>,
) {
    val deleteList = remotelyMutedTests - locallyMutedTests.keys
    val uploadList = locallyMutedTests - remotelyMutedTests.keys
    deleteMutedTests(deleteList)
    uploadMutedTests(uploadList)
}

internal fun getMandatoryProperty(propertyName: String) =
    System.getProperty(propertyName) ?: throw Exception("Property $propertyName must be set")

private const val MUTES_PACKAGE_NAME = "org.jetbrains.kotlin.test.mutes"
internal val projectId = getMandatoryProperty("$MUTES_PACKAGE_NAME.tests.project.id")

class RemotelyMutedTests {
    private val tests = runBlocking { getMutedTestsOnTeamcityForRootProject(projectId) }
    val projectTests = getTestsJson(projectId)
    private fun getTestsJson(scopeId: String): Map<String, MuteTestJson> {
        return filterMutedTestsByScope(tests, scopeId)
    }
}

class LocallyMutedTests {
    val projectTests = transformMutedTestsToJson(getCommonMuteTests(), projectId)

    private fun getCommonMuteTests(): List<MutedTest> {
        val databaseDir = "../../../tests"
        val commonDatabaseFile = File(databaseDir, "mute-common.csv")
        return flakyTests(commonDatabaseFile)
    }
}