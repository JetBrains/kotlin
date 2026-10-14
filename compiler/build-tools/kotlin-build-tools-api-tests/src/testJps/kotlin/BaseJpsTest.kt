/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

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
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import java.nio.file.Path

abstract class BaseJpsTest : BaseCompilationTest() {
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

internal fun Path.relativeToModule(fixtureName: String): String = toString().relativeToModule(fixtureName)

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
    val reports: MutableList<Pair<Path, String>> = mutableListOf()

    override fun report(filePath: Path, importedFqName: String) {
        reports += filePath to importedFqName
    }
}

internal class RecordingEnumWhenTracker : CompilerEnumWhenTracker {
    val reports: MutableList<Pair<Path, String>> = mutableListOf()

    override fun report(whenExpressionFilePath: Path, enumClassFqName: String) {
        reports += whenExpressionFilePath to enumClassFqName
    }
}

internal class RecordingInlineConstTracker : CompilerInlineConstTracker {
    data class Report(val filePath: Path, val owner: String, val name: String, val constType: String)

    val reports: MutableList<Report> = mutableListOf()

    override fun report(filePath: Path, owner: String, name: String, constType: String) {
        reports += Report(filePath, owner, name, constType)
    }
}

internal class RecordingExpectActualTracker : CompilerExpectActualTracker {
    val matched: MutableList<Pair<Path, Path>> = mutableListOf()
    val lenientStubs: MutableList<Path> = mutableListOf()

    override fun report(expectFilePath: Path, actualFilePath: Path) {
        matched += expectFilePath to actualFilePath
    }

    override fun reportExpectOfLenientStub(expectFilePath: Path) {
        lenientStubs.add(expectFilePath)
    }
}

internal class RecordingFileMappingTracker : CompilerFileMappingTracker {
    val sourcesToOutput: MutableList<Pair<List<Path>, Path>> = mutableListOf()
    val pluginReferencedSources: MutableList<Path> = mutableListOf()
    val pluginGeneratedOutputs: MutableList<Path> = mutableListOf()
    val pluginGeneratedSources: MutableList<Path> = mutableListOf()

    override fun recordSourceFilesToOutputFileMapping(sourceFilePaths: Collection<Path>, outputFilePath: Path) {
        sourcesToOutput += sourceFilePaths.toList() to outputFilePath
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFilePath: Path) {
        pluginReferencedSources.add(sourceFilePath)
    }

    override fun recordOutputFileGeneratedForPlugin(outputFilePath: Path) {
        pluginGeneratedOutputs.add(outputFilePath)
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFilePath: Path) {
        pluginGeneratedSources.add(sourceFilePath)
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
