/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerTargetId
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.readBytes

private class RecordingIncrementalCache : EmptyIncrementalCache() {
    val calls: MutableList<String> = mutableListOf()

    override fun getModuleMappingData(): ByteArray? {
        calls += "getModuleMappingData"
        return null
    }

    // No `getObsoletePackageParts` recorder: it is only reached from `addCompiledParts`, which runs only when
    // `getModuleMappingData()` returns non-null - and this stub returns null.
}

@DisplayName("The JPS incremental compilation components")
class IncrementalCacheJpsTest : BaseJpsTest() {

    @DisplayName("The compiler asks the JPS components for the cache of the module being compiled")
    @TestMetadata("basic-multimodule-project/module-3")
    @Test
    fun componentsAreQueried() {
        jvmProject(inProcess) {
            val module = module("basic-multimodule-project/module-3")
            val cache = RecordingIncrementalCache()
            val components = SingleCacheComponents(cache)
            module.compile(compilationConfigAction = { it.withJpsIc(components) }) {
                assertEquals(
                    setOf(CompilerTargetId(module.moduleName, "java-production")),
                    components.requestedTargets.toSet(),
                )
                assertTrue("getModuleMappingData" in cache.calls) { "calls: ${cache.calls}" }
            }
        }
    }
}
