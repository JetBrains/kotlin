/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GradleFailuresTest {
    @Test
    fun `failures of the build are those of the tasks and the others`() {
        fun node(message: String, vararg causes: FailureNode) = FailureNode(message, causes.toList())
        val compilation = node("Execution failed for task ':mod-b:compileTestKotlin'.", node("Compilation error."))
        val check = node("Execution failed for task ':mod-a:checkTestData'.")
        val configuration = node("A problem occurred evaluating project ':mod-c'.")

        fun render(failures: List<GradleFailure>): List<Pair<String?, String>> = failures.map { it.taskPath to it.text.lines().first() }

        // With several failures, Gradle reports their aggregate as the failure of the build
        assertEquals(
            listOf(
                ":mod-b:compileTestKotlin" to "Execution failed for task ':mod-b:compileTestKotlin'.",
                ":mod-a:checkTestData" to "Execution failed for task ':mod-a:checkTestData'.",
                null to "A problem occurred evaluating project ':mod-c'.",
            ),
            render(
                collectFailures(
                    taskFailures = listOf(":mod-b:compileTestKotlin" to compilation, ":mod-a:checkTestData" to check),
                    buildFailures = listOf(node("Build completed with 3 failures.", compilation, check, configuration)),
                    launchFailure = null,
                ),
            ),
        )

        // A failure of a task as the failure of the build may name the location of the script first, which is kept
        val located = "Build file '/repo/mod-a/build.gradle.kts' line: 12\nExecution failed for task ':mod-a:checkTestData'."
        assertEquals(
            listOf(GradleFailure(":mod-a:checkTestData", "$located\n> Boom")).map { it.taskPath to it.text },
            collectFailures(listOf(":mod-a:checkTestData" to check), listOf(node(located, node("Boom"))), launchFailure = null)
                .map { it.taskPath to it.text },
        )

        // Also after a Windows line break
        assertEquals(
            listOf(":mod-a:checkTestData" to "Build file '/repo/mod-a/build.gradle.kts' line: 12"),
            render(
                collectFailures(
                    listOf(":mod-a:checkTestData" to check),
                    listOf(node(located.replace("\n", "\r\n"))),
                    launchFailure = null,
                ),
            ),
        )

        // A failure of the build that only quotes a failure of a task is a failure of its own
        for (quoting in listOf("Publishing the results failed", "Cannot publish 'test results'")) {
            assertEquals(
                listOf(":mod-a:checkTestData" to "Execution failed for task ':mod-a:checkTestData'.", null to quoting),
                render(
                    collectFailures(
                        listOf(":mod-a:checkTestData" to check),
                        listOf(check, node("$quoting\nExecution failed for task ':mod-a:checkTestData'.")),
                        launchFailure = null,
                    ),
                ),
            )
        }

        // A single failure of a task is the failure of the build too; a lost connection comes on top of it
        assertEquals(
            listOf(":mod-a:checkTestData" to "Execution failed for task ':mod-a:checkTestData'.", null to "The daemon disappeared."),
            render(collectFailures(listOf(":mod-a:checkTestData" to check), listOf(check), node("The daemon disappeared."))),
        )
    }

    @Test
    fun `failures are rendered as the console shows them`() {
        val failure = FailureNode(
            "Execution failed for task ':mod-b:compileTestKotlin'.",
            listOf(
                FailureNode(
                    null,
                    listOf(
                        FailureNode(
                            "A failure occurred while executing GradleKotlinCompilerWorkAction",
                            listOf(FailureNode("Compilation error.\nSee log", emptyList())),
                        ),
                    ),
                ),
                FailureNode("Another cause", emptyList()),
            ),
        )
        assertEquals(
            """
                Execution failed for task ':mod-b:compileTestKotlin'.
                > A failure occurred while executing GradleKotlinCompilerWorkAction
                   > Compilation error.
                     See log
                > Another cause
            """.trimIndent(),
            renderFailure(failure),
        )

        // A cause repeating its parent, as Gradle wraps an unknown task, adds nothing
        val unknownTask = "Cannot locate tasks that match ':nope:checkTestData' as project 'nope' not found in root project 'kotlin'."
        assertEquals(unknownTask, renderFailure(FailureNode(unknownTask, listOf(FailureNode(unknownTask, emptyList())))))
        // So does a cause whose message the Tooling API repeats in its own
        val unknownOption = "Unknown command-line option '--nope'."
        assertEquals(
            "Could not execute build.\nProblem with provided build arguments.\n$unknownOption",
            renderFailure(
                FailureNode(
                    "Could not execute build.\nProblem with provided build arguments.\n$unknownOption",
                    listOf(
                        FailureNode(
                            "Problem with provided build arguments.\n$unknownOption",
                            listOf(FailureNode(unknownOption, emptyList())),
                        ),
                    ),
                ),
            ),
        )

        // A script failure names its location in its message
        assertEquals(
            "Build file '/repo/mod-b/build.gradle.kts' line: 82\nScript compilation error:\n  Line 82: nope()",
            renderFailure(
                FailureNode(
                    "Build file '/repo/mod-b/build.gradle.kts' line: 82\nScript compilation error:\n  Line 82: nope()",
                    emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `long failures are cut`() {
        val failure = FailureNode("Could not resolve all files.", (0 until 30).map { FailureNode("line $it", emptyList()) })
        val lines = renderFailure(failure).lines()
        assertEquals(21, lines.size)
        assertEquals("... 11 more lines", lines.last())
    }
}
