/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain

public interface CriLookupDataDeserializationOperation : BuildOperation<Collection<LookupEntry>>
public interface CriFileIdToPathDataDeserializationOperation : BuildOperation<Collection<FileIdToPathEntry>>
public interface CriSubtypeDataDeserializationOperation : BuildOperation<Collection<SubtypeEntry>>

public interface CriToolchain : KotlinToolchains.Toolchain {
    /**
     * Example:
     *  ```
     *   val lookupData: ByteArray = ...
     *   val toolchain = KotlinToolchain.loadImplementation(ClassLoader.getSystemClassLoader())
     *   val operation = toolchain.cri.createCriLookupDataDeserializationOperation(lookupData)
     *   val lookups = toolchain.createBuildSession().use { it.executeOperation(operation) }
     *  ```
     */
    public fun createCriLookupDataDeserializationOperation(data: ByteArray): CriLookupDataDeserializationOperation
    public fun createCriFileIdToPathDataDeserializationOperation(data: ByteArray): CriFileIdToPathDataDeserializationOperation
    public fun createCriSubtypeDataDeserializationOperation(data: ByteArray): CriSubtypeDataDeserializationOperation

    public companion object {
        /**
         * Gets a [CriToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<CriToolchain>()`
         */
        @JvmStatic
        @get:JvmName("get")
        public inline val KotlinToolchains.cri: CriToolchain get() = getToolchain<CriToolchain>()
    }
}
