import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.buildtools.api.trackers.IcEventCollector
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Paths

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class IcEventCollectorTest : BaseCompilationTest() {

    @DisplayName("IC_EVENT_COLLECTOR can be set using the Option in BaseCompilationOperation")
    @BtaVersionsOnlyCompilationTest
    fun setIcEventCollector(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
        val eventCollector = object : IcEventCollector {
            override fun collectEvents(eventsFromBta: List<IcEvent>) {}
        }

        jvmOperation[BaseCompilationOperation.IC_EVENT_COLLECTOR] = eventCollector
        assertEquals(eventCollector, jvmOperation[BaseCompilationOperation.IC_EVENT_COLLECTOR])
        assertEquals(eventCollector, jvmOperation.build()[BaseCompilationOperation.IC_EVENT_COLLECTOR])
    }
}
