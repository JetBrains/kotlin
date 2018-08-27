/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*
import platform.iconv.*
import platform.posix.size_tVar

fun main(args: Array<String>) {

    val sourceByteArray = "Hello!".toUtf8()

    val golden = listOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21)

    memScoped {

        val sourceLength = alloc<size_tVar>()
        val destLength = alloc<size_tVar>()

        val sourceBytes = allocArrayOf(sourceByteArray)
        val destBytes = allocArray<ByteVar>(golden.size)

        val sourcePtr = alloc<CArrayPointerVar<ByteVar>>()
        sourcePtr.value = sourceBytes

        val destPtr = alloc<CArrayPointerVar<ByteVar>>()
        destPtr.value = destBytes

        sourceLength.value = sourceByteArray.size.convert()
        destLength.value = golden.size.convert()

        val conversion = iconv_open("UTF-8", "LATIN1")

        iconv(conversion, sourcePtr.ptr, sourceLength.ptr, destPtr.ptr, destLength.ptr)

        golden.forEachIndexed { index, it ->
            println("$it ${destBytes[index]}")
            it == destBytes[index].toInt()
        }

        iconv_close(conversion)
    }
}
