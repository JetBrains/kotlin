/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import org.jetbrains.kotlin.buildtools.api.cri.CriFileIdToPathDataDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.cri.CriLookupDataDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.cri.CriSubtypeDataDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain

internal class CriToolchainImpl() : CriToolchain {
    override fun createCriLookupDataDeserializationOperation(data: ByteArray): CriLookupDataDeserializationOperation {
        return CriLookupDataDeserializationOperationImpl(data)
    }

    override fun createCriFileIdToPathDataDeserializationOperation(data: ByteArray): CriFileIdToPathDataDeserializationOperation {
        return CriFileIdToPathDataDeserializationOperationImpl(data)
    }

    override fun createCriSubtypeDataDeserializationOperation(data: ByteArray): CriSubtypeDataDeserializationOperation {
        return CriSubtypeDataDeserializationOperationImpl(data)
    }
}
