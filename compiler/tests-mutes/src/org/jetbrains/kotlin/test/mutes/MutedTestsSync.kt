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
    val bunches = Bunches.parseRulesToBunches(locallyMutedTests.tests.keys)

    syncMutedTests(remotelyMutedTests.projectTests, locallyMutedTests.projectTests)

    for ((originalBunchId, foundBunchId) in bunches) {
        getBuildTypeIds(originalBunchId)?.let { buildTypeIds ->
            for (buildTypeId in buildTypeIds.split(",")) {
                syncMutedTests(remotelyMutedTests.getTestsJson(buildTypeId), locallyMutedTests.getTestsJson(foundBunchId, buildTypeId))
            }
        }
    }
}

private fun syncMutedTests(
    remotelyMutedTests: Map<String, MuteTestJson>,
    locallyMutedTests: Map<String, MuteTestJson>
) {
    val uploadList = locallyMutedTests - remotelyMutedTests.keys
    val deleteList = remotelyMutedTests - locallyMutedTests.keys
    deleteMutedTests(deleteList)
    uploadMutedTests(uploadList)
}

internal fun getMandatoryProperty(propertyName: String) = (System.getProperty(propertyName)
    ?: throw Exception("Property $propertyName must be set"))

object Bunches {
    private val bunchRules: List<String> = readAllRulesFromFile()
    internal val baseBunchId = bunchRules.first()

    internal fun parseRulesToBunches(platforms: Set<String>): Map<String, String> {
        return bunchRules
            .map { it.split('_') }
            .map { rule ->
                rule.first() to (rule.find { platforms.contains(it) } ?: baseBunchId)
            }.toMap()
    }

    private fun readAllRulesFromFile(): List<String> {
        val file = File("../..", ".bunch")
        if (!file.exists()) {
            throw BunchException("Can't build list of rules. File '${file.canonicalPath}' doesn't exist")
        }
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private class BunchException(msg: String? = null) : Exception(msg)
}

private const val mutesPackageName = "org.jetbrains.kotlin.test.mutes"
internal val projectId = getMandatoryProperty("$mutesPackageName.tests.project.id")
internal fun getBuildTypeIds(bunchId: String) = System.getProperty("$mutesPackageName.$bunchId")

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
        val mutedTestsMap = mutableMapOf<String, List<MutedTest>>()
        val databaseDir = "../../tests"

        val commonDatabaseFile = File(databaseDir, "mute-common.csv")
        mutedTestsMap[muteCommonTestKey] = flakyTests(commonDatabaseFile)

        val platformDatabaseFile = File(databaseDir, "mute-platform.csv")
        File(databaseDir).walkTopDown().filter { f -> f.name.startsWith(platformDatabaseFile.name) }.toList().map { f ->
            val key = if (f.extension != "csv") f.extension else Bunches.baseBunchId
            mutedTestsMap[key] = flakyTests(f)
        }

        return mutedTestsMap
    }
}