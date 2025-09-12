/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.jvm.operations.CriDeserializerRetrievalOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CriFileIdsToPathsDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CriLookupsDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CriSubtypesDeserializationOperation

@ExperimentalBuildToolsApi
public interface CriToolchain {
    /**
     * Example:
     *  ```
     *   val toolchain = KotlinToolchain.loadImplementation(ClassLoader.getSystemClassLoader())
     *   val operation = toolchain.jvm.createCriLookupsDeserializationOperation(lookupsByteArray)
     *   val deserializedLookups = toolchain.createBuildSession().use { it.executeOperation(operation) }
     *  ```
     */
    public fun createCriLookupsDeserializationOperation(lookups: ByteArray): CriLookupsDeserializationOperation
    public fun createCriFileIdsToPathsDeserializationOperation(fileIdsToPaths: ByteArray): CriFileIdsToPathsDeserializationOperation
    public fun createCriSubtypesDeserializationOperation(subtypes: ByteArray): CriSubtypesDeserializationOperation


    /**
     * Example:
     *  ```
     *   val toolchain = KotlinToolchain.loadImplementation(ClassLoader.getSystemClassLoader())
     *   val operation = toolchain.jvm.createCriDeserializerRetrievalOperation()
     *   val deserializer = toolchain.createBuildSession().use { it.executeOperation(operation) }
     *   val deserializedLookups = deserializer.deserializeLookupData(lookupsByteArray)
     *  ```
     */
    public fun createCriDeserializerRetrievalOperation(): CriDeserializerRetrievalOperation
}
