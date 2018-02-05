/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

interface RemoteOutputStreamAsync {

    suspend fun close()

    suspend fun write(data: ByteArray, offset: Int, length: Int)

    suspend fun write(dataByte: Int)
}

interface RemoteInputStreamAsync {

    suspend fun close()

    suspend fun read(length: Int): ByteArray

    suspend fun read(): Int

}