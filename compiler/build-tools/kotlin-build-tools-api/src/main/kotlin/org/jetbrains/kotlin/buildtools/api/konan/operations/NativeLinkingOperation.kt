/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompositeBuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.konan.NativeFullCache
import org.jetbrains.kotlin.buildtools.api.konan.NativeHeaderCache
import org.jetbrains.kotlin.buildtools.api.konan.NativeKlibResolverResult
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.api.konan.NativeResolvedKlib
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface NativeLinkingOperation : CompositeBuildOperation<CompilationResult> {
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        // TODO: Native second stage compiler options
    }
}

// EXPOSITION ONLY:

@ExperimentalBuildToolsApi
internal val NativeResolvedKlib.headerCachePath: Path
    get() = TODO()

@ExperimentalBuildToolsApi
internal val NativeResolvedKlib.fullCachePath: Path
    get() = TODO()

@ExperimentalBuildToolsApi
internal data class NativeHeaderCacheImpl(
    override val klib: NativeResolvedKlib,
) : NativeHeaderCache {
    override val location: Path
        get() = klib.headerCachePath

    override val dependencies: Set<NativeHeaderCache>
        get() = klib.dependencies.map { NativeHeaderCacheImpl(it) }.toSet()
}

@ExperimentalBuildToolsApi
internal data class NativeFullCacheImpl(
    override val headerCache: NativeHeaderCache
) : NativeFullCache {
    override val location: Path
        get() = klib.fullCachePath
}

@ExperimentalBuildToolsApi
internal class SubOperationImpl(
    override val operation: BuildOperation<*>,
) : CompositeBuildOperation.SubOperation {
    val mutableDependencies = mutableSetOf<SubOperationImpl>()

    override val dependencies: Set<CompositeBuildOperation.SubOperation>
        get() = mutableDependencies
}

@ExperimentalBuildToolsApi
internal fun NativeLinkingOperation.subOperationsWithCaches(
    toolchain: NativePlatformToolchain,
    buildSession: KotlinToolchains.BuildSession,
    klibs: List<Path>, // the same one that was passed to NativePlatformToolchain.createNativeLinkingOperation
    destinationDirectory: Path, // the same one that was passed to NativePlatformToolchain.createNativeLinkingOperation
): List<CompositeBuildOperation.SubOperation> {
    // Has to be executed immediately, not a sub operation.
    val allKlibs = (buildSession.executeOperation(toolchain.createNativeKlibResolverOperation(klibs)) as NativeKlibResolverResult.Success).allKlibs
    val allHeaderCaches = allKlibs.map { NativeHeaderCacheImpl(it) }
    val headerCacheOperations = allHeaderCaches.associateWith {
        SubOperationImpl(toolchain.createNativeHeaderCacheOperation(it.klib).apply {
            this[NativeHeaderCacheOperation.DEPENDENCIES] = it.dependencies.toList()
        })
    }
    headerCacheOperations.forEach {
        it.value.mutableDependencies.addAll(it.key.dependencies.map {
            headerCacheOperations[it]!!
        })
    }
    val allFullCaches = allHeaderCaches.map { NativeFullCacheImpl(it) }
    val fullCacheOperations = allFullCaches.associateWith {
        SubOperationImpl(toolchain.createNativeFullCacheOperation(it.klib).apply {
            this[NativeFullCacheOperation.DEPENDENCIES] = it.dependencies.toList()
        })
    }
    fullCacheOperations.forEach {
        it.value.mutableDependencies.addAll(it.key.dependencies.map {
            headerCacheOperations[it]!! // only depends on the header cache of dependencies; not even on the header cache of self
        })
    }
    val finalLinkingOperation = SubOperationImpl(toolchain.createNativeCacheLinkingOperation(
        emptyList(),
        allFullCaches,
        destinationDirectory,
    ))
    finalLinkingOperation.mutableDependencies.addAll(fullCacheOperations.values)
    return buildList {
        addAll(headerCacheOperations.values)
        addAll(fullCacheOperations.values)
        add(finalLinkingOperation)
    }
}

@ExperimentalBuildToolsApi
internal fun NativeLinkingOperation.subOperationsWithoutCaches(
    toolchain: NativePlatformToolchain,
    buildSession: KotlinToolchains.BuildSession,
    klibs: List<Path>, // the same one that was passed to NativePlatformToolchain.createNativeLinkingOperation
    destinationDirectory: Path, // the same one that was passed to NativePlatformToolchain.createNativeLinkingOperation
): List<CompositeBuildOperation.SubOperation> {
    // Has to be executed immediately, not a sub operation.
    val allKlibs = (buildSession.executeOperation(toolchain.createNativeKlibResolverOperation(klibs)) as NativeKlibResolverResult.Success).allKlibs
    val finalLinkingOperation = SubOperationImpl(toolchain.createNativeCacheLinkingOperation(
        allKlibs,
        emptyList(),
        destinationDirectory,
    ))
    return listOf(finalLinkingOperation)
}