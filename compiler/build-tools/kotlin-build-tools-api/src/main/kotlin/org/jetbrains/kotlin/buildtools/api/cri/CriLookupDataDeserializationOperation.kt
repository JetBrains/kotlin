/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * Deserializes [ByteArray] containing serialized lookup data into [LookupEntry]s.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [CriToolchain.createCriLookupDataDeserializationOperation].
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
public interface CriLookupDataDeserializationOperation : BuildOperation<Collection<LookupEntry>>
