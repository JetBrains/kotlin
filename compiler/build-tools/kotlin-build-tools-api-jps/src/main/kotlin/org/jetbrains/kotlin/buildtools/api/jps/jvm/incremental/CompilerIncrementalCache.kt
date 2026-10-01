/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi

/**
 * What a build system recorded about one module the last time it was compiled.
 *
 * When only some sources are recompiled, the compiler still has to see the whole module: the outputs left over from
 * previous compilations stand in for the sources that were not recompiled. This interface is how it reads them, and
 * how it learns which of them are stale and must not be resolved against.
 *
 * @since 2.5.0
 */
@InternalBuildToolsApi
public interface CompilerIncrementalCache {
    /**
     * Class files left over from a previous compilation whose declarations must no longer be visible.
     *
     * @return internal names of the classes to disregard
     */
    public fun getObsoletePackageParts(): Collection<String>

    /**
     * What a previous compilation recorded about the module as a whole.
     *
     * @return the recorded contents, or `null` if nothing was recorded
     */
    public fun getModuleMappingData(): ByteArray?

    /**
     * What a previous compilation recorded for the common part of a multiplatform module.
     *
     * @param fragmentName name of the module the part belongs to
     * @return the recorded contents, keyed by the path of the file each was produced from,
     *   or an empty map if nothing was recorded for [fragmentName]
     */
    public fun getMetadata(fragmentName: String): Map<String, ByteArray>
}
