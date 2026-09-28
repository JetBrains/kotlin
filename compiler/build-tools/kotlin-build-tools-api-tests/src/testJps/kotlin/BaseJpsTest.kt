/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCache
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerPackagePartData
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerTargetId
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.*
import org.jetbrains.kotlin.buildtools.api.jps.jvm.operations.jpsManagedIcConfigurationBuilder
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader

abstract class BaseJpsTest : BaseCompilationTest() {

    /**
     * The JPS-managed incremental compilation configuration is only supported with the in-process execution
     * policy for now; see `JvmCompilationOperationImpl.checkSupportedWithDaemon`.
     */
    protected val inProcess: CompilerExecutionStrategyConfiguration
        get() = toolchain to toolchain.createInProcessExecutionPolicy()

    protected val daemon: ExecutionPolicy
        get() = toolchain.daemonExecutionPolicyBuilder().build()

    protected companion object {
        val toolchain: KotlinToolchains
            get() = KotlinToolchains.loadImplementation(btaClassloader)
    }
}

/**
 * Turns an absolute compiler-reported path into a module-relative, `/`-separated one.
 *
 * [fixtureName] is the RAW fixture path passed to `module(...)`, NOT [JvmModule.moduleName] - the latter has its
 * path separators replaced with underscores and would never match, making `substringAfter` return the whole string.
 */
internal fun String.relativeToModule(fixtureName: String): String =
    replace('\\', '/').substringAfter(fixtureName.replace('\\', '/'))

/** A cache that records nothing and knows nothing; the default for tracker-only tests. */
internal open class EmptyIncrementalCache : CompilerIncrementalCache {
    override fun getObsoletePackageParts(): Collection<String> = emptyList()
    override fun getObsoleteMultifileClasses(): Collection<String> = emptyList()
    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? = null
    override fun getPackagePartData(partInternalName: String): CompilerPackagePartData? = null
    override fun getModuleMappingData(): ByteArray? = null
    override fun getMetadata(fragmentName: String): Map<String, ByteArray> = emptyMap()
    override fun getClassFilePath(internalClassName: String): String = "$internalClassName.class"
    override fun close() {}
}

internal open class SingleCacheComponents(private val cache: CompilerIncrementalCache = EmptyIncrementalCache()) :
    CompilerIncrementalCompilationComponents {
    val requestedTargets: MutableList<CompilerTargetId> = mutableListOf()

    override fun getIncrementalCache(target: CompilerTargetId): CompilerIncrementalCache {
        requestedTargets += target
        return cache
    }
}

/** Shorthand used by every test in this suite. */
internal fun JvmCompilationOperation.Builder.withJpsIc(
    components: CompilerIncrementalCompilationComponents = SingleCacheComponents(),
    configure: JvmJpsManagedIncrementalCompilationConfiguration.Builder.() -> Unit = {},
) {
    val ic = jpsManagedIcConfigurationBuilder(components)
    ic.configure()
    this[JvmCompilationOperation.INCREMENTAL_COMPILATION] = ic.build()
}

internal class RecordingImportTracker : CompilerImportTracker {
    val reports: MutableList<Pair<String, String>> = mutableListOf()

    override fun report(filePath: String, importedFqName: String) {
        reports += filePath to importedFqName
    }
}

internal class RecordingEnumWhenTracker : CompilerEnumWhenTracker {
    val reports: MutableList<Pair<String, String>> = mutableListOf()

    override fun report(whenExpressionFilePath: String, enumClassFqName: String) {
        reports += whenExpressionFilePath to enumClassFqName
    }
}

internal class RecordingInlineConstTracker : CompilerInlineConstTracker {
    data class Report(val filePath: String, val owner: String, val name: String, val constType: String)

    val reports: MutableList<Report> = mutableListOf()

    override fun report(filePath: String, owner: String, name: String, constType: String) {
        reports += Report(filePath, owner, name, constType)
    }
}

internal class RecordingExpectActualTracker : CompilerExpectActualTracker {
    val matched: MutableList<Pair<String, String>> = mutableListOf()
    val lenientStubs: MutableList<String> = mutableListOf()

    override fun report(expectFilePath: String, actualFilePath: String) {
        matched += expectFilePath to actualFilePath
    }

    override fun reportExpectOfLenientStub(expectFilePath: String) {
        lenientStubs += expectFilePath
    }
}

internal class RecordingFileMappingTracker : CompilerFileMappingTracker {
    val sourcesToOutput: MutableList<Pair<List<String>, String>> = mutableListOf()
    val pluginReferencedSources: MutableList<String> = mutableListOf()
    val pluginGeneratedOutputs: MutableList<String> = mutableListOf()
    val pluginGeneratedSources: MutableList<String> = mutableListOf()

    override fun recordSourceFilesToOutputFileMapping(sourceFilePaths: Collection<String>, outputFilePath: String) {
        sourcesToOutput += sourceFilePaths.toList() to outputFilePath
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFilePath: String) {
        pluginReferencedSources += sourceFilePath
    }

    override fun recordOutputFileGeneratedForPlugin(outputFilePath: String) {
        pluginGeneratedOutputs += outputFilePath
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFilePath: String) {
        pluginGeneratedSources += sourceFilePath
    }
}

internal class RecordingLookupTracker : CompilerLookupTracker {
    data class Lookup(
        val filePath: String,
        val scopeFqName: String,
        val scopeKind: CompilerLookupTracker.ScopeKind,
        val name: String,
    )

    val lookups: MutableList<Lookup> = mutableListOf()

    override fun recordLookup(filePath: String, scopeFqName: String, scopeKind: CompilerLookupTracker.ScopeKind, name: String) {
        lookups += Lookup(filePath, scopeFqName, scopeKind, name)
    }

    override fun clear() {}
}
