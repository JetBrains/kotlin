package org.jetbrains.kotlin.backend.konan.llvm

import kotlin_native.interop.*
import org.jetbrains.kotlin.backend.konan.hash.*

internal fun localHash(data: ByteArray): Long {
    memScoped {
        val hashBox = alloc(Int64Box)
        val bytes = allocNativeArrayOf(data)
        MakeLocalHash(bytes.ptr, data.size, hashBox)
        return hashBox.value
    }
}

internal fun globalHash(data: ByteArray, retValPlacement: Placement): GlobalHash {
    val res = retValPlacement.alloc(GlobalHash)
    memScoped {
        val bytes = allocNativeArrayOf(data)
        MakeGlobalHash(bytes.ptr, data.size, res)
    }
    return res
}

internal fun base64Encode(data: ByteArray): String {
    memScoped {
        val resultSize = 4 * data.size / 3 + 3 + 1
        val result = alloc(NativeArray of Int8Box length resultSize)
        val bytes = allocNativeArrayOf(data)
        Base64Encode(bytes.ptr, data.size, result.ptr, resultSize)
        // TODO: any better way to do that without two copies?
        return CString.fromArray(result).toString()
    }
}

internal class LocalHash(val value: Long) : ConstValue by Int64(value)
