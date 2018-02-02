/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.RemoteInputStream
import java.io.InputStream

class RemoteInputStreamClient(val remote: RemoteInputStream, val profiler: Profiler = DummyProfiler()): InputStream() {
    override fun read(data: ByteArray): Int = read(data, 0, data.size)

    override fun read(data: ByteArray, offset: Int, length: Int): Int =
            profiler.withMeasure(this) {
                val bytes = remote.read(length)
                assert(bytes.size <= length)
                System.arraycopy(bytes, 0, data, offset, length)
                bytes.size
            }

    override fun read(): Int =
            profiler.withMeasure(this) { remote.read() }
}
