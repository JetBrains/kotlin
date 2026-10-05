/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.KSerializer
import org.jetbrains.kotlin.buildtools.internal.MessageVisitor

public interface BtaSerializable {
    public fun prepareForSerialization(operationId: Int): List<MessageVisitor>
    public fun getResultSerializer(): KSerializer<out Any>
}
