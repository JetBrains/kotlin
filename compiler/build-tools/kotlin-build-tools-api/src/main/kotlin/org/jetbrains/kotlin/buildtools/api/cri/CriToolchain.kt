/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain

/**
 * Allows creating operations that can be used for working with Compiler Reference Index generation.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.cri].
 *
 * An example of the basic usage is:
 *  ```
 *   val lookupData: ByteArray = ...
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.cri.createCriLookupDataDeserializationOperation(lookupData))
 *   val lookups = toolchain.createBuildSession().use { it.executeOperation(operation) }
 *  ```
 *
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public interface CriToolchain : KotlinToolchains.Toolchain {
    /**
     * Creates a build operation for deserializing [ByteArray] containing serialized lookup data into [LookupEntry]s.
     *
     * @param data serialized lookup data
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    public fun createCriLookupDataDeserializationOperation(data: ByteArray): CriLookupDataDeserializationOperation

    /**
     * Creates a build operation for deserializing [ByteArray] containing serialized fileIdToPath data into [FileIdToPathEntry]s.
     *
     * @param data serialized fileIdToPath data
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    public fun createCriFileIdToPathDataDeserializationOperation(data: ByteArray): CriFileIdToPathDataDeserializationOperation

    /**
     * Creates a build operation for deserializing [ByteArray] containing serialized subtype data into [SubtypeEntry]s.
     *
     * @param data serialized subtype data
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
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
