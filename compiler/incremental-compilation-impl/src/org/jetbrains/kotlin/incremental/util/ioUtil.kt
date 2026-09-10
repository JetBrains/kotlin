/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.io.readBytes

/**
 * Similar to [InputStream.readBytes], however using an [expectedSize] to pre-allocate the exact
 * buffer necessary for the returned [ByteArray].
 *
 * A new buffer will be created, if the [expectedSize] turns out to be not correct (e.g. it being too small or too large).
 */
internal fun InputStream.readBytesWithExpectedSize(expectedSize: Long): ByteArray {
    // ZIP sizes can be incorrect. Bound the initial allocation and still read to EOF.
    if (expectedSize !in 0..1024 * 1024L) {
        val byteArrayOutput = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
        copyTo(byteArrayOutput)
        return byteArrayOutput.toByteArray()
    }

    val bytes = ByteArray(expectedSize.toInt())
    var offset = 0
    while (offset < bytes.size) {
        val count = read(bytes, offset, bytes.size - offset)

        /* Unhappy path: The actual size was less than the 'expectedSize'. We therefore copy the actual data */
        if (count < 0) {
            return bytes.copyOf(offset)
        }

        offset += count
    }

    /*
    Probe if the correct size was read.
    If the 'expectedSize' was actually too small, we have to create a new, larger buffer and
    copy the remainder.
     */
    val next = read()

    /* Happy path! The expectedSize was correct */
    if (next < 0) {
        return bytes
    }

    /* Unhappy path: The actual size was more than the 'expectedSize'. We therefore continue reading */
    val output = ByteArrayOutputStream(expectedSize.toInt() + DEFAULT_BUFFER_SIZE)
    output.write(bytes)
    output.write(next)
    copyTo(output)
    return output.toByteArray()
}
