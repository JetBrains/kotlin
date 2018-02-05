/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.RemoteInputStream
import org.jetbrains.kotlin.daemon.common.RemoteOutputStream

class RemoteOutputStreamAsyncWrapper(private val rmiOutput: RemoteOutputStream) : RemoteOutputStreamAsync {

    suspend override fun close() = runBlocking {
        rmiOutput.close()
    }

    suspend override fun write(data: ByteArray, offset: Int, length: Int) = runBlocking {
        rmiOutput.write(data, offset, length)
    }

    suspend override fun write(dataByte: Int) = runBlocking {
        rmiOutput.write(dataByte)
    }

}

class RemoteInputStreamAsyncWrapper(private val rmiInput: RemoteInputStream) : RemoteInputStreamAsync {

    suspend override fun close() = runBlocking {
        rmiInput.close()
    }

    suspend override fun read() = runBlocking {
        rmiInput.read()
    }

    suspend override fun read(length: Int) = runBlocking {
        rmiInput.read(length)
    }

}