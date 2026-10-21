/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer

/**
 * Marker interface for classes that can be serialized and deserialized for communication between BTA and the Daemon.
 *
 * It provides hooks for custom processing before serialization (usually on the BTA side) and after deserialization (usually in Daemon)
 * happens, in order for an object to prepare its state for crossing the process boundary and restore it.
 *
 * For example, in [beforeSerialization] any in-process callbacks can be removed and their presence recorded. Then, in the daemon process,
 * these callbacks can be reconstructed based on the recorded information, and using a mechanism for sending remote [Message]s.
 */
public interface BtaSerializable {
    public fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor>


    /**
     * Called after deserialization, usually in the daemon process. Can be used to reconstruct any callbacks or other state that was removed
     * during serialization.
     *
     * The new mechanism using [messageReporter] should be preferred for new callbacks. The [compilerFacade] is provided as a fallback for
     * operations that cannot be performed using the new mechanism at this time, such as JPS incremental caches passing.
     */
    public fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase? = null)
}

public interface SupportsNewDaemonMode<T> : BuildOperation<T>, BtaSerializable {
    public fun deserializeResult(serializedData: SerializedDataContainer): T
    public fun serializeResult(result: T): SerializedDataContainer
}
