/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCache
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerTargetId
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputFileBytesContain
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputFileBytesDoNotContain
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteExisting
import kotlin.io.path.readBytes

private class RecordingIncrementalCache(
    private val moduleMappingData: ByteArray? = null,
    private val obsoletePackageParts: Collection<String> = emptyList(),
) : CompilerIncrementalCache {
    val calls: MutableList<String> = mutableListOf()

    override fun getModuleMappingData(): ByteArray? {
        calls += "getModuleMappingData"
        return moduleMappingData
    }

    override fun getObsoletePackageParts(): Collection<String> {
        calls += "getObsoletePackageParts"
        return obsoletePackageParts
    }
}

@DisplayName("The JPS incremental compilation components")
class IncrementalCacheJpsTest : BaseJpsTest() {

    @DisplayName("The compiler asks the JPS components for the cache of the module being compiled")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-3")
    fun componentsAreQueried(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
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

    @DisplayName("The compiler merges the previous module mapping from the JPS cache and drops its obsolete parts")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-1")
    fun previousModuleMappingIsMerged(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            val mappingFile = "META-INF/${module.moduleName}.kotlin_module"

            module.compile(compilationConfigAction = { it.withJpsIc() }) {
                assertOutputFileBytesContain(mappingFile, "FooKt")
                assertOutputFileBytesContain(mappingFile, "BazKt")
            }
            val previousMapping = module.outputDirectory.resolve(mappingFile).readBytes()

            module.sourcesDirectory.resolve("foo.kt").deleteExisting()
            module.sourcesDirectory.resolve("bar.kt").deleteExisting()
            module.sourcesDirectory.resolve("baz.kt").deleteExisting()
            module.outputDirectory.resolve("BazKt.class").deleteExisting()
            module.createPredefinedFile("secret.kt", "new-file")

            val cache = RecordingIncrementalCache(previousMapping, obsoletePackageParts = listOf("BazKt"))
            module.compile(compilationConfigAction = { it.withJpsIc(SingleCacheComponents(cache)) }) {
                // FooKt can come only from the previous mapping, SecretKt is the part of this compilation,
                // and BazKt is dropped only by the obsolete parts
                assertOutputFileBytesContain(mappingFile, "FooKt")
                assertOutputFileBytesContain(mappingFile, "SecretKt")
                assertOutputFileBytesDoNotContain(mappingFile, "BazKt")
            }
        }
    }
}
