/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.daemon.common.impls.RemoteOutputStream
import java.io.OutputStream

class RemoteOutputStreamClient(val remote: RemoteOutputStream, val profiler: Profiler = DummyProfiler()): OutputStream() {
    override fun write(data: ByteArray) {
        profiler.withMeasure(this) { remote.write(data, 0, data.size) }
    }

    override fun write(data: ByteArray, offset: Int, length: Int) {
        profiler.withMeasure(this) { remote.write(data, offset, length) }
    }

    override fun write(byte: Int) {
        profiler.withMeasure(this) { remote.write(byte) }
    }
}
