/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> serializeAndRestore(value: T): T {
    val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(value) } }.toByteArray()
    return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as T
}
