package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.*
import org.jetbrains.kotlin.backend.native.hash.*

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

internal class LocalHash(val value: Long) : ConstValue by Int64(value)