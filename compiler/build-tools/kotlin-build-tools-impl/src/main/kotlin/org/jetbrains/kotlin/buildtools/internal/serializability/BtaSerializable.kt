/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.internal.MessageVisitor
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase

public interface BtaSerializable {
    public fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> {
        return emptyList()
    }

    public fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase? = null) {}
}
