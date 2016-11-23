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

internal fun base64Encode(data: ByteArray): String {
    memScoped {
        val resultSize = 4 * data.size / 3 + 3 + 1
        val result = allocArray<CInt8Var>(resultSize)
        val bytes = allocArrayOf(data)
        Base64Encode(bytes.ptr, data.size, result.ptr, resultSize)
        // TODO: any better way to do that without two copies?
        return CString.fromArray(result).toString()
    }
}

internal class LocalHash(val value: Long) : ConstValue by Int64(value)
