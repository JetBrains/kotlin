/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import kotlin.system.exitProcess

/**
 * Arguments are as follows:
 * 1. `<btaVersion>` - current BTA version string (can be a snapshot, e.g. `2.4.255-SNAPSHOT`)
 * 2. `<compatibilityType>` - `backward` or `forward`
 * 3. `<compatibilityTestsVersions>` - comma-separated list of pinned version strings already covered
 * 4. `<compatibilityTestsExcludedVersions>` - (optional) comma-separated list of intentionally excluded versions; defaults to empty
 */
fun main(args: Array<String>) {
    val btaVersion = args[0]
    val compatibilityType = CompatibilityType.fromString(args[1])
    val covered = parseVersionList(args[2])
    val excluded = args.getOrNull(3)?.let { parseVersionList(it) } ?: emptyList()

    try {
        val currentVersion = KotlinToolingVersion(btaVersion)
        val allPublishedVersions = MavenMetadataFetcher(MAVEN_METADATA_URL).fetch()
        val compatibleVersions = CompatibilityWindowSelector(compatibilityType).select(allPublishedVersions, currentVersion)

        checkCompatibilityCoverage(compatibleVersions, covered.toSet(), excluded.toSet())
    } catch (e: Throwable) {
        System.err.println(e.message)
        exitProcess(1)
    }
}

private const val MAVEN_METADATA_URL = "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-build-tools-api/maven-metadata.xml"

private fun parseVersionList(csv: String): List<KotlinToolingVersion> =
    csv.split(",").filter { it.isNotBlank() }.map { KotlinToolingVersion(it.trim()) }

private fun checkCompatibilityCoverage(
    expected: List<KotlinToolingVersion>,
    covered: Set<KotlinToolingVersion>,
    excluded: Set<KotlinToolingVersion>,
) {
    val missing = expected.filterNot { it in covered || it in excluded }

    if (missing.isNotEmpty()) {
        error("BTA compatibility coverage check failed: ${missing.size} version(s) missing: [${missing.joinToString()}]")
    }

    println("All published BTA versions are covered.")
}

