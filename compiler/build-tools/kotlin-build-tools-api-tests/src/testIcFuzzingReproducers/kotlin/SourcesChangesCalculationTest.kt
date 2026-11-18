/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.SourcesChangesMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("The source changes calculated by the incremental compilation")
class SourcesChangesCalculationTest {
    private fun deletingFile(sourcesChangesMode: SourcesChangesMode) = icFuzzingReproducer(sourcesChangesMode) {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "package p0\n\nval p = 1\n",
                "b.kt" to "package p0\n\nconst val q = 2L\n",
            ),
        )
        step("delete b.kt", removed = setOf("b.kt"))
    }

    @Test
    @DisplayName("a deleted file is detected after a rebuild with unknown changes")
    fun testDeletedFileAfterRebuild() = deletingFile(SourcesChangesMode.CALCULATED_AFTER_REBUILD)

    @Test
    @DisplayName("a deleted file is detected when the changes are calculated from the first build on")
    fun testDeletedFile() = deletingFile(SourcesChangesMode.CALCULATED)
}
