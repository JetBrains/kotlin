/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CriDeserializerRetrievalOperation

internal class CriToolchainImpl() : CriToolchain {
    override fun createCriDeserializerRetrievalOperation(): CriDeserializerRetrievalOperation {
        return CriDeserializerRetrievalOperationImpl()
    }
}
