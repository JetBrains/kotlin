/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.serialization.CachedLibrariesBase.Kind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

abstract class Cache(protected val target: KonanTarget, val kind: Kind, val path: String, val rootDirectory: String) {
    val bitcodeDependencies by lazy { computeBitcodeDependencies() }
    val binariesPaths by lazy { computeBinariesPaths() }
    val serializedInlineFunctionBodies by lazy { computeSerializedInlineFunctionBodies() }
    val serializedClassFields by lazy { computeSerializedClassFields() }
    val serializedEagerInitializedFiles by lazy { computeSerializedEagerInitializedFiles() }

    protected abstract fun computeBitcodeDependencies(): List<NativeUnresolvedDependency>
    protected abstract fun computeBinariesPaths(): List<String>
    protected abstract fun computeSerializedInlineFunctionBodies(): List<SerializedInlineFunctionReference>
    protected abstract fun computeSerializedClassFields(): List<SerializedClassFields>
    protected abstract fun computeSerializedEagerInitializedFiles(): List<SerializedEagerInitializedFile>

    protected fun Kind.toCompilerOutputKind(): CompilerOutputKind = when (this) {
        Kind.DYNAMIC -> CompilerOutputKind.DYNAMIC_CACHE
        Kind.STATIC -> CompilerOutputKind.STATIC_CACHE
        Kind.HEADER -> CompilerOutputKind.HEADER_CACHE
    }
}

