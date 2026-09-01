/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * What a build system recorded about one module the last time it was compiled.
 *
 * When only some sources are recompiled, the compiler still has to see the whole module: the outputs left over from
 * previous compilations stand in for the sources that were not recompiled. This interface is how it reads them, and
 * how it learns which of them are stale and must not be resolved against.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerIncrementalCache {
    /**
     * Class files left over from a previous compilation whose declarations must no longer be visible.
     *
     * @return internal names of the classes to disregard
     */
    public fun getObsoletePackageParts(): Collection<String>

    /**
     * Classes assembled from several source files by a previous compilation whose declarations must no longer be
     * visible.
     *
     * @return internal names of the classes to disregard
     */
    public fun getObsoleteMultifileClasses(): Collection<String>

    /**
     * The parts a class assembled from several source files was built from, for the parts that are still current.
     *
     * @param facadeInternalName internal name of the assembled class
     * @return internal names of its current parts, or `null` if nothing was recorded for [facadeInternalName]
     */
    public fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>?

    /**
     * The declarations recorded for a class file that is not being recompiled.
     *
     * @param partInternalName internal name of the class
     * @return what was recorded, or `null` if nothing was recorded for [partInternalName]
     */
    public fun getPackagePartData(partInternalName: String): CompilerPackagePartData?

    /**
     * What a previous compilation recorded about the module as a whole.
     *
     * @return the recorded contents, or `null` if nothing was recorded
     */
    public fun getModuleMappingData(): ByteArray?

    /**
     * What a previous compilation recorded for the common part of a multiplatform module.
     *
     * @param fragmentName name of the part
     * @return the recorded contents, keyed by the path of the file each was produced from
     */
    public fun getMetadata(fragmentName: String): Map<String, ByteArray>

    /**
     * Where a class produced by a previous compilation was written.
     *
     * @param internalClassName internal name of the class
     * @return the path of the file holding it
     */
    public fun getClassFilePath(internalClassName: String): String

    /**
     * Called when the compiler is finished reading from this cache.
     *
     * A build system that keeps its storage open across compilations may leave this empty and close it itself.
     */
    public fun close()
}
