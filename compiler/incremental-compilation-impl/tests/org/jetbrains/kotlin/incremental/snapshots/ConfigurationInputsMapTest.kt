/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.incremental.HashedConfigurationInputs
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.RebuildReason
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.properties.Delegates
import kotlin.test.assertIs

class ConfigurationInputsMapTest : TestWithWorkingDir() {
    private var map: ConfigurationInputsMap by Delegates.notNull()
    private lateinit var storageFile: File

    @Before
    override fun setUp() {
        super.setUp()
        val caches = File(workingDir, "caches").apply { mkdirs() }
        storageFile = File(caches, "configuration-inputs.tab")
        map = ConfigurationInputsMap(storageFile, IncrementalCompilationContext())
    }

    @After
    override fun tearDown() {
        map.close()
        super.tearDown()
    }

    @Test
    fun testFirstBuildNoRebuild() {
        val hashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(1, 2, 3)))
        val state = map.checkConfigurationState(hashedInputs)
        assertIs<ConfigurationInputsMap.ConfigurationState.RequiresRebuild>(state)
        assertEquals(
            "First build does not have any prior state and should report ${RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS}",
            RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS,
            state.reason,
        )
    }

    @Test
    fun testSameHashNoRebuild() {
        val hashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(1, 2, 3)))
        map.updateHash(hashedInputs)
        val state = map.checkConfigurationState(hashedInputs)
        assertIs<ConfigurationInputsMap.ConfigurationState.UpToDate>(state)
    }

    @Test
    fun testDifferentHashTriggersRebuild() {
        val oldHashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(1, 2, 3)))
        val newHashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(4, 5, 6)))
        map.updateHash(oldHashedInputs)
        val state = map.checkConfigurationState(newHashedInputs)
        assertIs<ConfigurationInputsMap.ConfigurationState.RequiresRebuild>(state)
        assertEquals("Different hash should trigger rebuild", RebuildReason.COMPILER_ARGS_CHANGED, state.reason)
    }

    @Test
    fun testChangedKeySetToEmpty() {
        val oldHashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(1, 2, 3)))
        val newHashedInputs = HashedConfigurationInputs(mapOf())
        map.updateHash(oldHashedInputs)
        val state = map.checkConfigurationState(newHashedInputs)
        assertIs<ConfigurationInputsMap.ConfigurationState.RequiresRebuild>(state)
        assertEquals(
            "Changes to the map key set should cause rebuild with ${RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS}",
            RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS,
            state.reason,
        )
    }

    @Test
    fun testChangedKeySet() {
        val oldHashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(1, 2, 3)))
        val newHashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.UNSAFE_INCREMENTAL_CHANGE_KT_62686 to byteArrayOf(1, 2, 3)))
        map.updateHash(oldHashedInputs)
        val state = map.checkConfigurationState(newHashedInputs)
        assertIs<ConfigurationInputsMap.ConfigurationState.RequiresRebuild>(state)
        assertEquals(
            "Changes to the map key set should cause rebuild with ${RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS}",
            RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS,
            state.reason,
        )
    }

    @Test
    fun testUpdateHashPersistsAcrossInstances() {
        val hashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(1, 2, 3)))
        map.updateHash(hashedInputs)
        map.close()

        // Reopen as a new instance (simulates next build)
        val map2 = ConfigurationInputsMap(storageFile, IncrementalCompilationContext())
        map2.use { map2 ->
            val loadedState = map2.checkConfigurationState(hashedInputs)
            assertIs<ConfigurationInputsMap.ConfigurationState.UpToDate>(loadedState)
            val differentHashedInputs = HashedConfigurationInputs(mapOf(RebuildReason.COMPILER_ARGS_CHANGED to byteArrayOf(99)))
            val changedState = map2.checkConfigurationState(differentHashedInputs)
            assertIs<ConfigurationInputsMap.ConfigurationState.RequiresRebuild>(changedState)
            assertEquals(
                "Different hash after persistence should trigger rebuild",
                RebuildReason.COMPILER_ARGS_CHANGED,
                changedState.reason,
            )
        }
    }
}
