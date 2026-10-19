/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl

// currently unused, but will be useful for actually setting up a serialization method (e.g. Protobuf) in the future
internal val btaSerializersModule: SerializersModule = SerializersModule {
    polymorphic(BtaSerializable::class) {
        subclass(JvmCompilationOperationImpl::class)
    }
    polymorphic(JvmIncrementalCompilationConfiguration::class) {
        subclass(
            JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class,
            JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer
        )
    }

}
