/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import java.nio.file.Path
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.BuildCacheOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeCachesOrchestrationOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeCompilationOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibsPopulatingOperation
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeLinkingOperation

@ExperimentalBuildToolsApi
public interface NativePlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * .kt -> .klib
     * @param destinationDirectory the directory where to put .klib either as a zip archive or as a directory
     */
    public fun createNativeCompilationOperation(sources: List<Path>, destinationDirectory: Path): NativeCompilationOperation

    /**
     * set of .klib files -> final binary (like .exe, .so, etc...)
     *
     * By default, handles all the cache machinery under the hood automatically,
     * however, may benefit from caches built via and set to [NativeLinkingOperation.CACHES]
     *
     * @param destinationDirectory the directory where to put the final binary
     */
    public fun createNativeLinkingOperation(
        klibs: Set<Path>,
        type: BinaryType,
        kind: BinaryKind,
        destinationDirectory: Path,
    ): NativeLinkingOperation

    /**
     * Given a set of klibs, populates it with additional (platform) klibs.
     * For example, it'll add `org.jetbrains.kotlin.native.platform.darwin`, `org.jetbrains.kotlin.native.platform.CoreFoundation` and some other klibs for kotlinx-coroutines-core-macosArm64
     *
     * The goal of having this as a separate operation is having the complete set of klibs to allow efficient cache management, like copying caches for the klibs to remote nodes
     *
     * The operation returns a set containing all the klibs from [klibs] + additional ones as the result of execution
     */
    public fun createKlibsPopulationOperation(klibs: Set<Path>): NativeKlibsPopulatingOperation

    /**
     * An operation that
     * 1. Analyzes the given klibs and populates them with additional (platform) klibs (if this wasn't done via [NativeKlibsPopulatingOperation]).
     *  For example, it'll add `org.jetbrains.kotlin.native.platform.darwin`, `org.jetbrains.kotlin.native.platform.CoreFoundation` and some other klibs for kotlinx-coroutines-core-macosArm64
     * 2. Determines which klibs are dirty (changed from previous cache buildings)
     * 2. Builds a disconnected DAG of dependencies between all of them.
     * 3. Deduplicates the DAG (the process to be designed; the aim is to avoid building caches for the same things multiple times in case of parallelization)
     *  It could have multiple copies of some subgraphs. For example, stdlib is expected to be seen everywhere or subgraph or the coroutines is expected to be quite popular.
     * 4. Makes a set of lists of [KlibCacheMetadata] (`Set<List<KlibCacheMetadata>>`) according to the DAG to perform caches transformations
     *
     * Each list represents a chain of [KlibCacheMetadata] that is used to create build operations via [createBuildCacheOperation], that means the next element likely depends on the result of the previous
     * The elements of the set can be run in parallel.
     *
     * Klib is considered dirty if [KlibCacheMetadata.isDirty] for it or there's a deep dirty klib among [KlibCacheMetadata.requiredKlibs].
     * Klib is considered deep dirty if it's dirty and its header cache has changed.
     *
     * Each chain is built depth-first from the DAG connected full subgraph. The following made-up graph:
     *               ktor-client
     *           /        |       \
     * kotlinx-io   ktor-utils  coroutines-core
     *                           /     |      \
     *                      stdlib atomicfu CoreFoundation
     *  represents a list from the set, and its value is, for example, [stdlib, atomicfu, CoreFoundation, coroutines-core, ktor-utils, kotlinx-io, ktor-client]
     *
     * The execution logic for each element of the list is:
     *  1. If this klib isn't dirty, skip it and proceed to the next.
     *  2. If klib is dirty, [createBuildCacheOperation] for it which may end up in marking it as deep dirty.
     */
    public fun createCachesOrchestrationOperation(klibs: Set<Path>, existingCaches: Set<Path>): NativeCachesOrchestrationOperation

    /**
     * An operation that builds caches for [klib] using [caches] from the dependencies.
     * Created based on [KlibCacheMetadata]
     */
    public fun createBuildCacheOperation(klib: Path, caches: Set<Path>, destination: Path): BuildCacheOperation

    public companion object {
        /**
         * Gets a [NativePlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<NativePlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.native: NativePlatformToolchain get() = getToolchain<NativePlatformToolchain>()
    }
}