/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.Companion.IC_EVENT_COLLECTOR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.buildtools.api.trackers.IcEventCollector
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class IcEventCollectorTest : BaseCompilationTest() {

    @DisplayName("IC_EVENT_COLLECTOR can be set using the Option in BaseCompilationOperation")
    @BtaVersionsOnlyCompilationTest
    fun setIcEventCollector(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
        val eventCollector = object : IcEventCollector {
            override fun collectEvents(eventsFromBta: List<IcEvent>) {}
        }

        jvmOperation[IC_EVENT_COLLECTOR] = eventCollector
        assertEquals(eventCollector, jvmOperation[IC_EVENT_COLLECTOR])
        assertEquals(eventCollector, jvmOperation.build()[IC_EVENT_COLLECTOR])
    }

    @DisplayName("IC_EVENT_COLLECTOR can be set and used to retrieve events in BTA V2 compilations")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-1")
    fun setIcEventCollectorAndCollectEvents(strategyConfig: org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("basic-multimodule-project/module-1")
            val events = mutableListOf<IcEvent>()
            val collector = object : IcEventCollector {
                override fun collectEvents(eventsFromBta: List<IcEvent>) {
                    events += eventsFromBta
                }
            }

            module1.compileIncrementally(SourcesChanges.ToBeCalculated, compilationConfigAction = { it[IC_EVENT_COLLECTOR] = collector })
            checkEvents(events, false)

            val sources = module1.sourcesDirectory
            Files.copy(
                sources.resolve("secret.kt.new-file"),
                sources.resolve("secret.kt"),
                StandardCopyOption.REPLACE_EXISTING
            )
            Files.copy(
                sources.resolve("bar.kt.add-default-argument"),
                sources.resolve("bar.kt"),
                StandardCopyOption.REPLACE_EXISTING
            )
            events.clear()

            module1.compileIncrementally(SourcesChanges.ToBeCalculated, compilationConfigAction = { it[IC_EVENT_COLLECTOR] = collector }) {
                assertCompiledSources("secret.kt", "bar.kt")
            }
            checkEvents(events, true)
        }
    }

    fun checkEvents(events: List<IcEvent>, isIncremental: Boolean) {
        assertEquals(1, events.count { it is IcEvent.CompilationStarted }) {
            "${if (isIncremental) "An incremental" else "The initial"} build should report one CompilationStarted event"
        }
        assertEquals(1, events.count { it is IcEvent.CompileIteration }) {
            "${if (isIncremental) "An incremental" else "The initial"} should report one CompileIteration event"
        }
        assertEquals(isIncremental, events.filterIsInstance<IcEvent.CompilationStarted>().single().isIncremental) {
            "${if (isIncremental) "An incremental" else "The initial"} should be reported as non-incremental"
        }
        assertTrue(events.all { it.iteration == 0 }) {
            "All events in ${if (isIncremental) "an incremental" else "the initial"} build should be reported with iteration 0"
        }
    }
}
