/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Deserializes [ByteArray] containing serialized fileIdToPath data into [FileIdToPathEntry]s.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [CriToolchain.createCriFileIdToPathDataDeserializationOperation].
 *
 * An example of the basic usage is:
 *  ```
 *   val fileIdToPathData: ByteArray = ...
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.cri.createCriFileIdToPathDataDeserializationOperation(fileIdToPathData))
 *   val fileIdsToPaths = toolchain.createBuildSession().use { it.executeOperation(operation) }
 *  ```
 *
 * @since 2.3.20
 */
@ExperimentalBuildToolsApi
public interface CriFileIdToPathDataDeserializationOperation : BuildOperation<Collection<FileIdToPathEntry>>
