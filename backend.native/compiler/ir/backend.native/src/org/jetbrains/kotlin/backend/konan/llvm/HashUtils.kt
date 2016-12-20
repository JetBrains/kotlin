package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import org.jetbrains.kotlin.backend.konan.hash.*

internal fun localHash(data: ByteArray): Long {
    memScoped {
        val res = alloc<LocalHashVar>()
        val bytes = allocArrayOf(data)
        MakeLocalHash(bytes[0].ptr, data.size, res.ptr)
        return res.value
    }
}

internal fun globalHash(data: ByteArray, retValPlacement: NativePlacement): GlobalHash {
    val res = retValPlacement.alloc<GlobalHash>()
    memScoped {
        val bytes = allocArrayOf(data)
        MakeGlobalHash(bytes[0].ptr, data.size, res.ptr)
    }
    return res
}

public fun base64Encode(data: ByteArray): String {
    memScoped {
        val resultSize = 4 * data.size / 3 + 3 + 1
        val result = allocArray<CInt8Var>(resultSize)
        val bytes = allocArrayOf(data)
        EncodeBase64(bytes.ptr, data.size, result.ptr, resultSize)
        // TODO: any better way to do that without two copies?
        return CString.fromArray(result).toString()
    }
}

public fun base64Decode(encoded: String): ByteArray {
    memScoped {
        val bufferSize: Int = 3 * encoded.length / 4
        val result = allocArray<CInt8Var>(bufferSize)
        val resultSize = allocArray<uint32_tVar>(1)
        resultSize[0].value = bufferSize
        val errorCode = DecodeBase64(encoded, encoded.length, result[0].ptr, resultSize[0].ptr)
        if (errorCode != 0) throw Error("Non-zero exit code of DecodeBase64: ${errorCode}")
        val realSize = resultSize[0].value!!
        val bytes = ByteArray(realSize)
        nativeMemUtils.getByteArray(result[0], bytes, realSize)
        return bytes
    }
}

internal class LocalHash(val value: Long) : ConstValue by Int64(value)
