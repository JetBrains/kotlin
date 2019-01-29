/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

class SerializedIr (
    val module: ByteArray,
    val declarations: Map<UniqId, ByteArray>,
    val debugIndex: Map<UniqId, String>
)