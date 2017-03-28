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
        val result = allocArray<CInt8Var>(resultSize)
        val bytes = allocArrayOf(data)
        EncodeBase64(bytes, data.size, result, resultSize)
        // TODO: any better way to do that without two copies?
        return result.toKString()
    }
}

public fun base64Decode(encoded: String): ByteArray {
    memScoped {
        val bufferSize: Int = 3 * encoded.length / 4
        val result = allocArray<CInt8Var>(bufferSize)
        val resultSize = allocArray<uint32_tVar>(1)
        resultSize[0].value = bufferSize
        val errorCode = DecodeBase64(encoded, encoded.length, result, resultSize)
        if (errorCode != 0) throw Error("Non-zero exit code of DecodeBase64: ${errorCode}")
        val realSize = resultSize[0].value!!
        val bytes = ByteArray(realSize)
        nativeMemUtils.getByteArray(result[0], bytes, realSize)
        return bytes
    }
}

internal class LocalHash(val value: Long) : ConstValue by Int64(value)
