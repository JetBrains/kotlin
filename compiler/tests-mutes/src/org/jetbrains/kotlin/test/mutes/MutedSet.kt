/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

import java.io.File

class MutedSet(muted: List<MutedTest>) {
    // Method key -> Simple class name -> List of muted tests
    private val cache: Map<String, Map<String, List<MutedTest>>> =
        muted
            .groupBy { it.methodKey } // Method key -> List of muted tests
            .mapValues { (_, tests) -> tests.groupBy { it.simpleClassName } }

    fun mutedTest(testClass: Class<*>, methodKey: String): MutedTest? {
        val mutedTests = cache[methodKey]?.get(testClass.simpleName) ?: return null

        return mutedTests.firstOrNull { mutedTest ->
            testClass.canonicalName.endsWith(mutedTest.classNameKey)
        }
    }
}

private fun loadMutedSet(files: List<File>): MutedSet {
    return MutedSet(files.flatMap { file -> loadMutedTests(file) })
}

val mutedSet by lazy {
    loadMutedSet(
        listOf(
            File("tests/mute-common.csv"),
            File("tests/mute-platform.csv")
        )
    )
}