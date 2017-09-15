/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import org.jetbrains.kotlin.backend.konan.hash.*

internal fun localHash(data: ByteArray): Long {
    memScoped {
        val res = alloc<LocalHashVar>()
        val bytes = allocArrayOf(data)
        MakeLocalHash(bytes, data.size, res.ptr)
        return res.value
    }
}

internal fun globalHash(data: ByteArray, retValPlacement: NativePlacement): GlobalHash {
    val res = retValPlacement.alloc<GlobalHash>()
    memScoped {
        val bytes = allocArrayOf(data)
        MakeGlobalHash(bytes, data.size, res.ptr)
    }
    return res
}

public fun base64Encode(data: ByteArray): String {
    memScoped {
        val resultSize = 4 * data.size / 3 + 3 + 1
        val result = allocArray<ByteVar>(resultSize)
        val bytes = allocArrayOf(data)
        EncodeBase64(bytes, data.size, result, resultSize)
        // TODO: any better way to do that without two copies?
        return result.toKString()
    }
}

public fun base64Decode(encoded: String): ByteArray {
    memScoped {
        val bufferSize: Int = 3 * encoded.length / 4
        val result = allocArray<ByteVar>(bufferSize)
        val resultSize = allocArray<uint32_tVar>(1)
        resultSize[0] = bufferSize
        val errorCode = DecodeBase64(encoded, encoded.length, result, resultSize)
        if (errorCode != 0) throw Error("Non-zero exit code of DecodeBase64: ${errorCode}")
        val realSize = resultSize[0]
        return result.readBytes(realSize)
    }
}

internal class LocalHash(val value: Long) : ConstValue by Int64(value)
