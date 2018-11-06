/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.experimental.RemoteOutputStreamAsyncClientSide
import java.io.OutputStream

class RemoteOutputStreamClient(val remote: RemoteOutputStreamAsyncClientSide, val profiler: Profiler = DummyProfiler()) : OutputStream() {
    override fun write(data: ByteArray) = runBlocking {
        profiler.withMeasure(this) { remote.write(data, 0, data.size) }
    }

    override fun write(data: ByteArray, offset: Int, length: Int) = runBlocking {
        profiler.withMeasure(this) { remote.write(data, offset, length) }
    }

    override fun write(byte: Int) = runBlocking {
        profiler.withMeasure(this) { remote.write(byte) }
    }
}
