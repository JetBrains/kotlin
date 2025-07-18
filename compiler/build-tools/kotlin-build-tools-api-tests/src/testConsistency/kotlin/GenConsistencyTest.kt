/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class GenConsistencyTest {
    @Test
    @DisplayName("Check that generated BTA API classes are up-to-date")
    fun testGeneratedApiClassesAreUpToDate() {
        val apiGenPath = File("../kotlin-build-tools-api/gen")
        checkPathHash(apiGenPath, 440032856, "kotlin-build-tools-api")
    }

    @Test
    @DisplayName("Check that generated BTA IMPL classes are up-to-date")
    fun testGeneratedImplClassesAreUpToDate() {
        val apiGenPath = File("../kotlin-build-tools-impl/gen")
        checkPathHash(apiGenPath, 3374709277, "kotlin-build-tools-impl")
    }
}

private fun checkPathHash(apiGenPath: File, expectedHash: Long, moduleName: String) {
    val apiGenFiles = apiGenPath.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(apiGenPath).path }
    var hash: Long = 0
    for (genFile in apiGenFiles) {
        hash += genFile.readText().hashCode()
    }
    assertEquals(
        expectedHash,
        hash,
        "Generated BTA classes have changed, please run " +
                "'./gradlew :compiler:build-tools:$moduleName:generateBtaArguments' task to regenerate them " +
                "and update the hash in this test."
    )
}
