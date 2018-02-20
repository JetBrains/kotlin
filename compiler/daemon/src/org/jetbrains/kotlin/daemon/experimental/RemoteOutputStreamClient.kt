/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.experimental.RemoteOutputStreamAsyncClientSide
import org.jetbrains.kotlin.daemon.common.experimental.withMeasureBlocking
import java.io.OutputStream

class RemoteOutputStreamClient(val remote: RemoteOutputStreamAsyncClientSide, val profiler: Profiler = DummyProfiler()): OutputStream() {
    override fun write(data: ByteArray) {
        profiler.withMeasureBlocking(this) { remote.write(data, 0, data.size) }
    }

    override fun write(data: ByteArray, offset: Int, length: Int) {
        profiler.withMeasureBlocking(this) { remote.write(data, offset, length) }
    }

    override fun write(byte: Int) {
        profiler.withMeasureBlocking(this) { remote.write(byte) }
    }
}
