/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

interface RemoteOutputStreamAsync {

    /** closeStream() name is chosen since Clients are AutoClosable now
     * and Client-implementations of RemoteOutputStreamAsync have conflict of 'close' name **/
    suspend fun closeStream()

    suspend fun write(data: ByteArray, offset: Int, length: Int)

    suspend fun write(dataByte: Int)
}

interface RemoteInputStreamAsync {

    /** closeStream() name is chosen since Clients are AutoClosable now
     * and Client-implementations of RemoteInputStreamAsync have conflict of 'close' name **/
    suspend fun closeStream()

    suspend fun read(length: Int): ByteArray

    suspend fun read(): Int

}