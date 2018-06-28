/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.impls.DummyProfiler
import org.jetbrains.kotlin.daemon.common.impls.Profiler
import org.jetbrains.kotlin.daemon.common.impls.RemoteInputStream
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
